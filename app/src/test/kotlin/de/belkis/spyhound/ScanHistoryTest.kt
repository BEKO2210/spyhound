package de.belkis.spyhound

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.belkis.spyhound.background.BackgroundReportStore
import de.belkis.spyhound.background.BackgroundScanPreferences
import de.belkis.spyhound.background.BackgroundScanRuntime
import de.belkis.spyhound.background.BackgroundScanWorker
import de.belkis.spyhound.background.ScanHistoryEntry
import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.CheckResult
import de.belkis.spyhound.scan.GapFix
import de.belkis.spyhound.scan.GapKind
import de.belkis.spyhound.scan.LeadKind
import de.belkis.spyhound.scan.Report
import de.belkis.spyhound.scan.TextsDe
import de.belkis.spyhound.ui.screens.formatHistoryTimestamp
import de.belkis.spyhound.ui.screens.historyItemContentDescription
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.work.testing.TestListenableWorkerBuilder
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScanHistoryTest {
    private lateinit var context: Context
    private lateinit var defaultScanner: suspend (Context) -> Report
    private lateinit var defaultNotifier: (Context, Int) -> Unit

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BackgroundReportStore.clearHistory(context)
        defaultScanner = BackgroundScanRuntime.scanner
        defaultNotifier = BackgroundScanRuntime.notifier
    }

    @After
    fun tearDown() {
        BackgroundReportStore.clearHistory(context)
        BackgroundScanPreferences.setWeeklyEnabled(context, false)
        BackgroundScanRuntime.scanner = defaultScanner
        BackgroundScanRuntime.notifier = defaultNotifier
    }

    @Test
    fun roundTripPreservesReportAndMetadata() {
        val check = CheckResult("apps", TextsDe).apply {
            examined = 84
            ran = true
            hit(
                where = "com.stalker.agent",
                detail = "Paketname auf Indikatorliste",
                ind = Indicator(
                    value = "com.stalker.agent",
                    type = IocType.APP_ID,
                    category = Category.STALKERWARE,
                    family = "SpyNote",
                    sourceId = "echap",
                    androidArtifacts = true,
                ),
            )
            lead(
                where = "com.unknown.helper",
                detail = "Nicht aus dem Store installiert",
                why = "Sideload",
                kind = LeadKind.SIDELOADED_APP,
            )
            gap(
                detail = "Kein SMS-Zugriff",
                fix = GapFix.PERMISSION_SMS,
                kind = GapKind.OTHER,
            )
        }
        val report = Report(
            startedAt = 1_793_891_990_000L,
            finishedAt = 1_793_892_000_000L,
            access = AccessLevel.SHELL,
            indicatorCount = 41_200,
            indicatorSources = 7,
            indicatorSourcesMissing = 1,
            checks = listOf(check),
            device = "Pixel 8, Android 14",
            oldestEvidence = mapOf("logcat" to 1_793_800_000_000L),
            title = "Bugreport: report.zip",
        )

        val saved = BackgroundReportStore.record(context, report, background = true)
        val loaded = BackgroundReportStore.history(context)

        assertEquals(1, loaded.size)
        val entry = loaded.single()
        assertEquals(saved.id, entry.id)
        assertTrue(entry.background)
        assertEquals(AccessLevel.SHELL, entry.access)
        assertEquals(1, entry.findingCount)
        assertEquals(1, entry.leadCount)
        assertEquals(1, entry.gapCount)
        assertEquals(report.startedAt, entry.report.startedAt)
        assertEquals(report.finishedAt, entry.report.finishedAt)
        assertEquals(report.indicatorCount, entry.report.indicatorCount)
        assertEquals(report.indicatorSources, entry.report.indicatorSources)
        assertEquals(report.indicatorSourcesMissing, entry.report.indicatorSourcesMissing)
        assertEquals(report.device, entry.report.device)
        assertNull(entry.report.title)
        assertEquals(report.oldestEvidence, entry.report.oldestEvidence)
        assertEquals("SpyNote", entry.report.findings.single().indicator.family)
        assertEquals(LeadKind.SIDELOADED_APP, entry.report.leads.single().kind)
        assertEquals(GapKind.OTHER, entry.report.gaps.single().kind)
    }

    @Test
    fun orderingIsNewestFirstAndCapDropsOldest() {
        val total = BackgroundReportStore.MAX_HISTORY + 5
        // Record in mixed order to verify sorting and cap together.
        val timestamps = (1..total).map { it * 10_000L }.shuffled(kotlin.random.Random(42))
        timestamps.forEach { ts ->
            BackgroundReportStore.record(
                context = context,
                report = sampleReport(finishedAt = ts),
                background = (ts % 20_000L == 0L),
            )
        }

        val history = BackgroundReportStore.history(context)
        assertEquals(BackgroundReportStore.MAX_HISTORY, history.size)
        val expectedTimestamps = (6..total).map { it * 10_000L }.sortedDescending()
        assertEquals(expectedTimestamps, history.map { it.timestamp })
    }

    @Test
    fun corruptAndOldFormatEntriesAreSkippedWithoutCrash() {
        val validOld = sampleReport(finishedAt = 100_000L, access = AccessLevel.APP)
        val validNew = sampleReport(finishedAt = 200_000L, access = AccessLevel.ROOT)
        BackgroundReportStore.record(context, validOld, background = false)
        BackgroundReportStore.record(context, validNew, background = true)

        val validArray = BackgroundReportStore.encodeHistory(BackgroundReportStore.history(context))
        val mixedArray = JSONArray().apply {
            put("not-a-json-object")
            put(JSONObject().put("legacyFormat", 1).put("summary", "old entry without checks"))
            put(
                JSONObject().apply {
                    put("id", 999L)
                    put(
                        "report",
                        JSONObject().apply {
                            put("startedAt", 150_000L)
                            put("finishedAt", 150_500L)
                            put("access", "INVALID_ACCESS_LEVEL")
                            put("checks", JSONArray())
                        },
                    )
                },
            )
            put(
                JSONObject().apply {
                    put("id", 998L)
                    put(
                        "report",
                        JSONObject().apply {
                            put("startedAt", -5L)
                            put("finishedAt", 150_500L)
                            put("access", "APP")
                            put("checks", JSONArray())
                        },
                    )
                },
            )
            repeat(validArray.length()) { i -> put(validArray.get(i)) }
        }

        val decoded = BackgroundReportStore.decodeHistory(mixedArray.toString())
        assertEquals(2, decoded.size)
        assertEquals(listOf(200_000L, 100_000L), decoded.map { it.timestamp })
        assertTrue(decoded[0].background)
        assertFalse(decoded[1].background)

        // Completely broken top-level JSON never crashes and yields an empty list.
        assertTrue(BackgroundReportStore.decodeHistory("{corrupt-json").isEmpty())
    }

    @Test
    fun deleteRestoreAndClearWorkAsExpected() {
        val e1 = BackgroundReportStore.record(context, sampleReport(finishedAt = 10_000L))
        val e2 = BackgroundReportStore.record(context, sampleReport(finishedAt = 20_000L))
        val e3 = BackgroundReportStore.record(context, sampleReport(finishedAt = 30_000L))

        val removed = BackgroundReportStore.delete(context, e2.id)
        assertNotNull(removed)
        assertEquals(listOf(e3.id, e1.id), BackgroundReportStore.history(context).map { it.id })
        assertNull(BackgroundReportStore.delete(context, 999_999L))

        BackgroundReportStore.restore(context, removed!!)
        assertEquals(listOf(e3.id, e2.id, e1.id), BackgroundReportStore.history(context).map { it.id })

        BackgroundReportStore.clearHistory(context)
        assertTrue(BackgroundReportStore.history(context).isEmpty())
    }

    @Test
    fun backgroundWorkerRecordsCheckInHistory() = runBlocking {
        BackgroundScanPreferences.setWeeklyEnabled(context, true)
        BackgroundScanRuntime.scanner = { sampleReport(finishedAt = 500_000L) }
        BackgroundScanRuntime.notifier = { _, _ -> }

        val worker = TestListenableWorkerBuilder<BackgroundScanWorker>(context).build()
        worker.doWork()

        val history = BackgroundReportStore.history(context)
        assertEquals(1, history.size)
        assertTrue(history.single().background)
        assertEquals(500_000L, history.single().timestamp)
    }

    @Test
    fun contentDescriptionIncludesFormattedDateDepthAndCounts() {
        val zone = ZoneId.of("Europe/Berlin")
        // 2026-11-05 16:20 CET -> 1793892000000L
        val ts = 1_793_892_000_000L
        val check = CheckResult("apps", TextsDe).apply {
            examined = 50
            ran = true
            lead("com.sideload.app", "Sideload", "", LeadKind.SIDELOADED_APP)
            gap("SMS", GapFix.PERMISSION_SMS, GapKind.OTHER)
            gap("Files", GapFix.PERMISSION_FILES, GapKind.OTHER)
        }
        val entry = ScanHistoryEntry(
            id = ts,
            report = sampleReport(finishedAt = ts, access = AccessLevel.SHELL, checks = listOf(check)),
            background = true,
        )

        val deContext = localizedContext("de")
        val deDate = formatHistoryTimestamp(ts, Locale.GERMAN, zone)
        assertEquals("05.11.2026, 16:20", deDate)

        val deDesc = historyItemContentDescription(deContext, entry, Locale.GERMAN, zone)
        assertTrue(deDesc.contains("05.11.2026, 16:20"))
        assertTrue(deDesc.contains("Tief"))
        assertTrue(deDesc.contains("Hintergrund"))
        assertTrue(deDesc.contains("0 Funde"))
        assertTrue(deDesc.contains("1 Hinweise"))
        assertTrue(deDesc.contains("2 Lücken"))

        val enContext = localizedContext("en")
        val enDesc = historyItemContentDescription(enContext, entry, Locale.ENGLISH, zone)
        assertTrue(enDesc.contains("05 Nov 2026, 16:20"))
        assertTrue(enDesc.contains("Deep"))
        assertTrue(enDesc.contains("Background"))
        assertTrue(enDesc.contains("0 Findings"))
        assertTrue(enDesc.contains("1 Leads"))
        assertTrue(enDesc.contains("2 Gaps"))
    }

    @Test
    fun persistedFindingsExcludeSendersPrivateUrlsAndFilePaths() {
        val check = CheckResult("SMS-Links", TextsDe).apply {
            ran = true
            hit("SMS von +49123456789", "https://bad.example/private?token=secret",
                Indicator("bad.example", IocType.DOMAIN, Category.MERCENARY, "Example", "test"))
            hit("/storage/emulated/0/private-document.pdf", "private file contents",
                Indicator("abc123", IocType.SHA256, Category.MALWARE, "Example", "test"))
        }
        val original = sampleReport(100_000L, checks = listOf(check)).copy(title = "private-document.zip")
        val entry = BackgroundReportStore.record(context, original)
        val json = BackgroundReportStore.encodeHistory(listOf(entry)).toString()
        listOf("+49123456789", "token=secret", "private-document", "private file contents").forEach {
            assertFalse(json.contains(it))
        }
        assertEquals(2, entry.findingCount)
        assertEquals("bad.example", entry.report.findings.first().where)
        assertEquals("SMS von +49123456789", original.findings.first().where)
    }

    @Test
    fun equalTimestampsHaveDistinctIdsAndNewestInsertionFirst() {
        val first = BackgroundReportStore.record(context, sampleReport(100_000L))
        val second = BackgroundReportStore.record(context, sampleReport(100_000L))
        assertTrue(second.id != first.id)
        assertEquals(listOf(second.id, first.id), BackgroundReportStore.history(context).map { it.id })
    }

    @Test
    fun malformedVersionAndUnknownIndicatorTypeSkipOnlyThatEntry() {
        val check = CheckResult("Apps", TextsDe).apply {
            hit("app", "detail", Indicator("com.example.test", IocType.APP_ID, Category.STALKERWARE, "Example", "test"))
        }
        BackgroundReportStore.record(context, sampleReport(100_000L, checks = listOf(check)))
        val entries = BackgroundReportStore.encodeHistory(BackgroundReportStore.history(context))
        val badVersion = JSONObject(entries.getJSONObject(0).toString()).put("version", 999)
        val badType = JSONObject(entries.getJSONObject(0).toString())
        badType.getJSONObject("report").getJSONArray("checks").getJSONObject(0)
            .getJSONArray("findings").getJSONObject(0).getJSONObject("indicator").put("type", "UNKNOWN")
        entries.put(badVersion).put(badType)
        assertEquals(1, BackgroundReportStore.decodeHistory(entries.toString()).size)
    }

    @Test
    fun concurrentManualAndBackgroundWritesDoNotLoseEntries() {
        val start = java.util.concurrent.CountDownLatch(1)
        val pool = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val writes = (1..20).map { index ->
                pool.submit {
                    start.await()
                    BackgroundReportStore.record(context, sampleReport(index * 10_000L), index % 2 == 0)
                }
            }
            start.countDown()
            writes.forEach { it.get(10, java.util.concurrent.TimeUnit.SECONDS) }
            assertEquals(20, BackgroundReportStore.history(context).size)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun missingFindingArrayDoesNotTurnCorruptReportIntoCleanResult() {
        val entry = BackgroundReportStore.record(context, sampleReport(100_000L))
        val array = BackgroundReportStore.encodeHistory(listOf(entry))
        array.getJSONObject(0).getJSONObject("report").getJSONArray("checks").getJSONObject(0).remove("findings")
        assertTrue(BackgroundReportStore.decodeHistory(array.toString()).isEmpty())
    }

    private fun localizedContext(language: String): Context {
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language))
        return context.createConfigurationContext(config)
    }

    private fun sampleReport(
        finishedAt: Long,
        access: AccessLevel = AccessLevel.APP,
        checks: List<CheckResult> = listOf(
            CheckResult("apps", TextsDe).apply {
                examined = 12
                ran = true
            },
        ),
    ): Report = Report(
        startedAt = finishedAt - 1_000L,
        finishedAt = finishedAt,
        access = access,
        indicatorCount = 100,
        indicatorSources = 5,
        indicatorSourcesMissing = 0,
        checks = checks,
        device = "Pixel",
    )
}
