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
    fun parse(format: String, text: String, ctx: SourceContext): List<Indicator> = when (format) {
        "stix2" -> stix2(text, ctx)
        "misp-csv" -> mispCsv(text, ctx)
        "domains-txt" -> domainsTxt(text, ctx)
        "csv-domain-ip" -> csvDomainIp(text, ctx)
        "curated-tsv" -> curatedTsv(text, ctx)
        else -> throw IocFormatException("unknown indicator format: $format")
    }

    // One comparison inside a STIX pattern: [object:path = 'value'].
    private val COMPARISON = Regex("""([a-z0-9-]+):([A-Za-z0-9_.'-]+)\s*=\s*'((?:\\.|[^'\\])*)'""")

    fun stix2(text: String, ctx: SourceContext): List<Indicator> {
        val root = runCatching { Json.parseToJsonElement(text).jsonObject }
            .getOrElse { throw IocFormatException("not a STIX 2 bundle: ${it.message}") }
        val objects = (root["objects"] as? JsonArray) ?: throw IocFormatException("STIX bundle has no objects")
        val malwareNames = HashMap<String, String>()
        val familyOf = HashMap<String, String>()
        for (o in objects) {
            val obj = o as? JsonObject ?: continue
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
            if (obj.str("type") != "indicator") continue
            val pattern = obj.str("pattern")
            val family = malwareNames[familyOf[obj.str("id")]].orEmpty().ifEmpty { ctx.family }
            for (m in COMPARISON.findAll(pattern)) {
                val (objType, path, rawValue) = m.destructured
                val type = stixType(objType, path.replace("'", "").lowercase()) ?: continue
                val value = rawValue.replace("\\'", "'").replace("\\\\", "\\")
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
        val rows = csvRows(text)
        val header = rows.firstOrNull()?.map { it.lowercase() } ?: return emptyList()
        val typeCol = header.indexOf("type")
        val valueCol = header.indexOf("value")
        if (typeCol < 0 || valueCol < 0) throw IocFormatException("MISP CSV lacks type/value columns")
        return rows.drop(1).mapNotNull { row ->
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
        }
    }

    fun domainsTxt(text: String, ctx: SourceContext): List<Indicator> =
        text.lineSequence().map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }
            .map { Indicator(it, inferType(it), ctx.category, ctx.family, ctx.sourceId) }.toList()

    /** Amnesty 2018: "domain_name","ip_address" */
    fun csvDomainIp(text: String, ctx: SourceContext): List<Indicator> =
        csvRows(text).drop(1).flatMap { row ->
            buildList {
                row.getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    add(Indicator(it, IocType.DOMAIN, ctx.category, ctx.family, ctx.sourceId))
                }
                row.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    add(Indicator(it, IocType.IP, ctx.category, ctx.family, ctx.sourceId))
                }
            }
        }

    /** The Zero Breach Toolkit's curated files: indicator TAB tier TAB confidence TAB source [TAB family]. */
    fun curatedTsv(text: String, ctx: SourceContext): List<Indicator> =
        text.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }.mapNotNull { line ->
            val cols = line.split('\t')
            val value = cols[0].trim()
            if (value.isEmpty() || cols.size < 3) return@mapNotNull null
            val family = cols.getOrNull(4)?.trim().orEmpty().ifEmpty { ctx.family }
            Indicator(value, inferType(value), ctx.category, family, cols.getOrNull(3)?.trim().orEmpty().ifEmpty { ctx.sourceId })
        }.toList()

    private val APP_ID_RE = Regex("""^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]+)+$""")

    /** Debug-only test list: package names become APP_ID; SHA-256 stays SHA256 (Toolkit TSV columns). */
    fun debugTestTsv(text: String, ctx: SourceContext): List<Indicator> =
        text.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }.mapNotNull { line ->
            val cols = line.split('\t')
            val value = cols[0].trim()
            if (value.isEmpty() || cols.size < 3) return@mapNotNull null
            val family = cols.getOrNull(4)?.trim().orEmpty().ifEmpty { ctx.family }
            val type = when {
                SHA256_RE.matches(value) -> IocType.SHA256
                APP_ID_RE.matches(value) -> IocType.APP_ID
                else -> inferType(value)
            }
            Indicator(value, type, ctx.category, family, cols.getOrNull(3)?.trim().orEmpty().ifEmpty { ctx.sourceId })
        }.toList()

    private val SHA256_RE = Regex("(?i)[0-9a-f]{64}")
    private val IPV4_RE = Regex("""\d{1,3}(\.\d{1,3}){3}""")

    fun inferType(v: String): IocType = when {
        SHA256_RE.matches(v.trim()) -> IocType.SHA256
        IPV4_RE.matches(v.trim()) -> IocType.IP
        ':' in v && v.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' || it == ':' } -> IocType.IP
        else -> IocType.DOMAIN
    }

    /** Minimal RFC 4180 reader: quoted fields, doubled quotes, commas inside quotes. */
    fun csvRows(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        for (line in text.lineSequence()) {
            if (line.isBlank()) continue
            val fields = ArrayList<String>()
            val cur = StringBuilder()
            var quoted = false
            var i = 0
            while (i < line.length) {
                val c = line[i]
                when {
                    quoted && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { cur.append('"'); i++ }
                    c == '"' -> quoted = !quoted
                    c == ',' && !quoted -> { fields += cur.toString(); cur.clear() }
                    else -> cur.append(c)
                }
                i++
            }
            // A quote that never closes means the row is malformed; drop it rather than guess a value.
            if (quoted) continue
            fields += cur.toString()
            rows += fields
        }
        return rows
    }

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
    )

    // Redistribution: MVT (MIT) and ECHAP (CC BY 4.0) ship inside the app; the others carry no
    // licence and are fetched on the device from the pinned commit instead.
    private val BUNDLED_REPOS = setOf("mvt-project/mvt-indicators", "AssoEchap/stalkerware-indicators")

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
        FeedSource(
            id = s("id"), name = s("name"), repo = repo, commit = s("commit"), path = s("path"),
            format = s("format"), cite = s("cite"),
            category = if (s("category") == "stalkerware") Category.STALKERWARE else Category.MERCENARY,
            family = s("family"), sha256 = s("sha256").lowercase(),
            bundled = repo in BUNDLED_REPOS, androidArtifacts = s("id") !in IOS_ONLY,
        ).also { require(SAFE_FILE_NAME.matches(it.fileName)) { "unsafe file name: ${it.fileName}" } }
    }
}
