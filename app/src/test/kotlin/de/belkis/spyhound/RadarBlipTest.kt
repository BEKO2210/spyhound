package de.belkis.spyhound

import de.belkis.spyhound.scan.ElementStatus
import de.belkis.spyhound.scan.RadarMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarBlipTest {
    @Test
    fun bundleCapsAt24AndMergesSeverity() {
        val many = List(300) { ElementStatus.CLEAN }
        val bundled = RadarMath.bundleBlips(many)
        assertEquals(24, bundled.size)
        assertTrue(bundled.all { it == ElementStatus.CLEAN })

        val mixed = buildList {
            addAll(List(50) { ElementStatus.CLEAN })
            add(ElementStatus.LEAD)
            add(ElementStatus.FINDING)
        }
        val b2 = RadarMath.bundleBlips(mixed)
        assertTrue(b2.size <= 24)
        assertEquals(ElementStatus.FINDING, b2.last())
    }

    @Test
    fun blipRadiusIncreasesWithIndex() {
        val first = RadarMath.layoutBlipInSector(0, 0, 8)
        val last = RadarMath.layoutBlipInSector(0, 7, 8)
        assertTrue(last.radiusFraction > first.radiusFraction)
    }

    @Test
    fun blipAnglesStayInsideSector() {
        val span = 360f / RadarMath.SECTOR_COUNT
        repeat(RadarMath.SECTOR_COUNT) { sector ->
            repeat(12) { i ->
                val layout = RadarMath.layoutBlipInSector(sector, i, 12)
                val start = -90f + sector * span
                val end = start + span
                assertTrue(layout.angleDeg > start)
                assertTrue(layout.angleDeg < end)
            }
        }
    }

    @Test
    fun mergeStatusPrefersFindingThenLead() {
        assertEquals(ElementStatus.FINDING, RadarMath.mergeStatus(ElementStatus.CLEAN, ElementStatus.FINDING))
        assertEquals(ElementStatus.LEAD, RadarMath.mergeStatus(ElementStatus.CLEAN, ElementStatus.LEAD))
        assertEquals(ElementStatus.FINDING, RadarMath.mergeStatus(ElementStatus.LEAD, ElementStatus.FINDING))
    }
}
