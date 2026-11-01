package de.belkis.spyhound.scan

import kotlin.math.max

internal const val SCAN_AREA_COUNT = 5

/** Highest fraction seen so far. The scan never draws progress backwards. */
internal fun heldFraction(held: Float, incoming: Float): Float {
    val next = incoming.coerceIn(0f, 1f)
    return max(held, next)
}

/** Degrees since the sweep's leading edge passed [blipDeg], in `0..360`. */
internal fun sweepPassAge(blipDeg: Float, sweepDeg: Float, sweepWidthDeg: Float = 60f): Float {
    val head = wrapDeg(sweepDeg + sweepWidthDeg)
    return wrapDeg(head - wrapDeg(blipDeg))
}

/** 1 on the leading edge, 0 once the beam has travelled [windowDeg] past the point. */
internal fun sweepGlow(ageDeg: Float, windowDeg: Float = 90f): Float {
    if (windowDeg <= 0f) return 0f
    return (1f - ageDeg / windowDeg).coerceIn(0f, 1f)
}

internal fun wrapDeg(deg: Float): Float {
    val wrapped = deg % 360f
    return if (wrapped < 0f) wrapped + 360f else wrapped
}

/** How many scan areas are finished. The active area itself is not counted. */
internal fun completedAreas(fraction: Float, activeSectorOrdinal: Int?): Int {
    if (fraction >= 1f) return SCAN_AREA_COUNT
    val current = activeSectorOrdinal ?: 0
    return if (current in 0 until SCAN_AREA_COUNT) current else 0
}
