package de.belkis.spyhound

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.belkis.spyhound.BuildConfig
import de.belkis.spyhound.setup.debugDeepCheckSnapshot
import de.belkis.spyhound.ui.components.DeepCheckSetupWizard
import de.belkis.spyhound.ui.deepCheckSnapshot
import de.belkis.spyhound.bugreport.BugreportImport
import de.belkis.spyhound.dns.DnsGuard
import de.belkis.spyhound.scan.GapFix
import de.belkis.spyhound.scan.ReportText
import de.belkis.spyhound.ui.MainViewModel
import de.belkis.spyhound.ui.SpyhoundScaffold
import de.belkis.spyhound.ui.SpyhoundTheme
import de.belkis.spyhound.ui.UiState
import de.belkis.spyhound.ui.screens.ResultScreen
import de.belkis.spyhound.ui.screens.ScanScreen
import de.belkis.spyhound.ui.screens.StartScreen

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val smsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refreshPermissions()
    }

    private val notificationsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refreshPermissions()
    }

    private val vpnPrepare = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) DnsGuard.start(this)
        vm.refreshDnsGuard()
    }

    private val pickBugreport = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importBugreport(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            SpyhoundTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                val screenshotKey = if (BuildConfig.DEBUG) {
                    intent.getStringExtra(EXTRA_DEEP_CHECK_SCREENSHOT)
                } else {
                    null
                }
                var showDeepCheckSetup by remember {
                    mutableStateOf(screenshotKey != null || intent.getBooleanExtra(EXTRA_OPEN_DEEP_CHECK, false))
                }
                val setupSnapshot = screenshotKey?.let { debugDeepCheckSnapshot(it) } ?: state.deepCheckSnapshot()
                LaunchedEffect(Unit) { vm.refreshDnsGuard() }
                Box(Modifier.fillMaxSize()) {
                    SpyhoundScaffold {
                        Box(Modifier.fillMaxSize()) {
                            SpyhoundNav(
                                state = state,
                                onOpenDeepCheckSetup = { showDeepCheckSetup = true },
                            )
                        }
                    }
                    if (showDeepCheckSetup) {
                        SpyhoundScaffold {
                            DeepCheckSetupWizard(
                                snapshot = setupSnapshot,
                                onDismiss = { showDeepCheckSetup = false },
                                onAllowSpyhound = { vm.connectShizuku() },
                                onScanNow = {
                                    showDeepCheckSetup = false
                                    vm.startScan()
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
        dispatchBugreportIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatchBugreportIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.onAppVisible()
    }

    private fun dispatchBugreportIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = BugreportImport.streamUri(intent) ?: return
                vm.handleIncomingBugreportIntent(uri, intent.type)
            }
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return
                vm.handleIncomingBugreportIntent(uri, intent.type)
            }
        }
    }

    @Composable
    private fun SpyhoundNav(
        state: UiState,
        onOpenDeepCheckSetup: () -> Unit,
    ) {
        when {
            state.report != null -> ResultScreen(
                report = state.report,
                onShare = { shareReport(state.report) },
                onRescan = {
                    vm.closeReport()
                    vm.startScan()
                },
                onGapFix = { fix -> handleGapFix(fix, onOpenDeepCheckSetup) },
                onBugreport = { openBugreportPicker() },
                onHelp = {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.accessnow.org/help/")))
                },
            )
            state.progress != null -> ScanScreen(
                progress = state.progress,
                onCancel = { vm.cancelScan() },
            )
            else -> StartScreen(
                state = state,
                onScan = { vm.startScan() },
                onUpdateIndicators = { vm.updateIndicators() },
                onRequestRoot = { vm.requestRoot() },
                onRequestSms = {
                    smsPermission.launch(Manifest.permission.READ_SMS)
                },
                onRequestFiles = { openAllFilesAccess() },
                onDnsToggle = { on -> toggleDns(on) },
                onPickBugreport = { openBugreportPicker() },
                onDismissBanner = { vm.clearBanner() },
                onDemoReport = {
                    if (BuildConfig.DEBUG) vm.showDemoReport()
                },
                onDeepCheckSetup = onOpenDeepCheckSetup,
            )
        }
    }

    private fun openBugreportPicker() {
        pickBugreport.launch(arrayOf("application/zip"))
    }

    private fun toggleDns(enable: Boolean) {
        if (enable) {
            val prep = android.net.VpnService.prepare(this)
            if (prep != null) vpnPrepare.launch(prep) else DnsGuard.start(this)
        } else {
            DnsGuard.stop(this)
        }
        vm.refreshDnsGuard()
    }

    private fun openAllFilesAccess() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun shareReport(report: de.belkis.spyhound.scan.Report) {
        val text = ReportText.render(report, BuildConfig.VERSION_NAME)
        startActivity(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Spyhound Prüfbericht")
                putExtra(Intent.EXTRA_TEXT, text)
            },
        )
    }

    private fun handleGapFix(fix: GapFix, onDeepCheckSetup: () -> Unit) {
        when (fix) {
            GapFix.PERMISSION_SMS -> smsPermission.launch(Manifest.permission.READ_SMS)
            GapFix.PERMISSION_FILES -> openAllFilesAccess()
            GapFix.SHIZUKU -> onDeepCheckSetup()
            GapFix.ROOT -> vm.requestRoot()
            GapFix.UPDATE_INDICATORS -> vm.updateIndicators()
            GapFix.NONE -> Unit
        }
    }

    companion object {
        const val EXTRA_OPEN_DEEP_CHECK = "open_deep_check"
        const val EXTRA_DEEP_CHECK_SCREENSHOT = "deep_check_screenshot"
    }
}
