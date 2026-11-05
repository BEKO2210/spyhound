package de.belkis.spyhound.ui

import de.belkis.spyhound.R
import de.belkis.spyhound.AppLanguage
import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.belkis.spyhound.bugreport.BUGREPORT_STALL_TIMEOUT_MS
import de.belkis.spyhound.bugreport.BugreportImport
import de.belkis.spyhound.bugreport.readWithStallTimeout
import de.belkis.spyhound.bugreport.analyzeBugreport
import de.belkis.spyhound.background.BackgroundReportStore
import de.belkis.spyhound.background.BackgroundScanPreferences
import de.belkis.spyhound.background.BackgroundScanScheduler
import de.belkis.spyhound.dns.DnsGuard
import de.belkis.spyhound.dns.DnsHit
import de.belkis.spyhound.dns.DnsFilterPrefs
import de.belkis.spyhound.dns.DnsFilterSettings
import de.belkis.spyhound.ioc.IocStore
import de.belkis.spyhound.ioc.LoadedIocs
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.CheckResult
import de.belkis.spyhound.collect.HardeningCollector
import de.belkis.spyhound.collect.StorageCollector
import de.belkis.spyhound.scan.HardeningItem
import de.belkis.spyhound.scan.Progress
import de.belkis.spyhound.scan.evaluateHardening
import java.time.LocalDate
import de.belkis.spyhound.scan.Report
import de.belkis.spyhound.scan.RadarMath
import de.belkis.spyhound.scan.RadarProgress
import de.belkis.spyhound.scan.ScanEngine
import de.belkis.spyhound.ui.DemoReport
import de.belkis.spyhound.shell.RootShell
import de.belkis.spyhound.shell.AdbSetupState
import de.belkis.spyhound.shell.AdbShell
import de.belkis.spyhound.shell.BuiltinAdb
import de.belkis.spyhound.shell.SHIZUKU_STOPPED_BANNER
import de.belkis.spyhound.shell.ScanGatedShellChanges
import de.belkis.spyhound.shell.Shell
import de.belkis.spyhound.shell.ShellPreferences
import de.belkis.spyhound.setup.isDeveloperOptionsEnabled
import de.belkis.spyhound.setup.isShizukuInstalled
import de.belkis.spyhound.shell.ShizukuShell
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

data class UiState(
    val iocs: LoadedIocs? = null,
    val access: AccessLevel = AccessLevel.APP,
    val shizuku: ShizukuShell.Status = ShizukuShell.Status.NOT_RUNNING,
    val shizukuInstalled: Boolean = false,
    val devOptionsEnabled: Boolean = false,
    val rootDenied: Boolean = false,
    /** A root request is waiting for the root manager; shown inside the card, not as a global busy line. */
    val rootAsking: Boolean = false,
    val smsGranted: Boolean = false,
    val filesGranted: Boolean = false,
    val updating: String? = null,
    val updateMessage: String? = null,
    val progress: Progress? = null,
    val report: Report? = null,
    val lastRadar: RadarProgress? = null,
    val busy: String? = null,
    val bannerMessage: String? = null,
    val dnsRunning: Boolean = false,
    val dnsHits: List<DnsHit> = emptyList(),
    val dnsFilter: DnsFilterSettings = DnsFilterSettings(),
    /** Null until the user starts "set up without a PC". */
    val builtinAdb: AdbSetupState? = null,
    val builtinAdbActive: Boolean = false,
    val adbWirelessOn: Boolean = false,
    val adbPairingPort: Int? = null,
    /** Empty until the first read finishes; the start card stays hidden meanwhile. */
    val hardening: List<HardeningItem> = emptyList(),
    val weeklyBackgroundEnabled: Boolean = false,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private fun s(id: Int, vararg args: Any): String = AppLanguage.localizedContext(getApplication()).getString(id, *args)
    private val store = IocStore(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state
    private var shell: Shell? = null
    private var pendingShell: Shell? = null
    private val shellGate = ScanGatedShellChanges()
    private var scanJob: Job? = null
    private var pendingBugreportChecks: List<CheckResult>? = null
    private var shellRestoreJob: Job? = null
    private var builtinAdbStarted = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { scheduleShellRestore() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { onShizukuBinderDead() }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        reloadIocs()
        refreshPermissions()
        refreshDnsGuard()
        refreshBackgroundWeeklySetting()
        scheduleShellRestore()
        viewModelScope.launch { DnsGuard.running.collect { refreshDnsGuard() } }
    }

    /** "Set up without a PC": pairs with this phone's own wireless debugging and adopts the shell when it is up. */
    fun startBuiltinAdb() {
        if (!builtinAdbStarted) {
            builtinAdbStarted = true
            viewModelScope.launch {
                BuiltinAdb.state.collect { st ->
                    val ctx = getApplication<Application>()
                    _state.update {
                        it.copy(
                            builtinAdb = st,
                            adbWirelessOn = BuiltinAdb.wirelessDebuggingOn(ctx),
                            adbPairingPort = BuiltinAdb.pairingPort,
                        )
                    }
                }
            }
        }
        BuiltinAdb.start(getApplication()) { adb ->
            viewModelScope.launch {
                // Root and Shizuku keep priority; the built-in connection only fills the gap.
                if (shell == null || shell?.isBuiltinAdb == true) adoptShell(adb) else adb.close()
            }
        }
    }

    fun stopBuiltinAdb() {
        if (builtinAdbStarted) BuiltinAdb.stop(getApplication())
    }

    private fun reloadIocs() = viewModelScope.launch {
        val loaded = store.load()
        _state.update { it.copy(iocs = loaded) }
    }

    /** Called on cold start and from [MainActivity.onResume]. */
    fun onAppVisible() {
        refreshPermissions()
        refreshBackgroundWeeklySetting()
        scheduleShellRestore()
    }

    fun setWeeklyBackgroundEnabled(enabled: Boolean) {
        val ctx = getApplication<Application>()
        BackgroundScanPreferences.setWeeklyEnabled(ctx, enabled)
        BackgroundScanScheduler.setEnabled(ctx, enabled)
        _state.update { it.copy(weeklyBackgroundEnabled = enabled) }
    }

    fun openBackgroundReportIfAvailable() {
        val report = BackgroundReportStore.consume(getApplication()) ?: return
        _state.update {
            it.copy(
                report = report,
                progress = null,
                lastRadar = RadarMath.fromReport(report),
            )
        }
    }

    fun refreshPermissions() {
        val ctx = getApplication<Application>()
        _state.update {
            it.copy(
                smsGranted = ctx.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED,
                filesGranted = StorageCollector.hasAccess(ctx),
                shizuku = ShizukuShell.status(),
                shizukuInstalled = isShizukuInstalled(ctx.packageManager),
                devOptionsEnabled = isDeveloperOptionsEnabled(ctx),
                access = shell?.level ?: AccessLevel.APP,
                builtinAdbActive = shell?.isBuiltinAdb == true,
            )
        }
        refreshDnsGuard()
        refreshHardening()
    }

    private fun refreshHardening() {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) {
                evaluateHardening(HardeningCollector(ctx).collect(), LocalDate.now())
            }
            _state.update { it.copy(hardening = items) }
        }
    }

    private fun refreshBackgroundWeeklySetting() {
        val ctx = getApplication<Application>()
        val enabled = BackgroundScanPreferences.isWeeklyEnabled(ctx)
        if (enabled) BackgroundScanScheduler.setEnabled(ctx, true)
        _state.update { it.copy(weeklyBackgroundEnabled = enabled) }
    }

    fun refreshDnsGuard() {
        val ctx = getApplication<Application>()
        _state.update {
            it.copy(
                dnsRunning = DnsGuard.isRunning,
                dnsHits = DnsGuard.recentHits(ctx),
                dnsFilter = DnsFilterPrefs.load(ctx),
            )
        }
    }

    /** Takes effect on the next DNS query of a running watcher; no restart needed. */
    fun setDnsFilter(settings: DnsFilterSettings) {
        DnsFilterPrefs.save(getApplication(), settings)
        _state.update { it.copy(dnsFilter = settings) }
    }

    fun updateIndicators() = viewModelScope.launch {
        _state.update { it.copy(updating = s(R.string.vm_connecting), updateMessage = null) }
        val (ok, failed) = store.update { name -> _state.update { s -> s.copy(updating = name) } }
        val loaded = store.load()
        _state.update {
            it.copy(
                iocs = loaded, updating = null,
                // Lead with the total so the message matches the "x of y sources" line above it.
                updateMessage = if (failed.isEmpty()) s(R.string.vm_sources_loaded, loaded.status.available, ok) else s(R.string.vm_sources_failed, ok, failed.size, failed.first()),
            )
        }
    }

    fun connectShizuku() {
        when (ShizukuShell.status()) {
            ShizukuShell.Status.NEEDS_PERMISSION -> ShizukuShell.requestPermission { granted ->
                if (granted) bindShizuku(showBusy = true) else refreshPermissions()
            }
            ShizukuShell.Status.READY -> bindShizuku(showBusy = true)
            else -> refreshPermissions()
        }
    }

    private fun bindShizuku(showBusy: Boolean) = viewModelScope.launch {
        if (showBusy) _state.update { it.copy(busy = s(R.string.vm_connecting_shizuku)) }
        val sh = ShizukuShell.connect()
        if (sh != null) adoptShell(sh) else if (showBusy) _state.update { it.copy(busy = null) }
        if (showBusy) _state.update { it.copy(busy = null) }
        refreshPermissions()
    }

    fun requestRoot() = viewModelScope.launch {
        _state.update { it.copy(rootAsking = true, rootDenied = false) }
        val sh = withContext(Dispatchers.IO) { RootShell.probe() }
        if (sh != null) {
            ShellPreferences.setRootGranted(getApplication(), true)
            adoptShell(sh)
        }
        _state.update { it.copy(rootAsking = false, rootDenied = sh == null) }
        refreshPermissions()
    }

    private fun scheduleShellRestore() {
        shellRestoreJob?.cancel()
        shellRestoreJob = viewModelScope.launch {
            restoreShellsQuietly()
        }
    }

    private suspend fun restoreShellsQuietly() {
        val ctx = getApplication<Application>()
        if (ShellPreferences.wasRootGranted(ctx)) {
            val root = withContext(Dispatchers.IO) { RootShell.probe() }
            if (root != null) {
                adoptShell(root)
                refreshPermissions()
                return
            }
        }
        if (ShizukuShell.status() == ShizukuShell.Status.READY) {
            val sh = ShizukuShell.connect()
            if (sh != null) adoptShell(sh)
            refreshPermissions()
        }
        // Last resort before the basic check: the built-in ADB connection, if it was paired before and the
        // setup flow is not running right now. After a reboot wireless debugging is off and this finds nothing.
        val builtinBusy = builtinAdbStarted && BuiltinAdb.state.value.let {
            it == AdbSetupState.WaitingForPairingCode || it == AdbSetupState.Pairing || it == AdbSetupState.Paired
        }
        if (shell == null && pendingShell == null && !builtinBusy &&
            ShellPreferences.wasBuiltinAdbPaired(ctx) && BuiltinAdb.wirelessDebuggingOn(ctx)
        ) {
            AdbShell.connect(ctx)?.let { adoptShell(it) }
            refreshPermissions()
        }
    }

    private fun onShizukuBinderDead() {
        if (!shellGate.deferClearShell(SHIZUKU_STOPPED_BANNER)) {
            pendingShell = null
            return
        }
        shell?.close()
        shell = null
        _state.update { it.copy(bannerMessage = SHIZUKU_STOPPED_BANNER) }
        refreshPermissions()
    }

    private fun adoptShell(newShell: Shell) {
        if (!shellGate.deferAdoptNewShell()) {
            pendingShell?.close()
            pendingShell = newShell
            return
        }
        shell?.close()
        shell = newShell
        refreshPermissions()
    }

    private fun applyDeferredShellWork() {
        val work = shellGate.scanFinished() ?: return
        if (work.clearShell) {
            shell?.close()
            shell = null
            pendingShell = null
        } else if (work.applyPendingShell) {
            pendingShell?.let { next ->
                shell?.close()
                shell = next
                pendingShell = null
            }
        }
        work.bannerMessage?.let { msg -> _state.update { it.copy(bannerMessage = msg) } }
        refreshPermissions()
    }

    fun startScan() {
        val iocs = _state.value.iocs ?: return
        val previous = scanJob
        scanJob = viewModelScope.launch {
            // A cancelled scan may still be closing its shell command; let it finish its cleanup first.
            previous?.cancelAndJoin()
            val job = coroutineContext.job
            shellGate.scanStarted()
            _state.update { it.copy(progress = Progress(s(R.string.vm_start), "", 0f), report = null) }
            var lastRadar: RadarProgress? = null
            val merged = try {
                val report = try {
                    ScanEngine(
                        getApplication(), iocs, shell, ShellPreferences.wasBuiltinAdbPaired(getApplication()),
                        builtinAdbKeyAvailable = AdbShell.keystoreUsable(),
                    ).run { p ->
                        // A cancelled scan must not bring its progress screen back.
                        if (job.isActive) {
                            lastRadar = p.radar ?: lastRadar
                            _state.update { s -> s.copy(progress = p) }
                        }
                    }
                } catch (e: CancellationException) {
                    // Cancelled: leave the state to cancelScan() and to a scan started right after.
                    throw e
                } catch (_: Exception) {
                    null
                }
                report?.let { mergePendingBugreport(it) }
            } finally {
                applyDeferredShellWork()
            }
            if (merged != null) {
                withContext(Dispatchers.IO) { BackgroundReportStore.record(getApplication(), merged) }
            }
            _state.update {
                it.copy(progress = null, report = merged, lastRadar = lastRadar ?: it.lastRadar)
            }
        }
    }

    fun clearBanner() = _state.update { it.copy(bannerMessage = null) }

    fun handleIncomingBugreportIntent(uri: Uri, mimeHint: String?) {
        val ctx = getApplication<Application>()
        if (!BugreportImport.acceptsUri(ctx, uri, mimeHint)) {
            _state.update { it.copy(bannerMessage = s(R.string.vm_shared_invalid)) }
            return
        }
        viewModelScope.launch {
            importBugreportInternal(uri)
        }
    }

    fun importBugreport(uri: Uri) = viewModelScope.launch {
        importBugreportInternal(uri)
    }

    private suspend fun importBugreportInternal(uri: Uri) {
        val ctx = getApplication<Application>()
        val loaded = _state.value.iocs
        if (loaded == null) {
            _state.update { it.copy(bannerMessage = s(R.string.vm_indicators_loading)) }
            return
        }
        if (!BugreportImport.acceptsUri(ctx, uri)) {
            _state.update {
                it.copy(
                    bannerMessage = s(R.string.vm_zip_invalid),
                )
            }
            return
        }
        _state.update { it.copy(busy = s(R.string.vm_checking_bugreport), bannerMessage = null) }
        val packages = withContext(Dispatchers.IO) { installedPackageNames() }
        val checks = readWithStallTimeout(
            BUGREPORT_STALL_TIMEOUT_MS,
            { ctx.contentResolver.openInputStream(uri) },
        ) { stream -> analyzeBugreport(stream, loaded.set, packages) }
        _state.update { it.copy(busy = null) }
        if (checks.isNullOrEmpty()) {
            _state.update { it.copy(bannerMessage = s(R.string.vm_bugreport_unreadable)) }
            return
        }
        if (_state.value.progress != null) {
            pendingBugreportChecks = (pendingBugreportChecks.orEmpty() + checks)
            _state.update { it.copy(bannerMessage = s(R.string.vm_bugreport_queued)) }
        } else {
            val report = bugreportReport(checks, loaded)
            withContext(Dispatchers.IO) { BackgroundReportStore.record(ctx, report) }
            _state.update { it.copy(report = report) }
        }
    }

    private fun mergePendingBugreport(report: Report): Report {
        val extra = pendingBugreportChecks ?: return report
        pendingBugreportChecks = null
        return report.copy(checks = report.checks + extra)
    }

    private fun bugreportReport(checks: List<CheckResult>, iocs: LoadedIocs): Report {
        val now = System.currentTimeMillis()
        return Report(
            startedAt = now,
            finishedAt = now,
            access = _state.value.access,
            indicatorCount = iocs.set.size,
            indicatorSources = iocs.status.available,
            indicatorSourcesMissing = iocs.status.missing.size + iocs.status.corrupt.size,
            checks = checks,
            device = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} " +
                "(Patch ${Build.VERSION.SECURITY_PATCH})",
            title = s(R.string.vm_bugreport_title),
        )
    }

    private fun installedPackageNames(): Set<String> {
        val pm = getApplication<Application>().packageManager
        val flags = PackageManager.MATCH_UNINSTALLED_PACKAGES
        return pm.getInstalledApplications(flags).map { it.packageName }.toSet()
    }

    fun cancelScan() {
        scanJob?.cancel()
        _state.update { it.copy(progress = null) }
    }

    fun closeReport() = _state.update { it.copy(report = null) }

    fun showDemoReport() {
        val report = DemoReport.withFinding(getApplication())
        _state.update {
            it.copy(report = report, progress = null, lastRadar = RadarMath.fromReport(report))
        }
    }

    override fun onCleared() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        shell?.close()
        pendingShell?.close()
    }
}
