package de.belkis.spyhound.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.belkis.spyhound.bugreport.BugreportImport
import de.belkis.spyhound.bugreport.analyzeBugreport
import de.belkis.spyhound.dns.DnsGuard
import de.belkis.spyhound.dns.DnsHit
import de.belkis.spyhound.ioc.IocStore
import de.belkis.spyhound.ioc.LoadedIocs
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.CheckResult
import de.belkis.spyhound.scan.Progress
import de.belkis.spyhound.scan.Report
import de.belkis.spyhound.scan.ScanEngine
import de.belkis.spyhound.shell.RootShell
import de.belkis.spyhound.shell.Shell
import de.belkis.spyhound.shell.ShizukuShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val iocs: LoadedIocs? = null,
    val access: AccessLevel = AccessLevel.APP,
    val shizuku: ShizukuShell.Status = ShizukuShell.Status.NOT_RUNNING,
    val rootDenied: Boolean = false,
    val smsGranted: Boolean = false,
    val filesGranted: Boolean = false,
    val updating: String? = null,
    val updateMessage: String? = null,
    val progress: Progress? = null,
    val report: Report? = null,
    val busy: String? = null,
    val bannerMessage: String? = null,
    val dnsRunning: Boolean = false,
    val dnsHits: List<DnsHit> = emptyList(),
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = IocStore(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state
    private var shell: Shell? = null
    private var scanJob: Job? = null
    private var pendingBugreportChecks: List<CheckResult>? = null

    init {
        reloadIocs()
        refreshPermissions()
        refreshDnsGuard()
    }

    private fun reloadIocs() = viewModelScope.launch {
        val loaded = store.load()
        _state.update { it.copy(iocs = loaded) }
    }

    fun refreshPermissions() {
        val ctx = getApplication<Application>()
        _state.update {
            it.copy(
                smsGranted = ctx.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED,
                filesGranted = Environment.isExternalStorageManager(),
                shizuku = ShizukuShell.status(),
                access = shell?.level ?: AccessLevel.APP,
            )
        }
        refreshDnsGuard()
    }

    fun refreshDnsGuard() {
        val ctx = getApplication<Application>()
        _state.update {
            it.copy(
                dnsRunning = DnsGuard.isRunning,
                dnsHits = DnsGuard.recentHits(ctx),
            )
        }
    }

    fun updateIndicators() = viewModelScope.launch {
        _state.update { it.copy(updating = "Verbinde …", updateMessage = null) }
        val (ok, failed) = store.update { name -> _state.update { s -> s.copy(updating = name) } }
        val loaded = store.load()
        _state.update {
            it.copy(
                iocs = loaded, updating = null,
                updateMessage = if (failed.isEmpty()) "$ok Quellen geladen und geprüft." else "$ok geladen, ${failed.size} fehlgeschlagen: ${failed.first()}",
            )
        }
    }

    fun connectShizuku() {
        when (ShizukuShell.status()) {
            ShizukuShell.Status.NEEDS_PERMISSION -> ShizukuShell.requestPermission { granted ->
                if (granted) bindShizuku() else refreshPermissions()
            }
            ShizukuShell.Status.READY -> bindShizuku()
            else -> refreshPermissions()
        }
    }

    private fun bindShizuku() = viewModelScope.launch {
        _state.update { it.copy(busy = "Verbinde mit Shizuku …") }
        val sh = ShizukuShell.connect()
        if (sh != null) { shell?.close(); shell = sh }
        _state.update { it.copy(busy = null) }
        refreshPermissions()
    }

    fun requestRoot() = viewModelScope.launch {
        _state.update { it.copy(busy = "Frage Root-Zugriff an … (Root-Manager bestätigen)") }
        val sh = withContext(Dispatchers.IO) { RootShell.probe() }
        if (sh != null) { shell?.close(); shell = sh }
        _state.update { it.copy(busy = null, rootDenied = sh == null) }
        refreshPermissions()
    }

    fun startScan() {
        val iocs = _state.value.iocs ?: return
        scanJob = viewModelScope.launch {
            _state.update { it.copy(progress = Progress("Start", "", 0f), report = null) }
            val report = runCatching {
                ScanEngine(getApplication(), iocs, shell).run { p -> _state.update { s -> s.copy(progress = p) } }
            }.getOrNull()
            val merged = report?.let { mergePendingBugreport(it) }
            _state.update { it.copy(progress = null, report = merged) }
        }
    }

    fun clearBanner() = _state.update { it.copy(bannerMessage = null) }

    fun handleIncomingBugreportIntent(uri: Uri, mimeHint: String?) {
        val ctx = getApplication<Application>()
        if (!BugreportImport.acceptsUri(ctx, uri, mimeHint)) {
            _state.update { it.copy(bannerMessage = "Geteilte Datei ist kein Fehlerbericht (bugreport….zip).") }
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
            _state.update { it.copy(bannerMessage = "Indikatoren werden noch geladen — bitte kurz warten.") }
            return
        }
        if (!BugreportImport.acceptsUri(ctx, uri)) {
            _state.update {
                it.copy(
                    bannerMessage = "Keine gültige Fehlerbericht-ZIP (bugreport….zip).",
                )
            }
            return
        }
        _state.update { it.copy(busy = "Fehlerbericht wird ausgewertet …", bannerMessage = null) }
        val checks = withContext(Dispatchers.IO) {
            runCatching {
                ctx.contentResolver.openInputStream(uri)?.use { stream ->
                    analyzeBugreport(stream, loaded.set, installedPackageNames())
                }
            }.getOrNull()
        }
        _state.update { it.copy(busy = null) }
        if (checks.isNullOrEmpty()) {
            _state.update { it.copy(bannerMessage = "Fehlerbericht konnte nicht gelesen werden.") }
            return
        }
        if (_state.value.progress != null) {
            pendingBugreportChecks = (pendingBugreportChecks.orEmpty() + checks)
            _state.update { it.copy(bannerMessage = "Fehlerbericht wird nach der laufenden Prüfung ergänzt.") }
        } else {
            _state.update { it.copy(report = bugreportReport(checks, loaded)) }
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
            title = "Fehlerbericht-Prüfung",
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
        _state.update { it.copy(report = DemoReport.withFinding(), progress = null) }
    }

    override fun onCleared() {
        shell?.close()
    }
}
