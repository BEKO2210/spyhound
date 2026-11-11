package de.belkis.spyhound

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocFormatException
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.ioc.Parsers
import de.belkis.spyhound.ioc.SourceContext
import de.belkis.spyhound.scan.*
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScanIocEdgeCasesTest {
    private val ctx = SourceContext("edge", Category.MERCENARY, "Edge")
    private fun ind(value: String, type: IocType) = Indicator(value, type, Category.MERCENARY, "Fam", "src")

    @Test fun appAnalysisSkipsSelfAndSystemAppRiskSignals() {
        val result = AppAnalyzer.analyze(listOf(
            AppRecord("self", apks = listOf(ApkFile("base.apk", null))),
            AppRecord("system", isSystem = true, apks = listOf(ApkFile("sys.apk", null))),
            AppRecord("sideload", installer = "storeish", hiddenFromLauncher = true),
        ), IocSet(emptyList()), "self")

        assertEquals(2, result.examined)
        assertEquals(1, result.leads.size)
        assertTrue(result.leads.single().detail.contains("kein Symbol"))
        // System apps are skipped; the sideloaded app without a readable certificate is an honest gap.
        assertEquals(setOf(GapKind.CERTIFICATES), result.gaps.map { it.kind }.toSet())
        assertTrue(result.ran)
    }

    @Test fun appAnalysisReportsUnhashedApksAndBothCertGaps() {
        val result = AppAnalyzer.analyze(listOf(AppRecord("one", apks = listOf(ApkFile("a.apk", null)))), IocSet(emptyList()), "self")
        assertEquals(setOf(GapKind.APP_FILES, GapKind.CERTIFICATES), result.gaps.map { it.kind }.toSet())
        assertTrue(result.ran)
    }

    @Test fun storeAppPowerLeadRequiresAccessibilityOrAdmin() {
        val apps = listOf(
            AppRecord("notify", installer = AppAnalyzer.STORES.first(), notificationAccess = true),
            AppRecord("admin", installer = AppAnalyzer.STORES.first(), deviceAdmin = true, accessibility = true),
        )
        val leads = AppAnalyzer.analyze(apps, IocSet(emptyList()), "self").leads
        assertEquals(1, leads.size)
        assertEquals(LeadKind.APP_POWERS, leads.single().kind)
        assertTrue(leads.single().detail.contains("Bedienungshilfe"))
        assertTrue(leads.single().detail.contains("Geräteadministrator"))
    }

    @Test fun samsungSystemUpdatersAreNotSideloading() {
        // Found on a Galaxy Tab S9 Ultra: Clock and the Buds plugins came through these and showed up as leads.
        val apps = listOf(
            AppRecord("com.sec.android.app.clockpackage", installer = "com.samsung.android.app.updatecenter", notificationAccess = true),
            AppRecord("com.samsung.accessory.zenithmgr", installer = "com.samsung.accessory.budsunitemgr", notificationAccess = true),
            AppRecord("sideloaded", installer = "com.sec.android.app.sbrowser", notificationAccess = true),
        )
        val leads = AppAnalyzer.analyze(apps, IocSet(emptyList()), "self").leads
        assertEquals(listOf(LeadKind.SIDELOADED_APP), leads.map { it.kind })
        assertTrue(leads.single().detail.contains("sbrowser"))
    }

    @Test fun honorStoreIsNotSideloading() {
        val apps = listOf(
            AppRecord("com.tencent.mm", installer = "com.hihonor.appmarket", notificationAccess = true),
            AppRecord("com.example.old", installer = "com.honor.appmarket", notificationAccess = true),
            AppRecord("sideloaded", installer = "com.honor.browser", notificationAccess = true),
        )
        val leads = AppAnalyzer.analyze(apps, IocSet(emptyList()), "self").leads
        assertEquals(listOf(LeadKind.SIDELOADED_APP), leads.map { it.kind })
        assertTrue(leads.single().detail.contains("com.honor.browser"))
    }

    @Test fun devicePatchBoundaryAndMalformedValues() {
        fun check(patch: String) = DeviceAnalyzer.analyze(
            DeviceState(emptyMap(), patch, 35, null, null, null, emptyList(), emptyList(), null),
            IocSet(emptyList()), java.time.LocalDate.of(2026, 11, 1),
        )
        assertTrue(check("2026-07-04").leads.none { it.kind == LeadKind.OLD_PATCH }) // exactly 120 days
        assertTrue(check("2026-07-03").leads.any { it.kind == LeadKind.OLD_PATCH })
        assertTrue(check("not-a-date").leads.none { it.kind == LeadKind.OLD_PATCH })
        assertEquals(GapFix.SHIZUKU, check("").gaps.single().fix)
    }

    @Test fun deviceWifiDebuggingTakesPrecedenceAndExpectedShellSuppressesUsb() {
        fun analyze(wifi: Boolean?, usb: Boolean?, expected: Boolean) = DeviceAnalyzer.analyze(
            DeviceState(mapOf("ro.boot.verifiedbootstate" to "yellow"), "", 35, usb, wifi, null, emptyList(), emptyList(), "PERMISSIVE"),
            IocSet(emptyList()), java.time.LocalDate.of(2026, 11, 1), expected,
        )
        val both = analyze(true, true, true)
        assertEquals(1, both.leads.count { it.kind == LeadKind.DEBUGGING })
        assertTrue(both.leads.single { it.kind == LeadKind.DEBUGGING }.where.contains("WLAN"))
        assertTrue(both.leads.any { it.kind == LeadKind.BOOTLOADER })
        assertTrue(both.leads.any { it.kind == LeadKind.SELINUX })
        assertFalse(analyze(false, true, true).leads.any { it.kind == LeadKind.DEBUGGING })
    }

    @Test fun messageAnalyzerCountsMessagesAndReportsDomainAndUrlIndicators() {
        val set = IocSet(listOf(ind("evil.example", IocType.DOMAIN), ind("https://evil.example/path", IocType.URL)))
        val result = MessageAnalyzer.analyze(listOf(Message("+1", 0, "https://evil.example/path"), Message("+2", 1, "ordinary")), set)
        assertEquals(2, result.examined)
        assertEquals(2, result.findings.size) // host and URL are distinct indicator types
        assertTrue(result.ran)
    }

    @Test fun scannerDeduplicatesTokensAndTrimsTerminalPunctuation() {
        val set = IocSet(listOf(ind("evil.example", IocType.DOMAIN), ind("/tmp/bad", IocType.FILE_PATH)))
        val scanner = TextScanner(set)
        assertEquals(listOf("evil.example"), scanner.hosts("evil.example evil.example.").map { it.token }.distinct())
        assertEquals(listOf("/tmp/bad"), scanner.paths("/tmp/bad, /tmp/bad.").map { it.token }.distinct())
        // Look-alike hosts are different domains and must not count as hits.
        assertTrue(scanner.all("prefixnot evil.example-suffix notevil.example").isEmpty())
    }

    @Test fun domainSuffixMatchingChoosesMostSpecificAndRejectsBareTld() {
        val set = IocSet(listOf(ind("example.com", IocType.DOMAIN), ind("evil.example.com", IocType.DOMAIN)))
        assertEquals("evil.example.com", set.lookupHost("x.evil.example.com")?.value)
        assertEquals("example.com", set.lookupHost("x.example.com")?.value)
        assertNull(set.lookupHost("com"))
        assertNull(set.lookupHost("notexample.com"))
    }

    @Test fun parsersHandleMissingColumnsAndMalformedDocuments() {
        assertFailsWith<IocFormatException> { Parsers.stix2("{broken", ctx) }
        assertFailsWith<IocFormatException> { Parsers.stix2("{}", ctx) }
        assertEquals(emptyList(), Parsers.stix2("{\"objects\":[]}", ctx))
        assertFailsWith<IocFormatException> { Parsers.parse("unknown", "", ctx) }
    }

    @Test fun mispAndTsvIgnoreUnsupportedOrIncompleteRows() {
        assertFailsWith<IocFormatException> { Parsers.mispCsv("uuid,value\n1,x", ctx) }
        val rows = Parsers.mispCsv("value,type\nx,unsupported\n,domain\ny,domain", ctx)
        assertEquals(listOf("y"), rows.map { it.value })
        val tsv = Parsers.curatedTsv("# comment\n\n\tno\nvalue\tx\ty\tsource\tfamily\nshort\tx\n", ctx)
        assertEquals(listOf("value"), tsv.map { it.value })
        assertEquals("source", tsv.single().sourceId)
    }

    @Test fun csvDropsRowWithUnterminatedQuote() {
        // One malformed row must neither become an indicator nor take the whole feed down.
        assertEquals(listOf(listOf("type", "value")), Parsers.csvRows("type,value\ndomain,\"unterminated"))
    }
}
