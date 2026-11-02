package de.belkis.spyhound

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import de.belkis.spyhound.background.BackgroundFindingDiff
import de.belkis.spyhound.background.BackgroundScanNotifier
import de.belkis.spyhound.background.BackgroundScanPreferences
import de.belkis.spyhound.background.BackgroundScanRuntime
import de.belkis.spyhound.background.BackgroundScanWorker
import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.CheckResult
import de.belkis.spyhound.scan.Report
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackgroundScanWorkerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    companion object {
        private var notificationCalls = 0
        private var notifiedWith = 0

        private fun fakeScanner(@Suppress("UNUSED_PARAMETER") context: Context): Report = fakeReportStatic()

        private fun notifyReal(context: Context, count: Int) {
            BackgroundScanNotifier.notifyNewFindings(context, count)
        }

        private fun countOnly(@Suppress("UNUSED_PARAMETER") context: Context, @Suppress("UNUSED_PARAMETER") count: Int) {
            notificationCalls++
        }

        private fun captureCount(@Suppress("UNUSED_PARAMETER") context: Context, count: Int) {
            notifiedWith = count
        }

        private fun fakeReportStatic(): Report {
            val check = CheckResult("Apps").apply {
                ran = true
                examined = 1
                hit(
                    where = "Paket com.example.spy",
                    detail = "Treffer",
                    ind = Indicator(
                        value = "com.example.spy",
                        type = IocType.APP_ID,
                        category = Category.MERCENARY,
                        family = "Demo",
                        sourceId = "demo",
                    ),
                )
            }
            return Report(
                startedAt = 1L,
                finishedAt = 2L,
                access = AccessLevel.APP,
                indicatorCount = 1,
                indicatorSources = 1,
                indicatorSourcesMissing = 0,
                checks = listOf(check),
                device = "demo",
            )
        }
    }

    @After
    fun tearDown() {
        notificationCalls = 0
        notifiedWith = 0
        BackgroundScanRuntime.scanner = ::fakeScanner
        BackgroundScanRuntime.notifier = ::notifyReal
        context.getSharedPreferences("spyhound_background_scan", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("spyhound_background_report", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun notifiesOnlyWhenNewFindingAppears() {
        BackgroundScanPreferences.setWeeklyEnabled(context, true)
        val finding = fakeReport().findings.first()
        BackgroundScanPreferences.setLastFindingKeys(context, setOf(BackgroundFindingDiff.keyOf(finding)))

        BackgroundScanRuntime.scanner = ::fakeScanner
        BackgroundScanRuntime.notifier = ::countOnly

        val result = runWorker()

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(0, notificationCalls)
    }

    @Test
    fun sendsNotificationForFreshFinding() {
        BackgroundScanPreferences.setWeeklyEnabled(context, true)
        BackgroundScanPreferences.setLastFindingKeys(context, emptySet())

        BackgroundScanRuntime.scanner = ::fakeScanner
        BackgroundScanRuntime.notifier = ::captureCount

        val result = runWorker()

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(1, notifiedWith)
    }

    private fun runWorker(): ListenableWorker.Result {
        val worker = TestListenableWorkerBuilder<BackgroundScanWorker>(context).build()
        return worker.startWork().get()
    }

    private fun fakeReport(): Report = fakeReportStatic()
}
