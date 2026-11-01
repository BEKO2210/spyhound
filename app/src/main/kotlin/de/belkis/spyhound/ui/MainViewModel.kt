package de.belkis.spyhound.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.belkis.spyhound.ioc.IocStore
import de.belkis.spyhound.ioc.LoadedIocs
import de.belkis.spyhound.scan.AccessLevel
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
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = IocStore(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state
    private var shell: Shell? = null
    private var scanJob: Job? = null

    init {
        reloadIocs()
        refreshPermissions()
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
            _state.update { it.copy(progress = null, report = report) }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _state.update { it.copy(progress = null) }
    }

    fun closeReport() = _state.update { it.copy(report = null) }

    override fun onCleared() {
        shell?.close()
    }
}
