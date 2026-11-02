package de.belkis.spyhound

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
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

    private fun shot(name: String, content: @Composable () -> Unit) {
        RuntimeEnvironment.setQualifiers("+$language-${device.qualifiers}")
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent { SpyhoundTheme { SpyhoundScaffold { content() } } }
            mainClock.advanceTimeBy(2_000)
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
    fun start() = shot("start") {
        StartScreen(
            state = UiState(iocs = iocs()),
            onScan = {}, onUpdateIndicators = {}, onRequestRoot = {}, onRequestSms = {}, onRequestFiles = {},
            onDnsToggle = {}, onPickBugreport = {}, onDismissBanner = {}, onDeepCheckSetup = {}, onOpenInfo = {},
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
    fun scan() = shot("scan") {
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
    fun result() = shot("result") {
        ResultScreen(
            report = DemoReport.withFinding(LocalContext.current),
            onShare = {}, onRescan = {}, onGapFix = {}, onBugreport = {}, onHelp = {},
        )
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
        InfoScreen(onBack = {}, onReplayIntro = {})
    }
}
