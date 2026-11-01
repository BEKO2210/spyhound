package de.belkis.spyhound

import de.belkis.spyhound.scan.completedAreas
import de.belkis.spyhound.scan.heldFraction
import de.belkis.spyhound.scan.sweepGlow
import de.belkis.spyhound.scan.sweepPassAge
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanMotionTest {
    @Test
    fun progressHoldsTheHighestFraction() {
        assertEquals(0.4f, heldFraction(0.4f, 0.2f))
        assertEquals(0.5f, heldFraction(0.4f, 0.5f))
        assertEquals(0f, heldFraction(0f, -1f))
        assertEquals(1f, heldFraction(0.2f, 2f))
        assertEquals(0.48f, heldFraction(0.48f, 0.48f))
    }

    @Test
    fun sweepAgeIsZeroWhenTheLeadingEdgeReachesTheBlip() {
        assertEquals(0f, sweepPassAge(blipDeg = 60f, sweepDeg = 0f, sweepWidthDeg = 60f))
        assertEquals(10f, sweepPassAge(blipDeg = 50f, sweepDeg = 0f, sweepWidthDeg = 60f))
        assertEquals(350f, sweepPassAge(blipDeg = 70f, sweepDeg = 0f, sweepWidthDeg = 60f))
        assertEquals(150f, sweepPassAge(blipDeg = -90f, sweepDeg = 0f, sweepWidthDeg = 60f))
    }

    @Test
    fun sweepGlowFadesAcrossTheTrailWindow() {
        assertEquals(1f, sweepGlow(0f))
        assertEquals(0.5f, sweepGlow(45f))
        assertEquals(0f, sweepGlow(90f))
        assertEquals(0f, sweepGlow(200f))
    }

    @Test
    fun completedAreasFollowTheActiveSectorAndFinishTogether() {
        assertEquals(0, completedAreas(0.2f, null))
        assertEquals(2, completedAreas(0.5f, 2))
        assertEquals(4, completedAreas(0.9f, 4))
        assertEquals(5, completedAreas(1f, 2))
        assertEquals(0, completedAreas(0.3f, 9))
    }
}
