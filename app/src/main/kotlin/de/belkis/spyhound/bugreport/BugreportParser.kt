package de.belkis.spyhound.bugreport

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

/**
 * Parsed Android bugreport.zip (dumpstate). Missing sections stay empty so
 * the analyzer can record them as gaps instead of pretending they were checked.
 */
data class Bugreport(
    val psText: String = "",
    val packages: List<String> = emptyList(),
    val dumpsys: Map<String, String> = emptyMap(),
    val logcat: String = "",
    val props: Map<String, String> = emptyMap(),
    val createdAt: LocalDateTime? = null,
    val device: String = "",
    val fsPaths: List<String> = emptyList(),
    val boardText: String = "",
    val truncated: Boolean = false,
    val warnings: List<String> = emptyList(),
)

/**
 * Reads a bugreport-*.zip with [java.util.zip]. Never writes entries to disk
 * (so zip-slip paths are ignored). Caps each kept section at 32 MiB and the
 * whole uncompressed stream plus the entry count, so zip bombs stop here.
 */
object BugreportParser {
    const val MAX_ENTRIES = 8_192
    const val MAX_TOTAL_BYTES = 512L * 1024 * 1024
    const val MAX_SECTION_BYTES = 32 * 1024 * 1024
    // Three section caps (32 MiB each) fit in 96 MiB; bounds heap on reports with many log/dumpsys blocks.
    const val MAX_RETAINED_TEXT_BYTES = 96 * 1024 * 1024
    internal const val MAX_LINE_BYTES = 1 * 1024 * 1024

    private val PROP = Regex("""^\[([^]]+)]:\s*\[(.*)]$""")
    private val DUMPSTATE_DATE = Regex("""== dumpstate:\s+(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})""")
    private val FINGERPRINT = Regex("""Build fingerprint:\s+'([^']+)'""")
    private val BUILD_LINE = Regex("""^Build:\s+(.+)$""", RegexOption.MULTILINE)
    private val DUMPSYS = Regex("""^DUMP OF SERVICE(?: (?:CRITICAL|HIGH))? ([^:]+):?\s*$""")
    private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun parse(input: InputStream): Bugreport {
        val acc = Acc()
        try {
            ZipInputStream(input).use { zis ->
                while (true) {
                    val entry = try {
                        zis.nextEntry
                    } catch (e: ZipException) {
                        acc.warn("Die Datei ist kein gültiges ZIP.")
                        break
                    } ?: break
                    acc.entries++
                    if (acc.entries > MAX_ENTRIES) {
                        acc.warn("Die ZIP enthält zu viele Einträge und wurde abgelehnt.")
                        break
                    }
                    val name = sanitizeName(entry.name)
                    if (name == null) {
                        drain(zis, acc)
                        if (acc.overBudget) {
                            acc.warn("Die entpackte Größe überschreitet die Grenze und wurde abgelehnt.")
                            break
                        }
                        continue
                    }
                    when {
                        isDumpstate(name) -> parseDumpstate(CappedReader(zis, acc), acc)
                        isBoard(name) -> acc.boardText = readCappedText(zis, acc)
                        isFs(name) -> {
                            acc.fsPaths += devicePath(name)
                            drain(zis, acc)
                        }
                        else -> drain(zis, acc)
                    }
                    if (acc.overBudget) {
                        acc.warn("Die entpackte Größe überschreitet die Grenze und wurde abgelehnt.")
                        break
                    }
                }
            }
        } catch (e: ZipException) {
            acc.warn("Die Datei ist kein gültiges ZIP.")
        }
        return acc.toBugreport()
    }

    internal fun parseDumpstate(reader: CappedReader, acc: Acc) {
        var kind = Kind.HEADER
        var label = ""
        val buf = StringBuilder()
        var sectionBytes = 0
        var collecting = true

        fun finish() {
            if (!collecting && buf.isEmpty()) {
                kind = Kind.SKIP
                return
            }
            val text = buf.toString()
            buf.setLength(0)
            sectionBytes = 0
            collecting = true
            when (kind) {
                Kind.HEADER -> acc.takeHeader(text)
                Kind.PROCESSES -> if (acc.psText.isEmpty()) acc.psText = acc.retainText(text)
                Kind.PROPS -> acc.takeProps(text)
                Kind.LOG -> acc.logcat.append(acc.retainText(text))
                Kind.DUMPSYS -> if (label.isNotEmpty()) {
                    acc.retainText(text).takeIf { it.isNotEmpty() }?.let { acc.dumpsys.putIfAbsent(label, it) }
                }
                Kind.SKIP -> Unit
            }
        }

        fun start(next: Kind, nextLabel: String = "", wanted: Boolean = true) {
            finish()
            kind = next
            label = nextLabel
            collecting = wanted
        }

        while (true) {
            val line = reader.readLine() ?: break
            when {
                isDuration(line) -> Unit
                dumpsysName(line) != null -> {
                    val svc = dumpsysName(line)!!
                    start(Kind.DUMPSYS, svc, wanted = true)
                }
                isCommandHeader(line) -> {
                    val title = commandTitle(line)
                    when {
                        title.startsWith("PROCESSES AND THREADS") || title.startsWith("PROCESSES (") ->
                            start(Kind.PROCESSES)
                        title.startsWith("SYSTEM PROPERTIES") ->
                            start(Kind.PROPS)
                        title.startsWith("SYSTEM LOG") || title.startsWith("EVENT LOG") || title.startsWith("RADIO LOG") ->
                            start(Kind.LOG)
                        else -> start(Kind.SKIP, wanted = false)
                    }
                }
                isDashesOnly(line) -> Unit
                collecting -> {
                    val add = line.length + 1
                    if (sectionBytes + add > MAX_SECTION_BYTES) {
                        acc.truncated = true
                        collecting = false
                    } else {
                        buf.append(line).append('\n')
                        sectionBytes += add
                    }
                }
            }
        }
        finish()
    }

    internal fun parsePackageNames(packageDump: String): List<String> {
        val names = LinkedHashSet<String>()
        var inList = false
        for (line in packageDump.lineSequence()) {
            val trimmed = line.trim()
            when {
                trimmed == "Packages:" || trimmed == "Hidden system packages:" -> inList = true
                inList && trimmed.startsWith("Package [") -> {
                    val start = trimmed.indexOf('[') + 1
                    val end = trimmed.indexOf(']')
                    if (end > start) names += trimmed.substring(start, end)
                }
                inList && line.isNotEmpty() && !line[0].isWhitespace() &&
                    trimmed != "Packages:" && trimmed != "Hidden system packages:" &&
                    !trimmed.startsWith("Package [") -> inList = false
            }
        }
        return names.toList()
    }

    internal fun toPsNames(psText: String): String {
        val lines = psText.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return ""
        val header = lines.first().trim()
        val cols = header.split(Regex("""\s+"""))
        val nameIdx = cols.indexOfFirst { it.equals("NAME", true) || it.equals("CMD", true) }
        val pidIdx = cols.indexOfFirst { it.equals("PID", true) }
        val tidIdx = cols.indexOfFirst { it.equals("TID", true) }
        val names = LinkedHashSet<String>()
        val out = StringBuilder("NAME\n")
        if (nameIdx < 0 && cols.size == 1 && !header.contains(':')) {
            names += header
            for (line in lines.drop(1)) names += line.trim().substringAfterLast(' ')
        } else {
            for (line in lines.drop(1)) {
                val parts = line.trim().split(Regex("""\s+"""))
                if (parts.isEmpty()) continue
                if (pidIdx >= 0 && tidIdx >= 0 && pidIdx < parts.size && tidIdx < parts.size &&
                    parts[pidIdx] != parts[tidIdx]
                ) continue
                names += parts.last()
            }
        }
        for (n in names) out.append(n).append('\n')
        return out.toString()
    }

    internal fun parseGetprop(text: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (line in text.lineSequence()) {
            val m = PROP.find(line.trim()) ?: continue
            out[m.groupValues[1]] = m.groupValues[2]
        }
        return out
    }

    private fun Acc.takeHeader(text: String) {
        val kept = retainText(text)
        DUMPSTATE_DATE.find(kept)?.groupValues?.get(1)?.let {
            createdAt = runCatching { LocalDateTime.parse(it, DATE_FMT) }.getOrNull()
        }
        fingerprint = FINGERPRINT.find(kept)?.groupValues?.get(1).orEmpty()
        buildLine = BUILD_LINE.find(kept)?.groupValues?.get(1).orEmpty()
    }

    private fun Acc.takeProps(text: String) {
        if (props.isEmpty()) props.putAll(parseGetprop(retainText(text)))
    }

    private fun Acc.toBugreport(): Bugreport {
        val pkgDump = dumpsys["package"].orEmpty()
        val pkgs = parsePackageNames(pkgDump)
        val mfr = props["ro.product.manufacturer"] ?: props["ro.product.brand"]
        val model = props["ro.product.model"]
        val device = when {
            !mfr.isNullOrBlank() && !model.isNullOrBlank() -> "$mfr $model"
            fingerprint.isNotEmpty() -> fingerprint
            buildLine.isNotEmpty() -> buildLine
            else -> ""
        }
        return Bugreport(
            psText = psText,
            packages = pkgs,
            dumpsys = dumpsys,
            logcat = logcat.toString(),
            props = props,
            createdAt = createdAt,
            device = device,
            fsPaths = fsPaths,
            boardText = boardText,
            truncated = truncated,
            warnings = warnings,
        )
    }

    private fun Acc.warn(msg: String) {
        if (msg !in warnings) warnings += msg
    }

    internal class Acc {
        var entries = 0
        var used = 0L
        val overBudget get() = used > MAX_TOTAL_BYTES
        fun add(n: Int): Boolean {
            used += n
            return used <= MAX_TOTAL_BYTES
        }

        var psText = ""
        val dumpsys = LinkedHashMap<String, String>()
        val logcat = StringBuilder()
        val props = LinkedHashMap<String, String>()
        var createdAt: LocalDateTime? = null
        var fingerprint = ""
        var buildLine = ""
        val fsPaths = ArrayList<String>()
        var boardText = ""
        var truncated = false
        val warnings = ArrayList<String>()
        private var retainedBytes = 0

        fun retainText(text: String): String {
            if (text.isEmpty()) return text
            val bytes = text.toByteArray(Charsets.UTF_8)
            val room = MAX_RETAINED_TEXT_BYTES - retainedBytes
            if (room <= 0) {
                truncated = true
                return ""
            }
            if (bytes.size <= room) {
                retainedBytes += bytes.size
                return text
            }
            truncated = true
            retainedBytes = MAX_RETAINED_TEXT_BYTES
            return String(bytes, 0, room, Charsets.UTF_8)
        }
    }

    private enum class Kind { HEADER, PROCESSES, PROPS, LOG, DUMPSYS, SKIP }

    internal class CappedReader(private val input: InputStream, private val acc: Acc) {
        private val buf = ByteArray(64 * 1024)
        private var pos = 0
        private var end = 0

        private fun nextByte(): Int {
            if (pos >= end) {
                val n = input.read(buf)
                if (n <= 0) return -1
                pos = 0
                end = n
            }
            return buf[pos++].toInt() and 0xff
        }

        fun readLine(): String? {
            if (acc.overBudget) return null
            val line = ByteArrayOutputStream(128)
            var stored = 0
            var any = false
            while (true) {
                val b = nextByte()
                if (b < 0) {
                    if (!any) return null
                    break
                }
                any = true
                if (!acc.add(1)) break
                if (b == '\n'.code) break
                if (b == '\r'.code) continue
                if (stored < MAX_LINE_BYTES) {
                    line.write(b)
                    stored++
                } else {
                    acc.truncated = true
                }
            }
            return String(line.toByteArray(), Charsets.UTF_8)
        }
    }

    private fun readCappedText(input: InputStream, acc: Acc): String {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        var stored = 0
        while (true) {
            val n = input.read(buf)
            if (n <= 0) break
            if (!acc.add(n)) break
            val room = MAX_SECTION_BYTES - stored
            if (room <= 0) {
                acc.truncated = true
            } else {
                val keep = minOf(n, room)
                out.write(buf, 0, keep)
                stored += keep
                if (keep < n) acc.truncated = true
            }
        }
        val raw = out.toString(Charsets.UTF_8.name())
        return acc.retainText(raw)
    }

    private fun drain(input: InputStream, acc: Acc) {
        val buf = ByteArray(64 * 1024)
        while (true) {
            val n = input.read(buf)
            if (n <= 0) break
            if (!acc.add(n)) break
        }
    }

    private fun sanitizeName(raw: String): String? {
        val n = raw.replace('\\', '/').trimStart('/')
        if (n.split('/').any { it == ".." }) return null
        return n.ifEmpty { null }
    }

    private fun devicePath(zipName: String): String = "/" + zipName.removePrefix("FS/").trimStart('/')

    private fun isDumpstate(name: String): Boolean {
        val base = name.substringAfterLast('/')
        if (!base.endsWith(".txt", ignoreCase = true)) return false
        val lower = base.lowercase()
        if ("dumpstate_log" in lower || "dumpstate-log" in lower) return false
        if (isBoard(name)) return false
        return lower.startsWith("bugreport") || lower.startsWith("dumpstate")
    }

    private fun isBoard(name: String): Boolean {
        val base = name.substringAfterLast('/').lowercase()
        return base == "dumpstate_board.txt" || base == "dumpstate-board.txt"
    }

    private fun isFs(name: String) = name.startsWith("FS/")

    private fun isDuration(line: String): Boolean = line.contains("was the duration of", ignoreCase = true)

    private fun isDashesOnly(line: String): Boolean {
        val t = line.trim()
        return t.length >= 10 && t.all { it == '-' }
    }

    private fun isCommandHeader(line: String): Boolean {
        val t = line.trim()
        if (!t.startsWith("------")) return false
        if (isDuration(t)) return false
        return t.any { it.isLetter() }
    }

    private fun commandTitle(line: String): String =
        line.trim().trimStart('-').trimEnd('-').trim().substringBefore(" (").trim()

    private fun dumpsysName(line: String): String? = DUMPSYS.matchEntire(line.trim())?.groupValues?.get(1)?.trim()
}
