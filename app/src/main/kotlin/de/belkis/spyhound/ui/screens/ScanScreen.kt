package de.belkis.spyhound.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.scan.Progress
import de.belkis.spyhound.ui.LocalMotionEnabled
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.components.SpyButton
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ScanScreen(
    progress: Progress,
    onCancel: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Witterung", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Der Spürhund folgt Spuren im Gerät — offline, ohne Datenabfluss.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(32.dp))
        RadarAnimation(
            fraction = progress.fraction,
            modifier = Modifier.size(280.dp),
        )
        Spacer(Modifier.height(28.dp))
        Text(progress.step, style = MaterialTheme.typography.titleMedium, color = SpyColors.Amber)
        Spacer(Modifier.height(6.dp))
        Text(progress.detail, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progress.fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = SpyColors.Amber,
            trackColor = SpyColors.Border,
            strokeCap = StrokeCap.Round,
        )
        Spacer(Modifier.height(32.dp))
        SpyButton(text = "Abbrechen", onClick = onCancel, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RadarAnimation(fraction: Float, modifier: Modifier = Modifier) {
    val motion = LocalMotionEnabled.current
    val infinite = rememberInfiniteTransition(label = "radar")
    val sweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val angle = if (motion) sweep else 45f
    val scale = if (motion) pulse else 1f
    val blipCount = min(12, (fraction * 24).toInt().coerceAtLeast(1))

    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f * scale
        for (i in 1..4) {
            drawCircle(
                color = SpyColors.Border,
                radius = r * i / 4f,
                center = c,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        drawCircle(color = SpyColors.Amber.copy(alpha = 0.15f), radius = r, center = c)
        val rad = Math.toRadians(angle.toDouble())
        drawLine(
            color = SpyColors.Amber.copy(alpha = 0.85f),
            start = c,
            end = Offset(c.x + cos(rad).toFloat() * r, c.y + sin(rad).toFloat() * r),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        repeat(blipCount) { i ->
            val a = (i * 47 + fraction * 120) % 360
            val br = r * (0.25f + (i % 3) * 0.18f)
            val ar = Math.toRadians(a.toDouble())
            val p = Offset(c.x + cos(ar).toFloat() * br, c.y + sin(ar).toFloat() * br)
            drawCircle(color = SpyColors.Amber, radius = 4.dp.toPx(), center = p)
        }
    }
}

@Composable
fun RadarScanButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val motion = LocalMotionEnabled.current
    val infinite = rememberInfiniteTransition(label = "btn")
    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val outer = if (motion && enabled) 200.dp * pulse else 200.dp
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(outer)
            .semantics { contentDescription = "Prüfen" },
        shape = CircleShape,
        color = SpyColors.SurfaceRaised,
        border = androidx.compose.foundation.BorderStroke(2.dp, SpyColors.Amber),
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 200.dp, minHeight = 200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(outer)) {
                val c = center
                val r = size.minDimension / 2f * 0.92f
                drawCircle(color = SpyColors.Amber.copy(alpha = 0.1f), radius = r, center = c)
                for (i in 1..3) {
                    drawCircle(
                        color = SpyColors.Border,
                        radius = r * i / 3f,
                        center = c,
                        style = Stroke(1.dp.toPx()),
                    )
                }
            }
            Text("Prüfen", style = MaterialTheme.typography.headlineMedium, color = SpyColors.Amber)
        }
    }
}
