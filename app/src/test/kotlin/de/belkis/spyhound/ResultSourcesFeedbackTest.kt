package de.belkis.spyhound

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.CheckResult
import de.belkis.spyhound.scan.GapFix
import de.belkis.spyhound.scan.Report
import de.belkis.spyhound.ui.SpyhoundScaffold
import de.belkis.spyhound.ui.SpyhoundTheme
import de.belkis.spyhound.ui.screens.ResultScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The card that offers "load sources" must answer the tap where it was made, not only on the start screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h914dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class ResultSourcesFeedbackTest {
    private val gapText = "1 Indikator-Quelle ist veraltet und wurde nicht benutzt (Example)."
    private val report = Report(
        startedAt = 1_793_891_990_000L,
        finishedAt = 1_793_892_000_000L,
        access = AccessLevel.APP,
        indicatorCount = 100,
        indicatorSources = 1,
        indicatorSourcesMissing = 1,
        checks = listOf(
            CheckResult("Apps").apply { ran = true; examined = 10 },
            CheckResult("Indikatoren").apply { gap(gapText, GapFix.UPDATE_INDICATORS) },
        ),
        device = "Example",
    )

    private fun ComposeUiTest.openCard(updating: String?, message: String?) {
        setContent {
            SpyhoundTheme { SpyhoundScaffold {
                ResultScreen(report, {}, {}, {}, {}, {}, sourcesUpdating = updating, sourcesMessage = message)
            } }
        }
        val card = hasContentDescription(gapText, substring = true)
        onNode(hasScrollToNodeAction()).performScrollToNode(card)
        onNode(card).performClick()
    }

    @Test
    fun outcomeIsShownInTheCard() = runComposeUiTest {
        val done = "All 46 sources current – 1 newly loaded and verified."
        openCard(updating = null, message = done)
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText(done))
        onNodeWithText(done).assertIsDisplayed()
    }

    @Test
    fun buttonShowsProgressAndCannotBeTappedTwice() = runComposeUiTest {
        openCard(updating = "Connecting …", message = null)
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Connecting …"))
        onNodeWithText("Connecting …").assertIsDisplayed()
        onNode(hasText("Connecting …") and androidx.compose.ui.test.hasClickAction()).assertIsNotEnabled()
    }
}
