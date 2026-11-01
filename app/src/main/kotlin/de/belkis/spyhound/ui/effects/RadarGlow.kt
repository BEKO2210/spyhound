package de.belkis.spyhound.ui.effects

import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.ui.LocalMotionEnabled
import org.intellij.lang.annotations.Language

private val MaxGlowRadius = 24.dp
private const val RADAR_DISC_RATIO = 0.88f
private const val TRAIL_SWEEP_FRAC = 60f / 360f
private const val MAX_RADIAL_ALPHA = 0.08f
private const val MAX_TRAIL_ALPHA = 0.14f
private const val TWO_PI = 6.2831855f

/**
 * AGSL shader source for the ambient radar glow (API 33+).
 *
 * Combines a soft radial center shimmer, a trailing afterglow behind [sweepAngle],
 * and calm procedural grain capped at 3% luminance. Outputs premultiplied alpha.
 */
@Language("AGSL")
private const val RADAR_GLOW_AGSL = """
    uniform float2 resolution;
    uniform float2 center;
    uniform float radius;
    uniform float sweepAngle;
    uniform float time;
    uniform float intensity;
    uniform float4 color;

    float hash(float2 p) {
        return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453123);
    }

    float smoothNoise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        float a = hash(i);
        float b = hash(i + float2(1.0, 0.0));
        float c = hash(i + float2(0.0, 1.0));
        float d = hash(i + float2(1.0, 1.0));
        return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
    }

    half4 main(float2 fragCoord) {
        float safeRadius = max(radius, 1.0);
        float2 delta = fragCoord - center;
        float dist = length(delta);
        float normDist = dist / safeRadius;

        if (normDist >= 1.0) {
            return half4(0.0);
        }

        // Soft outer envelope and subtle center shimmer (<= 8% luminance).
        float edgeMask = 1.0 - smoothstep(0.72, 1.0, normDist);
        float radialGlow = exp(-normDist * normDist * 2.6) * edgeMask * 0.08;

        // Trailing wake behind sweepAngle (radians, clockwise in screen space).
        float angle = atan(delta.y, delta.x);
        float behind = mod(sweepAngle - angle, 6.2831853);
        float trailArc = 1.0 - smoothstep(0.0, 1.15, behind);
        float centerMute = smoothstep(0.0, 0.10, normDist);
        float trailGlow = trailArc * trailArc * centerMute * edgeMask * 0.14;

        // Calm, fine-grained noise capped at 3% brightness.
        float2 uv = fragCoord / max(resolution.x, 1.0);
        float grain = smoothNoise(uv * 28.0 + float2(time * 0.35, -time * 0.25)) - 0.5;
        float noise = grain * 0.03 * edgeMask;

        float clampedIntensity = clamp(intensity, 0.0, 1.0);
        float alpha = clamp((radialGlow + trailGlow + noise) * clampedIntensity * color.a, 0.0, 0.22);
        return half4(half3(color.rgb * alpha), half(alpha));
    }
"""

/**
 * Draws a subtle radial radar glow and trailing sweep afterglow behind the content.
 *
 * Uses an AGSL [RuntimeShader] on Android 13 (API 33+) and falls back to cached
 * [Brush.radialGradient] and [Brush.sweepGradient] brushes on API 29–32.
 * Never alters layout size and performs zero allocations during frame drawing.
 * Respects reduced motion (`Settings.Global.ANIMATOR_DURATION_SCALE == 0f`) by rendering
 * a static glow without time-animated noise.
 *
 * @param sweepAngle Provider for the current radar sweep angle in radians.
 * @param intensity Overall glow strength in `0f..1f`.
 * @param color Base accent color; defaults to `#FFB547` (Amber).
 */
fun Modifier.radarGlow(
    sweepAngle: () -> Float,
    intensity: Float = 1f,
    color: Color = Color(0xFFFFB547),
): Modifier = composed {
    val motionEnabled = rememberReducedMotionAware()
    val timeSeconds = rememberGlowTimeSeconds(motionEnabled)
    val staticAngle = remember(motionEnabled) { if (!motionEnabled) sweepAngle() else 0f }
    val angleProvider: () -> Float = if (motionEnabled) sweepAngle else ({ staticAngle })

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(RADAR_GLOW_AGSL) }
        val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
        this.radarGlowAgsl(
            shader = shader,
            paint = paint,
            sweepAngle = angleProvider,
            timeSeconds = { timeSeconds.value },
            motionEnabled = motionEnabled,
            intensity = intensity,
            color = color,
        )
    } else {
        this.radarGlowFallback(
            sweepAngle = angleProvider,
            intensity = intensity,
            color = color,
        )
    }
}

@Composable
private fun rememberReducedMotionAware(): Boolean {
    val context = LocalContext.current
    val themeMotion = LocalMotionEnabled.current
    val systemScaleEnabled = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
    return themeMotion && systemScaleEnabled
}

@Composable
private fun rememberGlowTimeSeconds(motionEnabled: Boolean): State<Float> {
    // Read only inside the draw phase, so the noise animates without recomposing every frame.
    if (!motionEnabled) return remember { mutableFloatStateOf(0f) }
    val infinite = rememberInfiniteTransition(label = "radarGlowTime")
    return infinite.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI * 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glowTime",
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Modifier.radarGlowAgsl(
    shader: RuntimeShader,
    paint: Paint,
    sweepAngle: () -> Float,
    timeSeconds: () -> Float,
    motionEnabled: Boolean,
    intensity: Float,
    color: Color,
): Modifier = this.drawWithCache {
    val discRadius = size.minDimension * 0.5f * RADAR_DISC_RATIO
    val maxHaloPx = MaxGlowRadius.toPx()
    val glowRadius = (discRadius + maxHaloPx).coerceAtMost(size.minDimension * 0.5f + maxHaloPx)
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f

    shader.setFloatUniform("resolution", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
    shader.setFloatUniform("center", cx, cy)
    shader.setFloatUniform("radius", glowRadius.coerceAtLeast(1f))
    shader.setFloatUniform("color", color.red, color.green, color.blue, color.alpha)

    onDrawBehind {
        val clampedIntensity = intensity.coerceIn(0f, 1f)
        if (clampedIntensity <= 0f) return@onDrawBehind

        val angleRad = normalizeAngleRad(sweepAngle())
        val time = if (motionEnabled) timeSeconds() else 0f

        shader.setFloatUniform("sweepAngle", angleRad)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("intensity", clampedIntensity)
        paint.shader = shader

        drawContext.canvas.nativeCanvas.drawCircle(cx, cy, glowRadius, paint)
    }
}

private fun Modifier.radarGlowFallback(
    sweepAngle: () -> Float,
    intensity: Float,
    color: Color,
): Modifier = this.drawWithCache {
    val centerOffset = Offset(size.width * 0.5f, size.height * 0.5f)
    val discRadius = size.minDimension * 0.5f * RADAR_DISC_RATIO
    val maxHaloPx = MaxGlowRadius.toPx()
    val glowRadius = (discRadius + maxHaloPx)
        .coerceAtMost(size.minDimension * 0.5f + maxHaloPx)
        .coerceAtLeast(1f)

    val radialBrush = Brush.radialGradient(
        0.0f to color.copy(alpha = MAX_RADIAL_ALPHA),
        0.65f to color.copy(alpha = MAX_RADIAL_ALPHA * 0.45f),
        1.0f to Color.Transparent,
        center = centerOffset,
        radius = glowRadius,
    )

    // Trailing wake occupies the sector [1 - TRAIL_SWEEP_FRAC .. 1] behind 0 degrees.
    val trailStart = 1f - TRAIL_SWEEP_FRAC
    val sweepBrush = Brush.sweepGradient(
        0.0f to Color.Transparent,
        trailStart to Color.Transparent,
        (trailStart + TRAIL_SWEEP_FRAC * 0.35f) to color.copy(alpha = MAX_TRAIL_ALPHA * 0.25f),
        (trailStart + TRAIL_SWEEP_FRAC * 0.75f) to color.copy(alpha = MAX_TRAIL_ALPHA * 0.65f),
        1.0f to color.copy(alpha = MAX_TRAIL_ALPHA),
        center = centerOffset,
    )

    onDrawBehind {
        val clampedIntensity = intensity.coerceIn(0f, 1f)
        if (clampedIntensity <= 0f) return@onDrawBehind

        drawCircle(
            brush = radialBrush,
            radius = glowRadius,
            center = centerOffset,
            alpha = clampedIntensity,
        )

        val angleDeg = Math.toDegrees(normalizeAngleRad(sweepAngle()).toDouble()).toFloat()
        rotate(degrees = angleDeg, pivot = centerOffset) {
            drawCircle(
                brush = sweepBrush,
                radius = discRadius.coerceAtLeast(1f),
                center = centerOffset,
                alpha = clampedIntensity,
            )
        }
    }
}

private fun normalizeAngleRad(angle: Float): Float {
    if (!angle.isFinite()) return 0f
    val r = angle % TWO_PI
    return if (r < 0f) r + TWO_PI else r
}
