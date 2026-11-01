package de.belkis.spyhound

import de.belkis.spyhound.scan.ElementStatus
import de.belkis.spyhound.scan.RadarMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

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
    fun blipRadiusStaysInScanBand() {
        repeat(RadarMath.SECTOR_COUNT) { sector ->
            val layouts = RadarMath.layoutBlipsInSector(sector, 24)
            layouts.forEach { layout ->
                assertTrue(layout.radiusFraction >= RadarMath.BLIP_RADIUS_MIN)
                assertTrue(layout.radiusFraction <= RadarMath.BLIP_RADIUS_MAX)
            }
        }
    }

    @Test
    fun blipLayoutsAreDeterministic() {
        val a = RadarMath.layoutBlipsInSector(2, 18)
        val b = RadarMath.layoutBlipsInSector(2, 18)
        assertEquals(a, b)
    }

    @Test
    fun blipLayoutsRespectMinSeparation() {
        repeat(RadarMath.SECTOR_COUNT) { sector ->
            val layouts = RadarMath.layoutBlipsInSector(sector, RadarMath.MAX_BLIPS)
            for (i in layouts.indices) {
                for (j in i + 1 until layouts.size) {
                    val a = layouts[i]
                    val b = layouts[j]
                    val d = polarDistance(a.radiusFraction, a.angleDeg, b.radiusFraction, b.angleDeg)
                    assertTrue(
                        "sector $sector blips $i/$j too close: $d",
                        d >= RadarMath.BLIP_MIN_SEPARATION * 0.98f,
                    )
                }
            }
        }
    }

    private fun polarDistance(r1: Float, a1Deg: Float, r2: Float, a2Deg: Float): Float {
        val a1 = Math.toRadians(a1Deg.toDouble())
        val a2 = Math.toRadians(a2Deg.toDouble())
        val x1 = r1 * cos(a1)
        val y1 = r1 * sin(a1)
        val x2 = r2 * cos(a2)
        val y2 = r2 * sin(a2)
        return hypot(x1 - x2, y1 - y2).toFloat()
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
