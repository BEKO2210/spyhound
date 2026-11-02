package de.belkis.spyhound

import de.belkis.spyhound.ui.navigation.RootScreen
import de.belkis.spyhound.ui.navigation.TransitionDirection
import de.belkis.spyhound.ui.navigation.TransitionStyle
import de.belkis.spyhound.ui.navigation.overlayTransitionPlan
import de.belkis.spyhound.ui.navigation.rootTransitionPlan
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTransitionsTest {
    @Test
    fun rootUsesSharedRadarOnMainFlow() {
        assertEquals(
            TransitionStyle.SHARED_RADAR,
            rootTransitionPlan(RootScreen.START, RootScreen.SCAN, motionEnabled = true).style,
        )
        assertEquals(
            TransitionStyle.SHARED_RADAR,
            rootTransitionPlan(RootScreen.SCAN, RootScreen.RESULT, motionEnabled = true).style,
        )
    }

    @Test
    fun rootMirrorsDirectionOnBackNavigation() {
        assertEquals(
            TransitionDirection.BACKWARD,
            rootTransitionPlan(RootScreen.RESULT, RootScreen.SCAN, motionEnabled = true).direction,
        )
        assertEquals(
            TransitionDirection.BACKWARD,
            rootTransitionPlan(RootScreen.SCAN, RootScreen.START, motionEnabled = true).direction,
        )
    }

    @Test
    fun allTransitionsBecomeInstantWhenMotionIsReduced() {
        val root = rootTransitionPlan(RootScreen.START, RootScreen.SCAN, motionEnabled = false)
        val overlay = overlayTransitionPlan(opening = true, motionEnabled = false)
        assertEquals(TransitionStyle.INSTANT, root.style)
        assertEquals(TransitionStyle.INSTANT, overlay.style)
    }
}
