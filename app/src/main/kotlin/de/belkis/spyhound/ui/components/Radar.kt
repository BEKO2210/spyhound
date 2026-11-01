package de.belkis.spyhound.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.ui.LocalMotionEnabled
import de.belkis.spyhound.ui.SpyColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val SWEEP_DEG = 60f
private const val SWEEP_FRAC = SWEEP_DEG / 360f

private fun blipPosition(index: Int, seed: Int, radius: Float, center: Offset): Offset {
    val h = (index * 7919 + seed * 104729) % 10_000
    val angle = h * 360f / 10_000f
    val dist = 0.22f + (h % 7000) / 10_000f * 0.68f
    val rad = Math.toRadians(angle.toDouble())
    return Offset(
        center.x + cos(rad).toFloat() * radius * dist,
        center.y + sin(rad).toFloat() * radius * dist,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadarSweep(
    center: Offset,
    radius: Float,
    leadAlpha: Float,
) {
    val bounds = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
    val wedge = Path().apply {
        moveTo(center.x, center.y)
        arcTo(bounds, 0f, SWEEP_DEG, false)
        close()
    }
    clipPath(wedge) {
        drawCircle(
            brush = Brush.sweepGradient(
                0f to Color.Transparent,
                (SWEEP_FRAC * 0.2f) to SpyColors.Amber.copy(alpha = leadAlpha * 0.08f),
                (SWEEP_FRAC * 0.55f) to SpyColors.Amber.copy(alpha = leadAlpha * 0.45f),
                SWEEP_FRAC to SpyColors.Amber.copy(alpha = leadAlpha),
                1f to Color.Transparent,
                center = center,
            ),
            radius = radius,
            center = center,
        )
    }
    val edgeRad = Math.toRadians(SWEEP_DEG.toDouble())
    drawLine(
        color = SpyColors.Amber.copy(alpha = (leadAlpha + 0.35f).coerceAtMost(1f)),
        start = center,
        end = Offset(
            center.x + cos(edgeRad).toFloat() * radius,
            center.y + sin(edgeRad).toFloat() * radius,
        ),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

@Composable
fun RadarCanvas(
    fraction: Float,
    modifier: Modifier = Modifier,
    blipCount: Int = min(24, (fraction * 48).toInt().coerceAtLeast(0)),
    blipSeed: Int = 42,
    sweepLeadAlpha: Float = 0.55f,
) {
    val motion = LocalMotionEnabled.current
    val infinite = rememberInfiniteTransition(label = "radar")
    val sweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    val glow by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val angle = if (motion) sweep else 52f
    val centerGlow = if (motion) glow else 0.5f
    val pulsePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "blip",
    )

    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f * 0.94f
        drawCircle(color = SpyColors.SurfaceRaised, radius = r, center = c)
        for (i in 1..5) {
            drawCircle(
                color = SpyColors.Border.copy(alpha = 0.55f + i * 0.05f),
                radius = r * i / 5f,
                center = c,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.75.dp.toPx()),
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SpyColors.Amber.copy(alpha = 0.22f * centerGlow),
                    Color.Transparent,
                ),
                center = c,
                radius = r * 0.35f,
            ),
            radius = r * 0.35f,
            center = c,
        )
        rotate(angle, pivot = c) {
            drawRadarSweep(c, r, sweepLeadAlpha)
        }
        repeat(blipCount) { i ->
            val p = blipPosition(i, blipSeed, r, c)
            val phase = (pulsePhase + i * 29f) % 360f
            val alpha = if (motion) {
                0.25f + 0.75f * ((kotlin.math.sin(Math.toRadians(phase.toDouble())) + 1) / 2).toFloat()
            } else {
                0.85f
            }
            drawCircle(color = SpyColors.Amber.copy(alpha = alpha), radius = 3.5.dp.toPx(), center = p)
            drawCircle(
                color = SpyColors.Amber.copy(alpha = alpha * 0.35f),
                radius = 7.dp.toPx(),
                center = p,
            )
        }
    }
}

@Composable
fun RadarScanButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    label: String = "Jetzt prüfen",
    diameter: Dp = 176.dp,
) {
    val motion = LocalMotionEnabled.current
    val infinite = rememberInfiniteTransition(label = "btn")
    val pulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val outer = if (motion && enabled) diameter * pulse else diameter
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(outer)
            .semantics { contentDescription = label },
        shape = CircleShape,
        color = SpyColors.SurfaceRaised,
        border = BorderStroke(1.5.dp, SpyColors.Amber.copy(alpha = 0.65f)),
    ) {
        Box(
            modifier = Modifier.defaultMinSize(minWidth = diameter, minHeight = diameter),
            contentAlignment = Alignment.Center,
        ) {
            RadarCanvas(
                fraction = 0.15f,
                modifier = Modifier.size(outer * 0.92f),
                blipCount = 3,
                blipSeed = 7,
                sweepLeadAlpha = 0.25f,
            )
            Text(label, style = MaterialTheme.typography.titleMedium, color = SpyColors.Amber)
        }
    }
}
