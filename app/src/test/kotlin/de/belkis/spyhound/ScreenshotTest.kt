package de.belkis.spyhound

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.platform.LocalContext
import com.github.takahirom.roborazzi.captureRoboImage
import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocStatus
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.ioc.LoadedIocs
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.ElementStatus
import de.belkis.spyhound.scan.Progress
import de.belkis.spyhound.scan.RadarLegendCounts
import de.belkis.spyhound.scan.RadarProgress
import de.belkis.spyhound.scan.RadarSector
import de.belkis.spyhound.scan.SectorRadarData
import de.belkis.spyhound.setup.DeepCheckSetupSnapshot
import de.belkis.spyhound.shell.ShizukuShell
import de.belkis.spyhound.ui.DemoReport
import de.belkis.spyhound.ui.SpyhoundScaffold
import de.belkis.spyhound.ui.SpyhoundTheme
import de.belkis.spyhound.ui.design.SpySpacing
import de.belkis.spyhound.ui.UiState
import de.belkis.spyhound.ui.components.DeepCheckSetupWizard
import de.belkis.spyhound.ui.screens.InfoScreen
import de.belkis.spyhound.ui.screens.ResultScreen
import de.belkis.spyhound.ui.screens.ScanScreen
import de.belkis.spyhound.ui.screens.StartScreen
import de.belkis.spyhound.scan.HardeningInput
import de.belkis.spyhound.scan.evaluateHardening
import java.time.LocalDate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.ui.screens.HardeningCard
import de.belkis.spyhound.scan.AppRecord
import de.belkis.spyhound.scan.ApkFile
import de.belkis.spyhound.scan.AppAnalyzer
import de.belkis.spyhound.scan.Report
import de.belkis.spyhound.scan.TextsDe
import de.belkis.spyhound.scan.TextsEn
import de.belkis.spyhound.ioc.Parsers
import de.belkis.spyhound.ioc.SourceContext
import de.belkis.spyhound.scan.PermissionAudit
import de.belkis.spyhound.scan.AuditSnapshot
import de.belkis.spyhound.scan.RightGroup
import de.belkis.spyhound.ui.screens.AuditResult
import de.belkis.spyhound.ui.screens.PermissionAuditScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config

/**
 * Screenshots of the main screens in German and English on a phone and a tablet.
 * Record with `recordRoborazziDebug`, check with `verifyRoborazziDebug`. The clock is paused and
 * advanced by a fixed amount, so the endless radar and shimmer animations always show the same frame.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
@OptIn(ExperimentalTestApi::class)
class ScreenshotTest(private val language: String, private val device: Device) {

    enum class Device(val qualifiers: String) {
        PHONE("w411dp-h914dp-xxhdpi"),
        TABLET("w800dp-h1280dp-xhdpi"),
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun parameters(): List<Array<Any>> =
            listOf("de", "en").flatMap { l -> Device.entries.map { d -> arrayOf<Any>(l, d) } }
    }

    private fun shot(
        name: String,
        scrollToEnd: Boolean = false,
        scrollSwipes: Int = 6,
        twoColumn: Boolean = false,
        qualifiers: String = device.qualifiers,
        content: @Composable () -> Unit,
    ) {
        RuntimeEnvironment.setQualifiers("+$language-$qualifiers")
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent { SpyhoundTheme { SpyhoundScaffold(maxContentWidth = if (twoColumn) SpySpacing.TwoColumnMaxWidth else 640.dp) { content() } } }
            mainClock.advanceTimeBy(2_000)
            if (scrollToEnd) {
                repeat(scrollSwipes) { onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() } }
                mainClock.advanceTimeBy(2_000)
            }
            onRoot().captureRoboImage("src/test/screenshots/${name}_${language}_${device.name.lowercase()}.png")
        }
    }

    private fun iocs(): LoadedIocs {
        val set = IocSet(
            (0 until 4_200).map {
                Indicator("d$it.example", IocType.DOMAIN, Category.MERCENARY, "Demo", "demo")
            },
        )
        return LoadedIocs(set, IocStatus(sources = 29, available = 29, missing = emptyList(), corrupt = emptyList()))
    }

    @Test
    fun start() = shot("start", twoColumn = true) {
        StartScreen(
            state = UiState(iocs = iocs()),
            onScan = {}, onUpdateIndicators = {}, onRequestRoot = {}, onRequestSms = {}, onRequestFiles = {},
            onDnsToggle = {}, onWeeklyBackgroundToggle = {}, onPickBugreport = {}, onDismissBanner = {},
            onDeepCheckSetup = {}, onOpenInfo = {},
        )
    }

    /** A short phone, or a phone with large display zoom: the radar shrinks and the whole page scrolls. */
    @Test
    fun startCompact() {
        if (device != Device.PHONE) return
        shot("start_compact", twoColumn = true, qualifiers = "w360dp-h600dp-xhdpi") {
            StartScreen(
                state = UiState(iocs = iocs()),
                onScan = {}, onUpdateIndicators = {}, onRequestRoot = {}, onRequestSms = {}, onRequestFiles = {},
                onDnsToggle = {}, onWeeklyBackgroundToggle = {}, onPickBugreport = {}, onDismissBanner = {},
                onDeepCheckSetup = {}, onOpenInfo = {},
            )
        }
    }

    @Test
    fun startBottom() = shot("start_bottom", scrollToEnd = true, twoColumn = true) {
        StartScreen(
            state = UiState(iocs = iocs(), hardening = sampleHardening()),
            onScan = {}, onUpdateIndicators = {}, onRequestRoot = {}, onRequestSms = {}, onRequestFiles = {},
            onDnsToggle = {}, onWeeklyBackgroundToggle = {}, onPickBugreport = {}, onDismissBanner = {},
            onDeepCheckSetup = {}, onOpenInfo = {},
        )
    }

    @Test
    fun startLower() = shot("start_lower", scrollToEnd = true, twoColumn = true) {
        StartScreen(
            state = UiState(iocs = iocs()),
            onScan = {}, onUpdateIndicators = {}, onRequestRoot = {}, onRequestSms = {}, onRequestFiles = {},
            onDnsToggle = {}, onWeeklyBackgroundToggle = {}, onPickBugreport = {}, onDismissBanner = {},
            onDeepCheckSetup = {}, onOpenInfo = {},
        )
    }

    @Test
    fun hardening() = shot("hardening") {
        Box(Modifier.padding(20.dp)) { HardeningCard(sampleHardening()) }
    }

    private fun sampleHardening() = evaluateHardening(
        HardeningInput(
            screenLockSet = true, usbDebuggingOn = false, wifiDebuggingOn = true, developerOptionsOn = true,
            securityPatch = LocalDate.of(2026, 11, 2).minusDays(120), unknownInstallerApps = listOf("Files", "Store"),
        ),
        LocalDate.of(2026, 11, 2),
    )

    @Test
    fun scan() = shot("scan", twoColumn = true) {
        val sectors = RadarSector.entries.map { sector ->
            val blips = when (sector) {
                RadarSector.APPS -> List(14) { ElementStatus.CLEAN } + ElementStatus.LEAD
                RadarSector.DEVICE -> List(6) { ElementStatus.CLEAN }
                else -> emptyList()
            }
            SectorRadarData(sector, examined = blips.size, blips = blips)
        }
        val radar = RadarProgress(
            activeSector = RadarSector.DEVICE,
            sectors = sectors,
            legend = RadarLegendCounts(clean = 20, lead = 1, finding = 0),
        )
        ScanScreen(
            progress = Progress(
                step = LocalContext.current.getString(R.string.vm_start),
                detail = "com.example.app",
                fraction = 0.45f,
                radar = radar,
            ),
            onCancel = {},
        )
    }

    @Test
    fun result() = shot("result", twoColumn = true) {
        ResultScreen(
            report = DemoReport.withFinding(LocalContext.current),
            onShare = {}, onRescan = {}, onGapFix = {}, onBugreport = {}, onHelp = {},
        )
    }

    @Test
    fun resultMalware() = shot("result_malware", twoColumn = true) {
        val dir = java.io.File("src/main/assets/iocs")
        val src = de.belkis.spyhound.ioc.FeedCatalog.parse(java.io.File(dir, "sources.json").readText()).first { it.id == "eset-aridspy" }
        val set = IocSet(
            Parsers.parse(src.format, java.io.File(dir, src.fileName).readText(), SourceContext(src.id, src.category, src.family, true)),
        )
        val apps = AppAnalyzer.analyze(
            listOf(
                AppRecord(
                    "com.example.video", "Video Chat",
                    apks = listOf(ApkFile("/data/app/x/base.apk", "0cb41557841ff6f314c398250a165706e0b18f93674a7c12f4489018a1661673")),
                ),
            ),
            set, "de.belkis.spyhound", if (LocalContext.current.resources.configuration.locales[0].language == "de") TextsDe else TextsEn,
        )
        // Fixed time: the report shows dates relative to its end, so a moving clock would change the picture daily.
        val now = java.time.Instant.parse("2026-11-02T12:00:00Z").toEpochMilli()
        ResultScreen(
            report = Report(now - 30_000, now, AccessLevel.APP, set.size, 1, 0, listOf(apps), "Pixel"),
            onShare = {}, onRescan = {}, onGapFix = {}, onBugreport = {}, onHelp = {},
        )
    }

    @Test
    fun smsBlocked() = shot("sms_blocked") {
        de.belkis.spyhound.ui.components.SmsBlockedDialog(onOpenAppInfo = {}, onDismiss = {})
    }

    @Test
    fun wizard() = shot("wizard") {
        DeepCheckSetupWizard(
            snapshot = DeepCheckSetupSnapshot(
                shizukuInstalled = false,
                devOptionsEnabled = false,
                shizukuStatus = ShizukuShell.Status.NOT_RUNNING,
                access = AccessLevel.APP,
            ),
            onDismiss = {}, onAllowSpyhound = {}, onScanNow = {}, onSetupWithoutPc = {},
        )
    }

    @Test
    fun info() = shot("info") {
        InfoScreen(
            onBack = {},
            onReplayIntro = {},
            displayVersion = "0.0.0",
            testInitialScrollPx = if (device == Device.PHONE) 2_800 else 2_200,
        )
    }

    @Test
    fun infoExpanded() = shot("info_expanded") {
        InfoScreen(
            onBack = {},
            onReplayIntro = {},
            displayVersion = "0.0.0",
            sourcesSectionExpanded = true,
            testInitialScrollPx = if (device == Device.PHONE) 2_800 else 2_200,
        )
    }

    @Test
    fun infoTop() = shot("info_top") {
        InfoScreen(onBack = {}, onReplayIntro = {}, displayVersion = "0.0.0")
    }

    @Test
    fun infoLicences() = shot("info_licences") {
        InfoScreen(
            onBack = {},
            onReplayIntro = {},
            displayVersion = "0.0.0",
            licencesSectionExpanded = true,
            testInitialScrollPx = if (device == Device.PHONE) 3_400 else 2_800,
        )
    }

    private fun auditResult(): AuditResult {
        val cam = "android.permission.CAMERA"
        val mic = "android.permission.RECORD_AUDIO"
        val apps = listOf(
            AppRecord("com.example.chat", "Chat", grantedPermissions = setOf(cam, mic)),
            AppRecord("org.example.scanner.documents.pro", "Dokumentenscanner Pro Premium Edition", grantedPermissions = setOf(cam)),
            AppRecord("com.example.newcam", "Neue Kamera", grantedPermissions = setOf(cam, mic)),
            AppRecord("com.google.android.GoogleCamera", "Camera", isSystem = true, grantedPermissions = setOf(cam)),
            AppRecord("com.example.helper", "Helper", accessibility = true),
            AppRecord("com.example.sms.helper", "Nachrichten-Helfer", accessibility = true, grantedPermissions = setOf("android.permission.READ_SMS")),
            AppRecord("com.example.tracker", "Standort-Begleiter mit sehr langem Namen", deviceAdmin = true, grantedPermissions = setOf("android.permission.ACCESS_FINE_LOCATION")),
        )
        val before = AuditSnapshot(
            1_793_000_000_000L,
            mapOf(RightGroup.CAMERA to setOf("com.example.chat", "org.example.scanner.documents.pro", "com.google.android.GoogleCamera"),
                RightGroup.MICROPHONE to setOf("com.example.chat")),
        )
        return AuditResult(PermissionAudit.evaluate(apps, "me", before), before.takenAt)
    }

    @Test
    fun audit() = shot("audit") {
        PermissionAuditScreen(auditResult(), onBack = {}, onAppInfo = {})
    }

    @Test
    fun auditSystem() = shot("audit_system") {
        PermissionAuditScreen(auditResult(), onBack = {}, onAppInfo = {}, initialShowSystem = true)
    }
}
