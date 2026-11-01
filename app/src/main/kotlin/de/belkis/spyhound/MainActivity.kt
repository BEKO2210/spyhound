package de.belkis.spyhound

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import android.graphics.Color as AndroidColor
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
import de.belkis.spyhound.scan.Progress
import de.belkis.spyhound.scan.RadarLegendCounts
import de.belkis.spyhound.scan.RadarProgress
import de.belkis.spyhound.scan.RadarSector
import de.belkis.spyhound.scan.ReportText
import de.belkis.spyhound.scan.TextsDe
import de.belkis.spyhound.scan.TextsEn
import de.belkis.spyhound.scan.localized
import androidx.compose.ui.platform.LocalContext
import de.belkis.spyhound.ui.IntroPreferences
import de.belkis.spyhound.ui.MainViewModel
import de.belkis.spyhound.ui.SpyhoundScaffold
import de.belkis.spyhound.ui.SpyhoundTheme
import de.belkis.spyhound.ui.UiState
import de.belkis.spyhound.ui.screens.ResultScreen
import de.belkis.spyhound.ui.screens.ScanScreen
import de.belkis.spyhound.ui.screens.InfoScreen
import de.belkis.spyhound.ui.screens.IntroScreen
import de.belkis.spyhound.ui.screens.StartScreen

class MainActivity : AppCompatActivity() {
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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        if (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_INTRO_SCREENSHOT, false)) {
            val page = intent.getIntExtra(EXTRA_INTRO_PAGE, 0)
            setContent {
                SpyhoundTheme {
                    SpyhoundScaffold {
                        IntroScreen(
                            initialPage = page,
                            onFinished = { finish() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            return
        }
        if (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_INFO_SCREENSHOT, false)) {
            setContent {
                SpyhoundTheme {
                    SpyhoundScaffold {
                        InfoScreen(
                            onBack = { finish() },
                            onReplayIntro = { finish() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            return
        }
        if (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_SCAN_SCREENSHOT, false)) {
            setContent {
                SpyhoundTheme {
                    SpyhoundScaffold {
                        ScanScreen(
                            progress = Progress(
                                step = getString(R.string.scan_step_device),
                                detail = getString(R.string.scan_demo_detail),
                                fraction = 0.48f,
                                radar = RadarProgress(
                                    activeSector = RadarSector.DEVICE,
                                    deepLocked = true,
                                    legend = RadarLegendCounts(clean = 128, lead = 1),
                                ),
                            ),
                            onCancel = { finish() },
                        )
                    }
                }
            }
            return
        }
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
                val context = LocalContext.current
                var showIntro by remember {
                    mutableStateOf(
                        !IntroPreferences.hasCompleted(context) ||
                            (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_INTRO_FORCE, false)),
                    )
                }
                var showInfo by remember { mutableStateOf(false) }
                val finishIntro = {
                    IntroPreferences.markCompleted(context)
                    showIntro = false
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
                                onOpenInfo = { showInfo = true },
                            )
                        }
                    }
                    if (showIntro && !showDeepCheckSetup) {
                        BackHandler { finishIntro() }
                        SpyhoundScaffold {
                            IntroScreen(
                                onFinished = finishIntro,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    if (showInfo && !showDeepCheckSetup) {
                        BackHandler { showInfo = false }
                        SpyhoundScaffold {
                            InfoScreen(
                                onBack = { showInfo = false },
                                onReplayIntro = {
                                    showInfo = false
                                    showIntro = true
                                },
                                modifier = Modifier.fillMaxSize(),
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
        if (!intent.getBooleanExtra(EXTRA_SCAN_SCREENSHOT, false)) vm.onAppVisible()
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
        onOpenInfo: () -> Unit,
    ) {
        when {
            state.report != null -> {
                BackHandler { vm.closeReport() }
                ResultScreen(
                report = state.report.localized(currentTexts()),
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
            }
            state.progress != null -> ScanScreen(
                progress = state.progress,
                onCancel = { vm.cancelScan() },
            )
            else -> StartScreen(
                state = state.copy(
                    busy = state.busy?.let(currentTexts()::text),
                    bannerMessage = state.bannerMessage?.let(currentTexts()::text),
                    updateMessage = state.updateMessage?.let(currentTexts()::text),
                ),
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
                onOpenInfo = onOpenInfo,
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
        val text = ReportText.render(report, BuildConfig.VERSION_NAME, currentTexts())
        startActivity(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_subject))
                putExtra(Intent.EXTRA_TEXT, text)
            },
        )
    }

    private fun currentTexts() = if (resources.configuration.locales[0].language == "de") TextsDe else TextsEn

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
        const val EXTRA_SCAN_SCREENSHOT = "scan_screenshot"
        const val EXTRA_INTRO_SCREENSHOT = "intro_screenshot"
        const val EXTRA_INTRO_PAGE = "intro_page"
        const val EXTRA_INFO_SCREENSHOT = "info_screenshot"
        const val EXTRA_INTRO_FORCE = "intro_force"
        const val EXTRA_OPEN_DEEP_CHECK = "open_deep_check"
        const val EXTRA_DEEP_CHECK_SCREENSHOT = "deep_check_screenshot"
    }
}
