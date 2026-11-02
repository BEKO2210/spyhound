package de.belkis.spyhound.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.belkis.spyhound.ioc.IocStore
import de.belkis.spyhound.scan.Report
import de.belkis.spyhound.scan.ScanEngine

internal object BackgroundScanRuntime {
    var scanner: suspend (Context) -> Report = { context ->
        val loaded = IocStore(context).load()
        // Background run stays in basic mode: no shell, no deep checks, no network updates.
        ScanEngine(context, loaded, shell = null).run { }
    }
    var notifier: (Context, Int) -> Unit = { context, count ->
        BackgroundScanNotifier.notifyNewFindings(context, count)
    }
}

class BackgroundScanWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!BackgroundScanPreferences.isWeeklyEnabled(applicationContext)) return Result.success()
        val report = runCatching { BackgroundScanRuntime.scanner(applicationContext) }.getOrNull()
            ?: return Result.retry()
        val previous = BackgroundScanPreferences.getLastFindingKeys(applicationContext)
        val current = report.findings.map(BackgroundFindingDiff::keyOf).toSet()
        val newFindings = BackgroundFindingDiff.newFindings(previous, report.findings)
        BackgroundScanPreferences.setLastFindingKeys(applicationContext, current)
        if (newFindings.isNotEmpty()) {
            BackgroundReportStore.save(applicationContext, report)
            BackgroundScanRuntime.notifier(applicationContext, newFindings.size)
        }
        return Result.success()
    }
}
