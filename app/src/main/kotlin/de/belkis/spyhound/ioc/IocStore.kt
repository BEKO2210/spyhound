package de.belkis.spyhound.ioc

import android.content.Context
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
)

class LoadedIocs(val set: IocSet, val status: IocStatus)

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
        val all = ArrayList<Indicator>()
        // Curated files first, so their more precise attribution wins on duplicates.
        for (name in CURATED) {
            val text = context.assets.open("iocs/$name").bufferedReader().use { it.readText() }
            all += Parsers.curatedTsv(text, SourceContext(name.substringBefore('.'), Category.MERCENARY))
        }
        val missing = ArrayList<FeedSource>()
        val corrupt = ArrayList<FeedSource>()
        for (s in sources) {
            val bytes = readSource(s)
            if (bytes == null) { missing += s; continue }
            if (sha256(bytes) != s.sha256) { corrupt += s; continue }
            runCatching {
                Parsers.parse(s.format, bytes.toString(Charsets.UTF_8), SourceContext(s.id, s.category, s.family, s.androidArtifacts))
            }.onSuccess { all += it }.onFailure { corrupt += s }
        }
        LoadedIocs(IocSet(all), IocStatus(sources.size, sources.size - missing.size - corrupt.size, missing, corrupt))
    }

    private fun readSource(s: FeedSource): ByteArray? {
        val local = File(dir, s.fileName)
        if (local.isFile) return local.readBytes()
        if (!s.bundled) return null
        return runCatching { context.assets.open("iocs/${s.fileName}").use { it.readBytes() } }.getOrNull()
    }

    /** Downloads every source not shipped in the app. Returns (ok, failed) counts. Network only here. */
    suspend fun update(onProgress: (String) -> Unit = {}): Pair<Int, List<String>> = withContext(Dispatchers.IO) {
        var ok = 0
        val failed = ArrayList<String>()
        for (s in catalog().filter { !it.bundled }) {
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
        val url = URL(s.pinnedUrl)
        require(url.protocol == "https" && url.host == "raw.githubusercontent.com" &&
            FeedCatalog.ALLOWED_REPOS.any { url.path.startsWith("/$it/") } && ".." !in url.path) { "nicht erlaubte Quelle" }
        val conn = url.openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = false // a redirect could leave the allow-list
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
        val CURATED = listOf("adfection_iocs.txt", "curated_mercenary.txt")
        private const val MAX_DOWNLOAD = 64 shl 20

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
