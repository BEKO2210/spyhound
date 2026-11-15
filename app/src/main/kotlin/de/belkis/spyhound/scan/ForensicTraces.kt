package de.belkis.spyhound.scan

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocType

/**
 * Signs of phone-forensics hardware (Cellebrite) and related Android artifacts in logs and on disk.
 * Indicators from Zero Breach Toolkit (Apache-2.0), after Amnesty International research.
 */
object ForensicTraces {
    const val SOURCE_AMNESTY_CELLEBRITE = "amnesty-cellebrite-serbia-2025-02"
    const val SOURCE_GOOGLE_VARISTON = "google-variston-heliconia-2023-03"

    const val WHERE_CELLEBRITE_USB = "Spur eines Auslesegeräts (Cellebrite) im Systemprotokoll"
    const val DETAIL_CELLEBRITE_USB =
        "Im Systemprotokoll steht ein USB-Gerät, das Forscher dem Entsperr-Werkzeug von Cellebrite zuordnen " +
            "(Herstellerkennung %1\$s, Firmware-Version 12.34). Das spricht dafür, dass das Telefon an ein " +
            "Handy-Auslesegerät angeschlossen war. Fehlt der Eintrag, beweist das nichts."
    const val WHERE_VARISTON_PROP = "Verdächtige Systemeigenschaft (Variston Heliconia)"
    const val DETAIL_VARISTON_PROP =
        "Die Systemeigenschaft %1\$s ist gesetzt. Forscher ordnen sie der Heliconia-Angriffskette zu. " +
            "Das ist kein normaler Android-Wert. Fehlt sie, beweist das nichts."
    const val WHERE_VARISTON_PATH = "Verdächtiger Ordner (Variston Heliconia)"
    const val DETAIL_VARISTON_PATH =
        "Der Ordner %1\$s ist vorhanden. Forscher ordnen ihn der Heliconia-Angriffskette zu. " +
            "Fehlt er, beweist das nichts."

    const val GAP_SHORT_LOGCAT =
        "Die gespeicherten Systemprotokolle reichen weniger als 24 Stunden zurück. Eine ältere Verbindung " +
            "mit einem Handy-Auslesegerät (z. B. Cellebrite) ist darin möglicherweise nicht mehr sichtbar."

    const val LOGCAT_COVERAGE_HOURS = 24L

    private const val USB_VERSION = "12.34"

    private val CELLEBRITE_USB = mapOf(
        "045e" to "076c",
        "04f2" to "b071",
        "1130" to "3101",
        "041e" to "3000",
        "0763" to "2012",
    )

    private val VARISTON_PROPS = setOf("sys.brand.note", "sys.brand.notes", "sys.brand.doc")
    private val VARISTON_PATHS = setOf("/data/local/tmp/dropbox")

    private val USB_ATTACHED = Regex(
        """USB device attached: vidpid ([0-9a-fA-F]{4}):([0-9a-fA-F]{4}) """ +
            """mfg/product/ver/serial [^/\r\n]*/[^/\r\n]*/([^/\r\n]*)/""",
    )
    private val USB_PRODUCT = Regex("""(?<![0-9A-Za-z/])([0-9a-fA-F]{1,4})/([0-9a-fA-F]{1,4})/1234(?![0-9A-Za-z])""")

    data class UsbMatch(val vid: String, val pid: String)

    /** All Cellebrite USB emulation lines in [logcat], one entry per matching line. */
    fun scanLogcat(logcat: String): List<UsbMatch> {
        val out = ArrayList<UsbMatch>()
        for (line in logcat.lineSequence()) out += matchesOnLine(line)
        return out
    }

    fun onLogcatLine(result: CheckResult, line: String, reported: MutableSet<String>) {
        for (match in matchesOnLine(line)) {
            val key = "${match.vid}:${match.pid}"
            if (!reported.add(key)) continue
            result.hit(
                WHERE_CELLEBRITE_USB,
                DETAIL_CELLEBRITE_USB.format("${match.vid}:${match.pid}"),
                usbIndicator(match.vid, match.pid),
            )
        }
    }

    private fun matchesOnLine(line: String): List<UsbMatch> {
        if (!line.contains("1234") && !line.contains("12.34")) return emptyList()
        val out = ArrayList<UsbMatch>()
        for (match in USB_ATTACHED.findAll(line)) {
            val vid = match.groupValues[1].lowercase()
            val pid = match.groupValues[2].lowercase()
            if (CELLEBRITE_USB[vid] == pid && match.groupValues[3].trim() == USB_VERSION) {
                out += UsbMatch(vid, pid)
            }
        }
        for (match in USB_PRODUCT.findAll(line)) {
            val vid = match.groupValues[1].lowercase().padStart(4, '0')
            val pid = match.groupValues[2].lowercase().padStart(4, '0')
            if (CELLEBRITE_USB[vid] == pid) out += UsbMatch(vid, pid)
        }
        return out
    }

    fun onGetprop(result: CheckResult, props: Map<String, String>, skip: (String) -> Boolean = { false }) {
        for (name in VARISTON_PROPS) {
            if (name !in props || skip(name)) continue
            result.hit(WHERE_VARISTON_PROP, DETAIL_VARISTON_PROP.format(name), varistonPropIndicator(name))
        }
    }

    fun onFilePath(result: CheckResult, path: String, iocs: IocSet) {
        if (path !in VARISTON_PATHS) return
        if (iocs.lookup(IocType.FILE_PATH, path) != null) return
        result.hit(WHERE_VARISTON_PATH, DETAIL_VARISTON_PATH.format(path), varistonPathIndicator(path))
    }

    fun oldestLogcatTimestamp(logcat: String, now: Long): Long? {
        var oldest: Long? = null
        for (line in logcat.lineSequence()) {
            TimeRange.logcatTimestamp(line, now)?.let { t -> oldest = minOf(oldest ?: t, t) }
        }
        return oldest
    }

    fun logcatCoverageGap(result: CheckResult, oldestLogMs: Long?, nowMs: Long) {
        val oldest = oldestLogMs ?: return
        val hours = ((nowMs - oldest) / 3_600_000L).coerceAtLeast(0)
        if (hours < LOGCAT_COVERAGE_HOURS) {
            result.gap(GAP_SHORT_LOGCAT, kind = GapKind.SHORT_LOGCAT)
        }
    }

    private fun usbIndicator(vid: String, pid: String) = Indicator(
        "cellebrite usb $vid:$pid",
        IocType.FILE_NAME,
        Category.MERCENARY,
        "Cellebrite",
        SOURCE_AMNESTY_CELLEBRITE,
    )

    private fun varistonPropIndicator(name: String) = Indicator(
        name,
        IocType.ANDROID_PROP,
        Category.MERCENARY,
        "Variston Heliconia",
        SOURCE_GOOGLE_VARISTON,
    )

    private fun varistonPathIndicator(path: String) = Indicator(
        path,
        IocType.FILE_PATH,
        Category.MERCENARY,
        "Variston Heliconia",
        SOURCE_GOOGLE_VARISTON,
    )
}