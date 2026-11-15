package de.belkis.spyhound

import de.belkis.spyhound.bugreport.BUGREPORT_PREFIX
import de.belkis.spyhound.bugreport.Bugreport
import de.belkis.spyhound.bugreport.BugreportAnalyzer
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.scan.ForensicTraces
import de.belkis.spyhound.scan.GapKind
import de.belkis.spyhound.scan.ShellAnalyzer
import de.belkis.spyhound.scan.Verdict
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ForensicTracesTest {
    private val iocs = IocSet(emptyList())

    @Test fun cellebriteUsbLineMatches() {
        val line = "USB device attached: vidpid 04f2:b071 mfg/product/ver/serial Video/Video Device/12.34/00001"
        assertEquals(listOf(ForensicTraces.UsbMatch("04f2", "b071")), ForensicTraces.scanLogcat(line))
    }

    @Test fun kernelProductLineMatches() {
        val line = "uevent: PRODUCT=4f2/b071/1234/"
        assertEquals(listOf(ForensicTraces.UsbMatch("04f2", "b071")), ForensicTraces.scanLogcat(line))
    }

    @Test fun wrongVersionDoesNotMatch() {
        val line = "USB device attached: vidpid 04f2:b071 mfg/product/ver/serial Video/Video Device/1.00/00001"
        assertTrue(ForensicTraces.scanLogcat(line).isEmpty())
    }

    @Test fun unrelatedUsbDoesNotMatch() {
        val line = "USB device attached: vidpid 046d:c52b mfg/product/ver/serial Logitech/Keyboard/12.34/1"
        assertTrue(ForensicTraces.scanLogcat(line).isEmpty())
    }

    @Test fun malformedLineDoesNotMatch() {
        assertTrue(ForensicTraces.scanLogcat("not usb at all").isEmpty())
        assertTrue(ForensicTraces.scanLogcat("USB device attached: vidpid zz:qq broken").isEmpty())
    }

    @Test fun shellLogsProduceFinding() {
        val logcat = "I/UsbHost: USB device attached: vidpid 045e:076c mfg/product/ver/serial HID/HID Device/12.34/1"
        val r = ShellAnalyzer.logs(mapOf("logcat" to logcat), iocs)
        assertEquals(1, r.findings.size)
        assertEquals(ForensicTraces.WHERE_CELLEBRITE_USB, r.findings.single().where)
        assertEquals(Verdict.SPYWARE_INDICATOR, de.belkis.spyhound.scan.Report(0, 0, de.belkis.spyhound.scan.AccessLevel.SHELL, 0, 0, 0, listOf(r), "").verdict)
    }

    @Test fun bugreportLogcatFindingAndShortLogGap() {
        val now = LocalDateTime.of(2026, 11, 15, 12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val logcat = """
            11-15 11:30:00.000 I/UsbHost: USB device attached: vidpid 0763:2012 mfg/product/ver/serial Sound/Sound Device/12.34/x
            11-15 11:45:00.000 I/Other: ok
        """.trimIndent()
        val checks = BugreportAnalyzer.analyze(
            Bugreport(logcat = logcat, dumpsys = mapOf("x" to "y"), device = "d"),
            iocs,
            emptySet(),
            LocalDate.of(2026, 11, 15),
            now,
        )
        val logs = checks.first { it.name == BUGREPORT_PREFIX + ShellAnalyzer.LOGS }
        assertEquals(1, logs.findings.size)
        assertTrue(logs.gaps.any { it.kind == GapKind.SHORT_LOGCAT }, logs.gaps.toString())
    }
}