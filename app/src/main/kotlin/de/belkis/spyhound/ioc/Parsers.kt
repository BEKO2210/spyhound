package de.belkis.spyhound.ioc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class IocFormatException(message: String) : Exception(message)

/** Per-file context every parser stamps onto its indicators. */
data class SourceContext(
    val sourceId: String,
    val category: Category,
    val family: String = "",
    val androidArtifacts: Boolean = true,
)

object Parsers {
    private const val MAX_FIELDS = 64
    private const val MAX_FIELD_LENGTH = 8 * 1024
    private const val MAX_INDICATORS = 200_000

    private fun <T> Sequence<T>.boundedList(): List<T> {
        val out = ArrayList<T>()
        for (item in this) {
            if (out.size == MAX_INDICATORS) throw IocFormatException("too many indicators")
            out += item
        }
        return out
    }

    private fun boundedLines(text: String): Sequence<String> =
        text.lineSequence().filter { it.length <= MAX_FIELD_LENGTH }

    private fun tsvRows(text: String): Sequence<List<String>> =
        text.lineSequence().filter { line ->
            // Check limits before split allocates fields.
            var fields = 1
            var length = 0
            line.all { c ->
                if (c == '\t') { length = 0; ++fields <= MAX_FIELDS }
                else ++length <= MAX_FIELD_LENGTH
            }
        }.filter { it.isNotBlank() && !it.startsWith("#") }.map { it.split('\t') }

    fun parse(format: String, text: String, ctx: SourceContext): List<Indicator> = when (format) {
        "stix2" -> stix2(text, ctx)
        "misp-csv" -> mispCsv(text, ctx)
        "domains-txt" -> domainsTxt(text, ctx)
        "csv-domain-ip" -> csvDomainIp(text, ctx)
        "curated-tsv" -> curatedTsv(text, ctx)
        "hashlist" -> hashlist(text, ctx)
        "typed-csv-android-sha256" -> typedCsvAndroidSha256(text, ctx)
        else -> throw IocFormatException("unknown indicator format: $format")
    }

    // One comparison inside a STIX pattern: [object:path = 'value'].
    private val COMPARISON = Regex("""([a-z0-9-]+):([A-Za-z0-9_.'-]+)\s*=\s*'((?:\\.|[^'\\])*+)'""")

    private val SIMPLE_PATTERN = COMPARISON.pattern.let { comparison ->
        val observation = """\[\s*$comparison(?:\s+OR\s+$comparison)*\s*]"""
        Regex("""$observation(?:\s+OR\s+$observation)*""")
    }

    fun stix2(text: String, ctx: SourceContext): List<Indicator> {
        val root = runCatching { Json.parseToJsonElement(text).jsonObject }
            .getOrElse { throw IocFormatException("not a STIX 2 bundle: ${it.message}") }
        val objects = (root["objects"] as? JsonArray) ?: throw IocFormatException("STIX bundle has no objects")
        val malwareNames = HashMap<String, String>()
        val familyOf = HashMap<String, String>()
        for (o in objects) {
            val obj = o as? JsonObject ?: continue
            if (obj.size > MAX_FIELDS || obj.values.any {
                it is kotlinx.serialization.json.JsonPrimitive && it.content.length > MAX_FIELD_LENGTH
            }) continue
            when (obj.str("type")) {
                "malware" -> malwareNames[obj.str("id")] = obj.str("name")
                "relationship" -> if (obj.str("relationship_type") == "indicates") {
                    familyOf[obj.str("source_ref")] = obj.str("target_ref")
                }
            }
        }
        val out = ArrayList<Indicator>()
        for (o in objects) {
            val obj = o as? JsonObject ?: continue
            if (obj.size > MAX_FIELDS || obj.values.any {
                it is kotlinx.serialization.json.JsonPrimitive && it.content.length > MAX_FIELD_LENGTH
            }) continue
            if (obj.str("type") != "indicator" || obj.str("revoked") == "true") continue
            val pattern = obj.str("pattern")
            // Validate the entire grammar before emitting any part of a condition.
            if (!SIMPLE_PATTERN.matches(pattern.trim())) continue
            val family = malwareNames[familyOf[obj.str("id")]].orEmpty().ifEmpty { ctx.family }
            for (m in COMPARISON.findAll(pattern)) {
                val (objType, path, rawValue) = m.destructured
                val type = stixType(objType, path.replace("'", "").lowercase()) ?: continue
                val value = rawValue.replace("\\'", "'").replace("\\\\", "\\")
                if (out.size == MAX_INDICATORS) throw IocFormatException("too many indicators")
                out += Indicator(value, type, ctx.category, family, ctx.sourceId, ctx.androidArtifacts)
            }
        }
        return out
    }

    private fun stixType(obj: String, path: String): IocType? = when (obj) {
        "domain-name" -> IocType.DOMAIN
        "ipv4-addr", "ipv6-addr" -> IocType.IP
        "url" -> IocType.URL
        "email-addr" -> IocType.EMAIL
        "app" -> when (path) {
            "id" -> IocType.APP_ID
            "cert.sha1", "cert.sha256" -> IocType.APP_CERT
            else -> null
        }
        "process" -> if (path == "name") IocType.PROCESS else null
        "android-property" -> if (path == "name") IocType.ANDROID_PROP else null
        "file" -> when (path) {
            "hashes.sha256", "hashes.sha-256" -> IocType.SHA256
            "path" -> IocType.FILE_PATH
            "name" -> IocType.FILE_NAME
            else -> null
        }
        else -> null // configuration profiles and other iPhone-only objects
    }

    /** Citizen Lab's MISP export: uuid,event_id,category,type,value,comment,to_ids,date */
    fun mispCsv(text: String, ctx: SourceContext): List<Indicator> {
        val rows = csvRecords(text).iterator()
        val header = if (rows.hasNext()) rows.next().map { it.lowercase() } else return emptyList()
        val typeCol = header.indexOf("type")
        val valueCol = header.indexOf("value")
        if (typeCol < 0 || valueCol < 0) throw IocFormatException("MISP CSV lacks type/value columns")
        return rows.asSequence().mapNotNull { row ->
            val value = row.getOrNull(valueCol)?.trim().orEmpty()
            val type = when (row.getOrNull(typeCol)?.trim()?.lowercase()) {
                "domain", "hostname" -> IocType.DOMAIN
                "ip-dst", "ip-src" -> IocType.IP
                "url" -> IocType.URL
                "sha256" -> IocType.SHA256
                "email-src", "email-dst" -> IocType.EMAIL
                else -> null
            } ?: return@mapNotNull null
            if (value.isEmpty()) null else Indicator(value, type, ctx.category, ctx.family, ctx.sourceId)
        }.boundedList()
    }

    fun domainsTxt(text: String, ctx: SourceContext): List<Indicator> =
        boundedLines(text).map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }
            .map { Indicator(it, inferType(it), ctx.category, ctx.family, ctx.sourceId) }.boundedList()

    /** Amnesty 2018: "domain_name","ip_address" */
    fun csvDomainIp(text: String, ctx: SourceContext): List<Indicator> =
        csvRecords(text).drop(1).flatMap { row ->
            buildList {
                row.getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    add(Indicator(it, IocType.DOMAIN, ctx.category, ctx.family, ctx.sourceId))
                }
                row.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    add(Indicator(it, IocType.IP, ctx.category, ctx.family, ctx.sourceId))
                }
            }
        }.boundedList()

    /** The Zero Breach Toolkit's curated files: indicator TAB tier TAB confidence TAB source [TAB family]. */
    fun curatedTsv(text: String, ctx: SourceContext): List<Indicator> =
        tsvRows(text).mapNotNull { cols ->
            val value = cols[0].trim()
            if (value.isEmpty() || cols.size < 3) return@mapNotNull null
            val family = cols.getOrNull(4)?.trim().orEmpty().ifEmpty { ctx.family }
            Indicator(value, inferType(value), ctx.category, family, cols.getOrNull(3)?.trim().orEmpty().ifEmpty { ctx.sourceId })
        }.boundedList()

    /** One SHA-256 per line, `#` comments allowed. Lines that are not exactly 64 hex digits are dropped. */
    fun hashlist(text: String, ctx: SourceContext): List<Indicator> =
        boundedLines(text).map { it.substringBefore('#').trim().lowercase() }
            .filter { SHA256_LOWER_RE.matches(it) }
            .map { Indicator(it, IocType.SHA256, ctx.category, ctx.family, ctx.sourceId) }.boundedList()

    /**
     * Meta threat-research CSV: `indicator_type,indicator_value,comment,ds`. Only `sha256` rows whose comment says
     * Android count; the Windows hash in the same file and all other rows are dropped.
     */
    fun typedCsvAndroidSha256(text: String, ctx: SourceContext): List<Indicator> =
        csvRecords(text).mapNotNull { cols ->
            if (cols.size < 3 || cols[0].trim() != "sha256") return@mapNotNull null
            val hash = cols[1].trim().lowercase()
            if (!SHA256_LOWER_RE.matches(hash) || !cols[2].contains("android", ignoreCase = true)) return@mapNotNull null
            Indicator(hash, IocType.SHA256, ctx.category, ctx.family, ctx.sourceId)
        }.boundedList()

    private val SHA256_LOWER_RE = Regex("[0-9a-f]{64}")
    private val APP_ID_RE = Regex("""^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]+)+$""")

    /** Debug-only test list: package names become APP_ID; SHA-256 stays SHA256 (Toolkit TSV columns). */
    fun debugTestTsv(text: String, ctx: SourceContext): List<Indicator> =
        tsvRows(text).mapNotNull { cols ->
            val value = cols[0].trim()
            if (value.isEmpty() || cols.size < 3) return@mapNotNull null
            val family = cols.getOrNull(4)?.trim().orEmpty().ifEmpty { ctx.family }
            val type = when {
                SHA256_RE.matches(value) -> IocType.SHA256
                APP_ID_RE.matches(value) -> IocType.APP_ID
                else -> inferType(value)
            }
            Indicator(value, type, ctx.category, family, cols.getOrNull(3)?.trim().orEmpty().ifEmpty { ctx.sourceId })
        }.boundedList()

    private val SHA256_RE = Regex("(?i)[0-9a-f]{64}")
    private val IPV4_RE = Regex("""\d{1,3}(\.\d{1,3}){3}""")

    fun inferType(v: String): IocType = when {
        SHA256_RE.matches(v.trim()) -> IocType.SHA256
        IPV4_RE.matches(v.trim()) -> IocType.IP
        ':' in v && v.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' || it == ':' } -> IocType.IP
        else -> IocType.DOMAIN
    }

    /** RFC 4180 records; retain quote state even after a record exceeds its limits. */
    private fun csvRecords(text: String): Sequence<List<String>> = sequence {
        val fields = ArrayList<String>()
        val cur = StringBuilder()
        var quoted = false
        var closed = false
        var invalid = false
        var started = false
        var fieldCount = 1
        fun append(c: Char) {
            if (cur.length == MAX_FIELD_LENGTH) invalid = true
            if (!invalid) cur.append(c)
        }
        fun field() {
            if (!invalid) fields += cur.toString()
            cur.clear()
            closed = false
        }
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted -> when {
                    c != '"' -> append(c)
                    i + 1 < text.length && text[i + 1] == '"' -> { append('"'); i++ }
                    else -> { quoted = false; closed = true }
                }
                c == '\r' || c == '\n' -> {
                    field()
                    if (started && !invalid) yield(fields.toList())
                    fields.clear()
                    invalid = false
                    started = false
                    fieldCount = 1
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                }
                c == ',' -> {
                    field()
                    if (++fieldCount > MAX_FIELDS) invalid = true
                    started = true
                }
                c == '"' -> {
                    if (cur.isNotEmpty() || closed) invalid = true
                    quoted = true
                    started = true
                }
                else -> {
                    if (closed) invalid = true
                    append(c)
                    started = true
                }
            }
            i++
        }
        if (started && !quoted && !invalid) {
            field()
            yield(fields.toList())
        }
    }

    fun csvRows(text: String): List<List<String>> = csvRecords(text).boundedList()

    private fun JsonObject.str(key: String): String =
        (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
}

/** The published indicator sources, with the commit and file SHA-256 this release was checked against. */
data class FeedSource(
    val id: String,
    val name: String,
    val repo: String,
    val commit: String,
    val path: String,
    val format: String,
    val cite: String,
    val category: Category,
    val family: String,
    val sha256: String,
    val bundled: Boolean,
    val androidArtifacts: Boolean,
) {
    val pinnedUrl: String get() = "https://raw.githubusercontent.com/$repo/$commit/$path"
    val fileName: String get() = id + "." + path.substringAfterLast('.', "dat")
}

/** A source file name is a plain name inside the indicator folder, never a path. */
private val SAFE_FILE_NAME = Regex("[A-Za-z0-9_-]+\\.[A-Za-z0-9]+")

object FeedCatalog {
    /** Only these repositories are ever downloaded from, as in the desktop toolkit. */
    val ALLOWED_REPOS = setOf(
        "mvt-project/mvt-indicators", "AmnestyTech/investigations",
        "AssoEchap/stalkerware-indicators", "citizenlab/malware-indicators",
        "eset/malware-ioc", "meta/threat-research",
        // Plain domain lists for the DNS watcher (tracker, phishing); see assets/iocs/domainlists.json.
        "Perflyst/PiHoleBlocklist", "StevenBlack/hosts", "Phishing-Database/Phishing.Database",
    )

    // Redistribution: MVT (MIT), ECHAP (CC BY 4.0), ESET (BSD-2-Clause), Meta (MIT) and the two small MIT tracker lists ship inside
    // the app; the others carry no licence (or are too large and too short-lived, as the phishing list) and are fetched
    // on the device from the pinned commit instead.
    private val BUNDLED_REPOS = setOf(
        "mvt-project/mvt-indicators", "AssoEchap/stalkerware-indicators", "eset/malware-ioc",
        "meta/threat-research", "Perflyst/PiHoleBlocklist", "StevenBlack/hosts",
    )

    // Sources that document iPhone spyware only: their domains still count everywhere, their
    // app ids, process names and paths do not describe anything on Android.
    private val IOS_ONLY = setOf(
        "mvt-operation-triangulation", "mvt-quadream-kingspawn", "amnesty-pegasus-2021",
        "mvt-darksword", "mvt-coruna",
    )

    fun parse(json: String): List<FeedSource> = Json.parseToJsonElement(json).jsonArray.map { e ->
        val o = e.jsonObject
        fun s(k: String) = o[k]?.jsonPrimitive?.content.orEmpty()
        val repo = s("repo")
        require(repo in ALLOWED_REPOS) { "source outside the allow-list: $repo" }
        require(Regex("[0-9a-f]{40}").matches(s("commit"))) { "source commit must be 40 lowercase hex characters" }
        FeedSource(
            id = s("id"), name = s("name"), repo = repo, commit = s("commit"), path = s("path"),
            format = s("format"), cite = s("cite"),
            category = when (s("category")) {
                "stalkerware" -> Category.STALKERWARE
                "malware" -> Category.MALWARE
                "tracker" -> Category.TRACKER
                "phishing" -> Category.PHISHING
                else -> Category.MERCENARY
            },
            family = s("family"), sha256 = s("sha256").lowercase(),
            bundled = repo in BUNDLED_REPOS, androidArtifacts = s("id") !in IOS_ONLY,
        ).also { require(SAFE_FILE_NAME.matches(it.fileName)) { "unsafe file name: ${it.fileName}" } }
    }
}
