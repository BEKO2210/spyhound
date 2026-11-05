package de.belkis.spyhound

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import de.belkis.spyhound.background.ScanHistoryEntry
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.CheckResult
import de.belkis.spyhound.scan.Report
import de.belkis.spyhound.ui.SpyhoundScaffold
import de.belkis.spyhound.ui.SpyhoundTheme
import de.belkis.spyhound.ui.screens.HistoryResultScreen
import de.belkis.spyhound.ui.screens.HistoryScreen
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h914dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class HistoryScreenTest {
    private val entry = ScanHistoryEntry(
        1_793_892_000_000L,
        Report(
            startedAt = 1_793_891_990_000L,
            finishedAt = 1_793_892_000_000L,
            access = AccessLevel.APP,
            indicatorCount = 100,
            indicatorSources = 1,
            indicatorSourcesMissing = 0,
            checks = listOf(CheckResult("Apps").apply { ran = true; examined = 10 }),
            device = "Example",
        ),
    )
    private val zone = ZoneId.of("Europe/Berlin")

    @Test
    fun singleDeleteNeedsConfirmationAndCanBeCancelled() = runComposeUiTest {
        var deleted = 0
        setContent {
            SpyhoundTheme { SpyhoundScaffold {
                HistoryScreen(listOf(entry), {}, {}, { deleted++ }, {}, zoneId = zone)
            } }
        }
        onNodeWithContentDescription("Delete check from 05 Nov 2026, 16:20").performClick()
        runOnIdle { assertEquals(0, deleted) }
        onNodeWithText("Cancel").performClick()
        runOnIdle { assertEquals(0, deleted) }
        onNodeWithContentDescription("Delete check from 05 Nov 2026, 16:20").performClick()
        onNodeWithText("Delete", useUnmergedTree = true).performClick()
        runOnIdle { assertEquals(1, deleted) }
    }

    @Test
    fun clearAllNeedsConfirmation() = runComposeUiTest {
        var cleared = 0
        setContent {
            SpyhoundTheme { SpyhoundScaffold {
                HistoryScreen(listOf(entry), {}, {}, {}, { cleared++ }, zoneId = zone)
            } }
        }
        onNodeWithText("Clear history").performClick()
        runOnIdle { assertEquals(0, cleared) }
        onNodeWithText("All saved check results on this device will be deleted.").assertIsDisplayed()
        onNode(hasText("Clear history") and hasAnyAncestor(isDialog())).performClick()
        runOnIdle { assertEquals(1, cleared) }
    }

    @Test
    fun historicalResultShowsDateAndAllowsBackAndRescan() = runComposeUiTest {
        var back = 0
        var rescanned = 0
        setContent {
            SpyhoundTheme { SpyhoundScaffold {
                HistoryResultScreen(entry, { back++ }, {}, { rescanned++ }, {}, {}, {}, zoneId = zone)
            } }
        }
        onNodeWithText("Check from 05 Nov 2026, 16:20").assertIsDisplayed()
        onNodeWithContentDescription("Back").performClick()
        runOnIdle { assertEquals(1, back) }
        // Use the resource so this test follows the existing result action wording.
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        onNodeWithText(context.getString(R.string.t_051)).performClick()
        runOnIdle { assertEquals(1, rescanned) }
    }
}
