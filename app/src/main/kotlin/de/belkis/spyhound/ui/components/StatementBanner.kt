package de.belkis.spyhound.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.ui.design.SpyColors
import de.belkis.spyhound.ui.design.rememberReducedMotion
import kotlinx.coroutines.delay

private const val StatementHoldMs = 4_000L
private const val StatementFadeMs = 700

/**
 * One calm line that rotates through [statements]. The slot has the height of a single line and the text never
 * wraps, so nothing around it moves. The fade and the small drift run in graphicsLayer. It waits while the user
 * touches it or [paused] is true, stays on the first statement with reduced motion, and TalkBack reads all
 * statements once instead of every rotation.
 */
@Composable
fun StatementBanner(statements: List<String>, modifier: Modifier = Modifier, paused: Boolean = false) {
    val reduced = rememberReducedMotion()
    val style = MaterialTheme.typography.bodyMedium
    val density = LocalDensity.current
    val lineHeight = with(density) { (if (style.lineHeight.isSp) style.lineHeight else style.fontSize * 1.4f).toDp() }
    var index by remember { mutableIntStateOf(0) }
    var previous by remember { mutableIntStateOf(-1) }
    var touched by remember { mutableStateOf(false) }
    val progress = remember { Animatable(1f) }
    val size = statements.size
    val holding = paused || touched

    LaunchedEffect(size, holding, reduced) {
        if (reduced || holding || size < 2) return@LaunchedEffect
        while (true) {
            delay(StatementHoldMs)
            previous = index
            index = (index + 1) % size
        }
    }
    LaunchedEffect(index) {
        if (previous < 0) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(StatementFadeMs, easing = FastOutSlowInEasing))
    }

    val summary = remember(statements) { statements.joinToString(". ") }
    val drift = with(density) { 6.dp.toPx() }
    Box(
        modifier
            .fillMaxWidth()
            .height(lineHeight)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        touched = event.changes.any { it.pressed }
                    }
                }
            }
            .clearAndSetSemantics { contentDescription = summary },
    ) {
        val now = statements.getOrNull(index % size.coerceAtLeast(1)) ?: return@Box
        val before = statements.getOrNull(previous % size.coerceAtLeast(1)).takeIf { previous >= 0 }
        if (before != null && before != now) {
            Text(
                before,
                style = style,
                color = SpyColors.IvoryMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().graphicsLayer {
                    val p = progress.value
                    alpha = 1f - p
                    translationY = -drift * p
                },
            )
        }
        Text(
            now,
            style = style,
            color = SpyColors.IvoryMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().graphicsLayer {
                val p = if (before != null && before != now) progress.value else 1f
                alpha = p
                translationY = drift * (1f - p)
            },
        )
    }
}
