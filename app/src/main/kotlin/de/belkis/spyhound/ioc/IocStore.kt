package de.belkis.spyhound.ioc

import android.content.Context
import de.belkis.spyhound.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** State of the local indicator collection, shown on the start screen. */
data class IocStatus(
    val sources: Int,
    val available: Int,
    val missing: List<FeedSource>,
    val corrupt: List<FeedSource>,
    /** The catalog feeds that were read and verified, so the start screen only names sources that are really in use. */
    val active: List<FeedSource> = emptyList(),
)

class LoadedIocs(val set: IocSet, val status: IocStatus)

/** A tracker or phishing domain list hit: [domain] is the listed entry, [family] the label of its source. */
data class DomainMatch(val category: Category, val family: String, val domain: String)

/** The domain lists the DNS watcher uses beside the spyware indicators. They are never part of an [IocSet]. */
class DomainLists(
    private val lists: Map<Category, List<Pair<String, DomainList>>>,
    val missing: List<FeedSource> = emptyList(),
) {
    fun lookup(category: Category, host: String): DomainMatch? {
        for ((family, list) in lists[category].orEmpty()) {
            list.lookup(host)?.let { return DomainMatch(category, family, it) }
        }
        return null
    }

    fun size(category: Category): Int = lists[category].orEmpty().sumOf { it.second.size }

    companion object {
        val EMPTY = DomainLists(emptyMap())
    }
}

/**
 * Bundled indicators (MVT, ECHAP, Zero Breach Toolkit curated lists) plus the sources
 * that may only be downloaded on the device. Downloads come from the commit this
 * release was reviewed against and must match its SHA-256 byte for byte.
 */
class IocStore(private val context: Context) {
    private val dir = File(context.filesDir, "iocs").apply { mkdirs() }

    fun catalog(): List<FeedSource> =
        FeedCatalog.parse(context.assets.open("iocs/sources.json").bufferedReader().use { it.readText() })

    suspend fun load(): LoadedIocs = withContext(Dispatchers.IO) {
        val sources = catalog()
        KnownGood.useSystemList(context.assets.open("iocs/$KNOWN_GOOD").bufferedReader().use { it.readText() })
        val all = ArrayList<Indicator>()
        val missing = ArrayList<FeedSource>()
        val corrupt = ArrayList<FeedSource>()
        // Curated files first, so their more precise attribution wins on duplicates.
        appendCuratedIndicators(all, corrupt) { name ->
            context.assets.open("iocs/$name").use { it.readBytes() }
        }
        for (s in sources) {
            val bytes = readSource(s)
            if (bytes == null) { missing += s; continue }
            if (sha256(bytes) != s.sha256) { corrupt += s; continue }
            runCatching {
                Parsers.parse(s.format, bytes.toString(Charsets.UTF_8), SourceContext(s.id, s.category, s.family, s.androidArtifacts))
            }.onSuccess {
                // A source that lists a known-harmless fingerprint is treated as unreliable and left out entirely.
                if (KnownGood.conflicts(it).isEmpty()) all += it else corrupt += s
            }.onFailure { corrupt += s }
        }
        IocStore.appendDebugTestIndicators(all, dir, BuildConfig.DEBUG)
        // The curated lists belong to the toolkit, not to the catalog count shown as "45/45"; a broken one is still
        // listed in [corrupt] so the start screen reports it.
        val catalogBroken = corrupt.count { it in sources }
        LoadedIocs(IocSet(all), IocStatus(sources.size, sources.size - missing.size - catalogBroken, missing, corrupt, sources.filter { it !in missing && it !in corrupt }))
    }

    fun domainCatalog(): List<FeedSource> =
        FeedCatalog.parse(context.assets.open("iocs/domainlists.json").bufferedReader().use { it.readText() })

    /** Loads the tracker and phishing lists; a missing or altered file is skipped and reported, never half-used. */
    suspend fun loadDomainLists(): DomainLists = withContext(Dispatchers.IO) {
        val byCategory = HashMap<Category, MutableList<Pair<String, DomainList>>>()
        val missing = ArrayList<FeedSource>()
        for (s in domainCatalog()) {
            val bytes = readSource(s)
            if (bytes == null || sha256(bytes) != s.sha256) { missing += s; continue }
            byCategory.getOrPut(s.category) { ArrayList() } += s.family to DomainList.parse(bytes)
        }
        DomainLists(byCategory, missing)
    }

    private fun readSource(s: FeedSource): ByteArray? = selectSource(
        s.sha256,
        local = File(dir, s.fileName).takeIf { it.isFile }?.readBytes(),
        bundled = if (s.bundled) { { runCatching { context.assets.open("iocs/${s.fileName}").use { it.readBytes() } }.getOrNull() } } else null,
    )

    /** Downloads every source not shipped in the app. Returns (ok, failed) counts. Network only here. */
    suspend fun update(onProgress: (String) -> Unit = {}): Pair<Int, List<String>> = withContext(Dispatchers.IO) {
        var ok = 0
        val failed = ArrayList<String>()
        for (s in (catalog() + domainCatalog()).filter { !it.bundled }) {
            onProgress(s.name)
            val target = File(dir, s.fileName)
            if (target.isFile && sha256(target.readBytes()) == s.sha256) { ok++; continue }
            runCatching {
                val bytes = download(s)
                val got = sha256(bytes)
                if (got != s.sha256) throw IOException("Prüfsumme stimmt nicht (${got.take(12)} statt ${s.sha256.take(12)})")
                val tmp = File(dir, s.fileName + ".part")
                replaceAtomically(tmp, target, bytes)
            }.onSuccess { ok++ }.onFailure { failed += "${s.name}: ${it.message}" }
        }
        ok to failed
    }

    private fun download(s: FeedSource): ByteArray {
        require(FeedCatalog.isAllowedPath(s.repo, s.path) && Regex("[0-9a-f]{40}").matches(s.commit)) { "nicht erlaubte Quelle" }
        return withMirror(s.pinnedUrl, s.mirrorUrl, ::fetch)
    }

    private fun fetch(address: String): ByteArray {
        val url = URL(address)
        require(url.protocol == "https" && url.host in DOWNLOAD_HOSTS) { "nicht erlaubte Quelle" }
        val conn = url.openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = false // a redirect could leave the allow-list
        if (url.host == "api.github.com") conn.setRequestProperty("Accept", "application/vnd.github.raw")
        conn.connectTimeout = 20_000
        conn.readTimeout = 60_000
        try {
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(64 shl 10)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    if (out.size() > MAX_DOWNLOAD) throw IOException("Datei zu groß")
                }
                out.toByteArray()
            }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        /**
         * Prefers a copy that matches the pin: a damaged local file must not shadow a good bundled one.
         * If nothing matches, the first available copy is returned so the caller reports it as altered, not missing.
         */
        internal fun selectSource(pin: String, local: ByteArray?, bundled: (() -> ByteArray?)?): ByteArray? {
            if (local != null && sha256(local) == pin) return local
            val asset = bundled?.invoke()
            if (asset != null && sha256(asset) == pin) return asset
            return local ?: asset
        }

        val CURATED = listOf("adfection_iocs.txt", "curated_mercenary.txt")
        private val CURATED_SHA256 = mapOf(
            "adfection_iocs.txt" to "42bff7164687f1faf6ab5691738e156f72684cadbf63e403150e02acc65ad518",
            "curated_mercenary.txt" to "2508f48565144b9b63d47f916379f7c829a5541cdd63cd42d8bd225b4cc29e21",
        )
        const val KNOWN_GOOD = "knowngood-system.sha256"
        private const val MAX_DOWNLOAD = 64 shl 20
        private val DOWNLOAD_HOSTS = setOf("raw.githubusercontent.com", "api.github.com")

        /**
         * Tries the raw host first and the GitHub API second (raw.githubusercontent.com is blocked in some countries).
         * Both serve the file at the pinned commit; the caller checks the SHA-256 either way, so the host does not matter.
         */
        internal fun withMirror(primary: String, mirror: String, fetch: (String) -> ByteArray): ByteArray =
            try {
                fetch(primary)
            } catch (first: IOException) {
                try {
                    fetch(mirror)
                } catch (second: IOException) {
                    second.addSuppressed(first)
                    throw IOException("${first.message}; Ersatzweg: ${second.message}", second)
                }
            }
        internal const val DEBUG_TEST_FILE = "debug-test.tsv"

        internal fun appendCuratedIndicators(
            all: MutableList<Indicator>,
            corrupt: MutableList<FeedSource>,
            readAsset: (String) -> ByteArray,
        ) {
            for (name in CURATED) {
                // Local-only descriptors let corrupt assets use the same reporting path as catalog sources.
                val source = FeedSource(
                    id = name.substringBefore('.'), name = name, repo = "", commit = "", path = name,
                    format = "curated-tsv", cite = "", category = Category.MERCENARY, family = "",
                    sha256 = CURATED_SHA256.getValue(name), bundled = true, androidArtifacts = true,
                )
                runCatching {
                    val bytes = readAsset(name)
                    if (sha256(bytes) != source.sha256) throw IocFormatException("curated source hash mismatch")
                    val indicators = Parsers.curatedTsv(bytes.toString(Charsets.UTF_8), SourceContext(source.id, source.category))
                    if (KnownGood.conflicts(indicators).isNotEmpty()) throw IocFormatException("curated source conflicts with known-good list")
                    indicators
                }.onSuccess { all += it }.onFailure { corrupt += source }
            }
        }

        internal fun appendDebugTestIndicators(all: MutableList<Indicator>, iocDir: File, debugBuild: Boolean) {
            if (!debugBuild) return
            val file = File(iocDir, DEBUG_TEST_FILE)
            if (!file.isFile) return
            val ctx = SourceContext("debug-test", Category.MERCENARY, "Testspy")
            runCatching {
                all += Parsers.debugTestTsv(file.readText(), ctx)
            }
        }

        /** Writes [bytes] next to [target] and moves them over it in one step; a failure leaves the old file intact. */
        internal fun replaceAtomically(tmp: File, target: File, bytes: ByteArray) {
            try {
                tmp.writeBytes(bytes)
                try {
                    Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            } catch (e: IOException) {
                tmp.delete()
                throw IOException("konnte nicht gespeichert werden", e)
            }
        }

        fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
