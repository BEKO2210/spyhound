package de.belkis.spyhound

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import de.belkis.spyhound.ui.LocalMotionEnabled
import de.belkis.spyhound.ui.MainViewModel
import de.belkis.spyhound.ui.SpyhoundScaffold
import de.belkis.spyhound.ui.design.SpySpacing
import de.belkis.spyhound.ui.SpyhoundTheme
import de.belkis.spyhound.ui.UiState
import de.belkis.spyhound.ui.design.SpyMotion
import de.belkis.spyhound.ui.navigation.RootScreen
import de.belkis.spyhound.ui.navigation.TransitionDirection
import de.belkis.spyhound.ui.navigation.TransitionStyle
import de.belkis.spyhound.ui.navigation.overlayTransitionPlan
import de.belkis.spyhound.ui.navigation.rootTransitionPlan
import de.belkis.spyhound.ui.screens.ResultScreen
import de.belkis.spyhound.ui.screens.ScanScreen
import de.belkis.spyhound.ui.screens.InfoScreen
import de.belkis.spyhound.ui.screens.PermissionAuditRoute
import de.belkis.spyhound.ui.screens.IntroScreen
import de.belkis.spyhound.ui.screens.StartScreen
import de.belkis.spyhound.ui.components.SmsBlockedDialog
import de.belkis.spyhound.scan.SmsRequestOutcome
import de.belkis.spyhound.scan.smsRequestOutcome
import androidx.compose.ui.unit.dp

class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()

    private var showSmsBlocked by mutableStateOf(false)

    private val smsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.refreshPermissions()
        showSmsBlocked = smsRequestOutcome(
            granted,
            shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS),
        ) == SmsRequestOutcome.BLOCKED_BY_ANDROID
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
                    SpyhoundScaffold(maxContentWidth = SpySpacing.TwoColumnMaxWidth) {
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
                var showAudit by remember { mutableStateOf(false) }
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
                    SpyhoundScaffold(maxContentWidth = SpySpacing.TwoColumnMaxWidth) {
                        Box(Modifier.fillMaxSize()) {
                            SpyhoundNav(
                                state = state,
                                onOpenDeepCheckSetup = { showDeepCheckSetup = true },
                                onOpenInfo = { showInfo = true },
                                onOpenPermissionAudit = { showAudit = true },
                            )
                        }
                    }
                    if (showSmsBlocked) SmsBlockedDialog(
                        onOpenAppInfo = { openAppInfo() },
                        onDismiss = { showSmsBlocked = false },
                    )
                    SpyhoundOverlay(visible = showIntro && !showDeepCheckSetup, onBack = finishIntro) {
                        SpyhoundScaffold {
                            IntroScreen(
                                onFinished = finishIntro,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    SpyhoundOverlay(visible = showInfo && !showDeepCheckSetup, onBack = { showInfo = false }) {
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
                    SpyhoundOverlay(visible = showAudit && !showDeepCheckSetup, onBack = { showAudit = false }) {
                        SpyhoundScaffold {
                            PermissionAuditRoute(onBack = { showAudit = false }, modifier = Modifier.fillMaxSize())
                        }
                    }
                    SpyhoundOverlay(visible = showDeepCheckSetup, onBack = null) {
                        SpyhoundScaffold {
                            DeepCheckSetupWizard(
                                snapshot = setupSnapshot,
                                onDismiss = { showDeepCheckSetup = false },
                                onAllowSpyhound = { vm.connectShizuku() },
                                builtinAdb = state.builtinAdb,
                                adbWirelessOn = state.adbWirelessOn,
                                adbPairingPort = state.adbPairingPort,
                                onSetupWithoutPc = { startBuiltinSetup() },
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
        dispatchBackgroundIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatchBugreportIntent(intent)
        dispatchBackgroundIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!intent.getBooleanExtra(EXTRA_SCAN_SCREENSHOT, false)) vm.onAppVisible()
        if (checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) showSmsBlocked = false
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

    private fun dispatchBackgroundIntent(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_BACKGROUND_RESULT) vm.openBackgroundReportIfAvailable()
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    private fun SpyhoundNav(
        state: UiState,
        onOpenDeepCheckSetup: () -> Unit,
        onOpenInfo: () -> Unit,
        onOpenPermissionAudit: () -> Unit,
    ) {
        val motionEnabled = LocalMotionEnabled.current
        val shift = with(LocalDensity.current) { 16.dp.roundToPx() }
        val target = rootScreen(state)
        SharedTransitionLayout {
            AnimatedContent(
                targetState = target,
                transitionSpec = {
                    val plan = rootTransitionPlan(initialState, targetState, motionEnabled)
                    if (plan.style == TransitionStyle.INSTANT) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        val forward = plan.direction == TransitionDirection.FORWARD
                        val enterOffset = if (forward) shift else -shift
                        val exitOffset = -enterOffset
                        (fadeIn(tween(SpyMotion.Screen, easing = SpyMotion.Enter)) +
                            slideInVertically(
                                animationSpec = tween(SpyMotion.Screen, easing = SpyMotion.Enter),
                                initialOffsetY = { enterOffset },
                            )) togetherWith
                            (fadeOut(tween(SpyMotion.Screen, easing = SpyMotion.Exit)) +
                                slideOutVertically(
                                    animationSpec = tween(SpyMotion.Screen, easing = SpyMotion.Exit),
                                    targetOffsetY = { exitOffset },
                                ))
                    }.using(SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> tween(0) }))
                },
                label = "spyhound-nav",
            ) { screen ->
                val sharedRadar = sharedRadarModifier(this, motionEnabled)
                when (screen) {
                    RootScreen.RESULT -> {
                        val report = state.report ?: return@AnimatedContent
                        BackHandler { vm.closeReport() }
                        ResultScreen(
                            report = report.localized(currentTexts()),
                            onShare = { shareReport(report) },
                            onRescan = {
                                vm.closeReport()
                                vm.startScan()
                            },
                            onGapFix = { fix -> handleGapFix(fix, onOpenDeepCheckSetup) },
                            onBugreport = { openBugreportPicker() },
                            onHelp = {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.accessnow.org/help/")))
                            },
                            heroSymbolModifier = sharedRadar,
                        )
                    }
                    RootScreen.SCAN -> {
                        val progress = state.progress ?: return@AnimatedContent
                        ScanScreen(
                            progress = progress,
                            onCancel = { vm.cancelScan() },
                            radarModifier = sharedRadar,
                        )
                    }
                    RootScreen.START -> StartScreen(
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
                        onWeeklyBackgroundToggle = { enabled -> vm.setWeeklyBackgroundEnabled(enabled) },
                        onPickBugreport = { openBugreportPicker() },
                        onDismissBanner = { vm.clearBanner() },
                        onDemoReport = {
                            if (BuildConfig.DEBUG) vm.showDemoReport()
                        },
                        onDeepCheckSetup = onOpenDeepCheckSetup,
                        onOpenInfo = onOpenInfo,
                        onOpenPermissionAudit = onOpenPermissionAudit,
                        radarModifier = sharedRadar,
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    private fun SharedTransitionScope.sharedRadarModifier(
        animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
        motionEnabled: Boolean,
    ): Modifier {
        if (!motionEnabled) return Modifier
        return Modifier.sharedElement(
            rememberSharedContentState(key = "main-radar-shared"),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }

    private fun rootScreen(state: UiState): RootScreen = when {
        state.report != null -> RootScreen.RESULT
        state.progress != null -> RootScreen.SCAN
        else -> RootScreen.START
    }

    @Composable
    private fun SpyhoundOverlay(
        visible: Boolean,
        onBack: (() -> Unit)?,
        content: @Composable () -> Unit,
    ) {
        val motionEnabled = LocalMotionEnabled.current
        val shift = with(LocalDensity.current) { 16.dp.roundToPx() }
        val enterPlan = overlayTransitionPlan(opening = true, motionEnabled = motionEnabled)
        val exitPlan = overlayTransitionPlan(opening = false, motionEnabled = motionEnabled)
        AnimatedVisibility(
            visible = visible,
            enter = visibilityEnterTransition(enterPlan.direction, enterPlan.style, shift),
            exit = visibilityExitTransition(exitPlan.direction, exitPlan.style, shift),
            label = "overlay-screen",
        ) {
            if (onBack != null) BackHandler { onBack() }
            Box(Modifier.fillMaxSize()) { content() }
        }
    }

    private fun visibilityEnterTransition(
        direction: TransitionDirection,
        style: TransitionStyle,
        shiftPx: Int,
    ): EnterTransition {
        if (style == TransitionStyle.INSTANT) return EnterTransition.None
        val offset = if (direction == TransitionDirection.FORWARD) shiftPx else -shiftPx
        return fadeIn(tween(SpyMotion.Screen, easing = SpyMotion.Enter)) +
            slideInVertically(
                animationSpec = tween(SpyMotion.Screen, easing = SpyMotion.Enter),
                initialOffsetY = { offset },
            )
    }

    private fun visibilityExitTransition(
        direction: TransitionDirection,
        style: TransitionStyle,
        shiftPx: Int,
    ): ExitTransition {
        if (style == TransitionStyle.INSTANT) return ExitTransition.None
        val offset = if (direction == TransitionDirection.FORWARD) -shiftPx else shiftPx
        return fadeOut(tween(SpyMotion.Screen, easing = SpyMotion.Exit)) +
            slideOutVertically(
                animationSpec = tween(SpyMotion.Screen, easing = SpyMotion.Exit),
                targetOffsetY = { offset },
            )
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

    private fun openAppInfo() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            },
        )
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
            GapFix.SHIZUKU, GapFix.RESTART_BUILTIN_ADB -> onDeepCheckSetup()
            GapFix.ROOT -> vm.requestRoot()
            GapFix.UPDATE_INDICATORS -> vm.updateIndicators()
            GapFix.NONE -> Unit
        }
    }

    /** The pairing code is typed into a notification, so that needs the notification permission. */
    private fun startBuiltinSetup() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        vm.startBuiltinAdb()
    }

    companion object {
        const val ACTION_OPEN_BACKGROUND_RESULT = "de.belkis.spyhound.action.OPEN_BACKGROUND_RESULT"
        const val EXTRA_SCAN_SCREENSHOT = "scan_screenshot"
        const val EXTRA_INTRO_SCREENSHOT = "intro_screenshot"
        const val EXTRA_INTRO_PAGE = "intro_page"
        const val EXTRA_INFO_SCREENSHOT = "info_screenshot"
        const val EXTRA_INTRO_FORCE = "intro_force"
        const val EXTRA_OPEN_DEEP_CHECK = "open_deep_check"
        const val EXTRA_DEEP_CHECK_SCREENSHOT = "deep_check_screenshot"
    }
}
