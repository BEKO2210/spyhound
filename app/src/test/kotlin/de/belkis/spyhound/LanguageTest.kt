package de.belkis.spyhound

import de.belkis.spyhound.bugreport.Bugreport
import de.belkis.spyhound.bugreport.BugreportAnalyzer
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.AppAnalyzer
import de.belkis.spyhound.scan.AppRecord
import de.belkis.spyhound.scan.MessageAnalyzer
import de.belkis.spyhound.scan.ShellAnalyzer
import de.belkis.spyhound.scan.DeviceAnalyzer
import de.belkis.spyhound.scan.DeviceState
import de.belkis.spyhound.scan.Report
import de.belkis.spyhound.scan.ReportText
import de.belkis.spyhound.scan.TextsEn
import de.belkis.spyhound.scan.localized
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class LanguageTest {
    @Test fun missingSmsPermissionIsEnglish() {
        val check = MessageAnalyzer.analyze(null, IocSet(emptyList()), TextsEn)
        val report = report(check)
        assertEquals("SMS links", report.checks.single().name)
        assertEquals("SMS was not checked. Read SMS permission is missing.", report.gaps.single().detail)
    }

    @Test fun appGapAndSharedReportAreEnglish() {
        val check = AppAnalyzer.analyze(listOf(AppRecord("com.example", apks = emptyList())), IocSet(emptyList()), "de.belkis.spyhound", TextsEn)
        val report = report(check)
        assertContains(report.gaps.single().detail, "Signing certificate could not be read")
        val shared = ReportText.render(report, "1.0", TextsEn)
        assertContains(shared, "NOT CHECKED")
        assertContains(shared, "No known traces in checked areas")
    }

    @Test fun deviceShellAndBugReportGapsAreEnglish() {
        val iocs = IocSet(emptyList())
        val device = DeviceAnalyzer.analyze(
            DeviceState(emptyMap(), "", 36, null, null, null, emptyList(), emptyList(), null), iocs, texts = TextsEn,
        )
        assertEquals("System properties (getprop) could not be read.", report(device).gaps.single().detail)
        assertEquals("Process list was empty or unreadable.",
            report(ShellAnalyzer.processes("", iocs, TextsEn)).gaps.single().detail)
        val bug = BugreportAnalyzer.analyze(Bugreport(), iocs, emptySet(), texts = TextsEn)
        assertContains(report(bug.first { it.gaps.isNotEmpty() }).gaps.single().detail, "missing from bug report")
    }

    private fun report(check: de.belkis.spyhound.scan.CheckResult) = Report(
        startedAt = 0,
        finishedAt = 1000,
        access = AccessLevel.APP,
        indicatorCount = 0,
        indicatorSources = 0,
        indicatorSourcesMissing = 0,
        checks = listOf(check),
        device = "Test device",
    )

    @Test
    fun checkIdStaysUntranslated() {
        // The result screen matches checks by id; a translated name broke the SMS chip in English.
        val r = de.belkis.spyhound.scan.CheckResult(MessageAnalyzer.NAME, TextsEn)
        assertEquals(MessageAnalyzer.NAME, r.id)
        assertEquals("SMS links", r.name)
        // The English report rebuilds every check; the id must survive that too.
        val report = Report(0, 0, AccessLevel.APP, 0, 0, 0, listOf(r.apply { ran = true }), "Pixel")
        val en = report.localized(TextsEn).checks.single()
        assertEquals(MessageAnalyzer.NAME, en.id)
        assertEquals("SMS links", en.name)
    }
}
