package de.belkis.spyhound.ui.design

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

object SpyMotion {
    val Enter = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Exit = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    const val Micro = 120
    const val Element = 220
    const val Screen = 380
    const val Hero = 600
    const val Stagger = 40
    const val MaxStagger = 8
    const val DampingRatio = 0.8f
    const val Stiffness = 380f
    val Press = spring<Float>(dampingRatio = DampingRatio, stiffness = Stiffness)
}

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

@Composable
fun staggeredAppear(index: Int, visible: Boolean = true): Modifier {
    val reduced = rememberReducedMotion()
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val alpha by animateFloatAsState(
        targetValue = if ((visible && entered) || reduced) 1f else 0f,
        animationSpec = tween(SpyMotion.Element, delayMillis = index.coerceIn(0, SpyMotion.MaxStagger - 1) * SpyMotion.Stagger, easing = SpyMotion.Enter),
        label = "stagger alpha",
    )
    return Modifier.graphicsLayer { this.alpha = alpha; translationY = (1f - alpha) * 16.dp.toPx() }
}

@Composable
fun countUp(value: Int): Int {
    val reduced = rememberReducedMotion()
    val count by animateIntAsState(value, if (reduced) tween(0) else tween(SpyMotion.Hero, easing = SpyMotion.Enter), label = "count up")
    return count
}
