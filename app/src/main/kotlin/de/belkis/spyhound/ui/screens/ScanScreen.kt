package de.belkis.spyhound.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.R
import de.belkis.spyhound.scan.Progress
import de.belkis.spyhound.scan.RadarLegendCounts
import de.belkis.spyhound.scan.RadarProgress
import de.belkis.spyhound.scan.TextsDe
import de.belkis.spyhound.scan.TextsEn
import de.belkis.spyhound.scan.completedAreas
import de.belkis.spyhound.scan.heldFraction
import de.belkis.spyhound.ui.LocalMotionEnabled
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.SpyTypographyExtras
import de.belkis.spyhound.ui.components.RadarCanvas
import de.belkis.spyhound.ui.components.RadarLegend
import de.belkis.spyhound.ui.components.SpyButton
import de.belkis.spyhound.ui.components.ThinProgressLine
import de.belkis.spyhound.ui.design.SpyHaptics
import de.belkis.spyhound.ui.design.SpyMotion
import de.belkis.spyhound.ui.design.SpySpacing
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.sample

private val SCAN_STEPS = listOf(
    R.string.scan_step_apps, R.string.scan_step_device, R.string.scan_step_sms,
    R.string.scan_step_files, R.string.scan_step_deep,
)

private val ScanPercentSlotHeight = 40.dp
private val ScanPercentSlotWidth = 120.dp
private val ScanStepSlotHeight = 22.dp
private val ScanDetailSlotHeight = 24.dp
private val ScanRadarSize = 260.dp
private val ScanRadarBlockHeight = ScanRadarSize + 40.dp
private val ScanStepRowHeight = 48.dp
private val ScanStepListHeight = ScanStepRowHeight * SCAN_STEPS.size
private val ScanCancelReserve = 64.dp

@OptIn(FlowPreview::class)
@Composable
fun ScanScreen(
    progress: Progress,
    onCancel: () -> Unit,
) {
    val texts = if (LocalContext.current.resources.configuration.locales[0].language == "de") TextsDe else TextsEn
    val motion = LocalMotionEnabled.current
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val current = completedAreas(progress.fraction, progress.radar?.activeSector?.ordinal)
    val radar = progress.radar
    val context = LocalContext.current
    val view = LocalView.current
    val haptics = remember(context) { SpyHaptics(context) }

    val liveDetail by rememberUpdatedState(progress.detail)
    var shownDetail by remember { mutableStateOf(progress.detail) }
    LaunchedEffect(Unit) {
        snapshotFlow { liveDetail }
            .sample(125)
            .collect { shownDetail = it }
    }

    var held by remember { mutableFloatStateOf(0f) }
    val fraction = heldFraction(held, progress.fraction)
    SideEffect { held = fraction }
    val shown by animateFloatAsState(
        targetValue = fraction,
        animationSpec = if (motion) tween(SpyMotion.Element, easing = SpyMotion.Enter) else tween(0),
        label = "scan fraction",
    )
    val pct = (shown * 100f).toInt().coerceIn(0, 100)

    val findings = radar?.legend?.finding ?: 0
    var heldDone by remember { mutableIntStateOf(current) }
    var heldFindings by remember { mutableIntStateOf(findings) }
    LaunchedEffect(Unit) { haptics.tick(view) }
    LaunchedEffect(current) {
        if (current > heldDone) {
            repeat(current - heldDone) { haptics.tick(view) }
            heldDone = current
        }
    }
    LaunchedEffect(findings) {
        if (findings > heldFindings) {
            haptics.alert(view)
            heldFindings = findings
        }
    }

    val shimmerTransition = rememberInfiniteTransition(label = "scan-step-shimmer")
    val shimmer by shimmerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = SpyMotion.Hero, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "scan-step-shimmer-phase",
    )

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = ScanCancelReserve),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScanHeading()
            if (wide) {
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = SpySpacing.TabletEdge),
                    horizontalArrangement = Arrangement.spacedBy(SpySpacing.Section),
                ) {
                    Column(
                        Modifier
                            .weight(5f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ScanRadar(radar)
                        RadarLegend(legend = radar?.legend ?: RadarLegendCounts(), modifier = Modifier.padding(top = 8.dp))
                        Spacer(Modifier.height(16.dp))
                        ScanPercent(pct)
                        Spacer(Modifier.height(8.dp))
                        ThinProgressLine(shown, Modifier.padding(horizontal = 4.dp))
                        Spacer(Modifier.weight(1f))
                    }
                    Column(
                        Modifier
                            .weight(7f)
                            .fillMaxHeight(),
                    ) {
                        ScanNow(texts.text(progress.step), texts.text(shownDetail), motion)
                        Spacer(Modifier.height(16.dp))
                        Spacer(Modifier.height(12.dp))
                        ScanSteps(progress, current, shimmer, motion)
                        Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                Column(
                    modifier = Modifier.height(ScanRadarBlockHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ScanRadar(radar)
                    RadarLegend(
                        legend = radar?.legend ?: RadarLegendCounts(),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                ScanPercent(pct)
                Spacer(Modifier.height(8.dp))
                ThinProgressLine(shown, Modifier.padding(horizontal = 4.dp))
                Spacer(Modifier.height(16.dp))
                ScanNow(texts.text(progress.step), texts.text(shownDetail), motion)
                Spacer(Modifier.height(8.dp))
                ScanSteps(progress, current, shimmer, motion)
                Spacer(Modifier.weight(1f))
            }
        }
        if (wide) {
            SpyButton(
                text = stringResource(R.string.scan_cancel),
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = SpySpacing.TabletEdge, bottom = 8.dp)
                    .fillMaxWidth(7f / 12f),
            )
        } else {
            SpyButton(
                text = stringResource(R.string.scan_cancel),
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ScanHeading() {
    Text(
        stringResource(R.string.scan_title),
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(6.dp))
    Text(
        stringResource(R.string.scan_intro),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun ScanRadar(radar: RadarProgress?) {
    val radarLabel = stringResource(R.string.scan_radar_cd)
    RadarCanvas(
        radar = radar,
        modifier = Modifier
            .size(ScanRadarSize)
            .clearAndSetSemantics { contentDescription = radarLabel },
        sweepTrail = true,
        blipFlash = true,
    )
}

@Composable
private fun ScanPercent(pct: Int) {
    val percentLabel = stringResource(R.string.scan_percent_cd, pct)
    Box(
        modifier = Modifier
            .width(ScanPercentSlotWidth)
            .height(ScanPercentSlotHeight)
            .clearAndSetSemantics { contentDescription = percentLabel },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = stringResource(R.string.scan_percent, pct.toString().padStart(3, '\u2007')),
            style = SpyTypographyExtras.tabularHeadline,
            color = SpyColors.Amber,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ScanNow(step: String, detail: String, motion: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ScanStepSlotHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            step,
            style = MaterialTheme.typography.titleMedium,
            color = SpyColors.Amber,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
    AnimatedContent(
        targetState = detail,
        transitionSpec = {
            if (!motion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                (fadeIn(tween(SpyMotion.Micro)) togetherWith fadeOut(tween(SpyMotion.Micro))).using(
                    SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> tween(0) }),
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(ScanDetailSlotHeight)
            .clipToBounds(),
        label = "scan-detail",
    ) { line ->
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                line,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ScanSteps(progress: Progress, current: Int, shimmer: Float, motion: Boolean) {
    Column(Modifier.fillMaxWidth().height(ScanStepListHeight)) {
        SCAN_STEPS.forEachIndexed { index, label ->
            val done = progress.fraction >= 1f || current > index
            val active = when (index) {
                4 -> progress.fraction in 0.72f..0.99f
                else -> current == index && progress.fraction < 1f
            }
            ScanStepLine(
                label = stringResource(label),
                done = done,
                active = active,
                shimmer = if (motion) shimmer else -1f,
            )
        }
    }
}

@Composable
private fun ScanStepLine(label: String, done: Boolean, active: Boolean, shimmer: Float) {
    var settled by remember { mutableStateOf(false) }
    val trim by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = if (shimmer < 0f || !settled) tween(0) else tween(SpyMotion.Element, easing = SpyMotion.Enter),
        label = "scan check",
    )
    LaunchedEffect(Unit) { settled = true }
    val description = stringResource(
        when {
            done -> R.string.scan_step_done
            active -> R.string.scan_step_active
            else -> R.string.scan_step_pending
        },
        label,
    )
    Row(
        Modifier
            .fillMaxWidth()
            .height(ScanStepRowHeight)
            .clearAndSetSemantics { contentDescription = description }
            .drawBehind {
                if (active && shimmer >= 0f) {
                    val band = size.width * 0.4f
                    val x = -band + (size.width + band) * shimmer
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                SpyColors.Amber.copy(alpha = 0.16f),
                                Color.Transparent,
                            ),
                            startX = x,
                            endX = x + band,
                        ),
                    )
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(24.dp)) {
            val stroke = 1.75.dp.toPx()
            val ring = when {
                done -> SpyColors.Clear
                active -> SpyColors.Amber
                else -> SpyColors.Border
            }
            drawCircle(
                color = ring,
                radius = size.minDimension / 2f - stroke / 2f,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (trim <= 0f) return@Canvas
            val start = Offset(6.2.dp.toPx(), 12.4.dp.toPx())
            val knee = Offset(10.2.dp.toPx(), 16.2.dp.toPx())
            val end = Offset(17.6.dp.toPx(), 8.0.dp.toPx())
            val first = (knee - start).getDistance()
            val second = (end - knee).getDistance()
            val drawn = (first + second) * trim.coerceIn(0f, 1f)
            fun point(from: Offset, to: Offset, distance: Float): Offset {
                val span = (to - from).getDistance().coerceAtLeast(0.01f)
                val t = (distance / span).coerceIn(0f, 1f)
                return Offset(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)
            }
            if (drawn <= first) {
                drawLine(SpyColors.Clear, start, point(start, knee, drawn), stroke, StrokeCap.Round)
            } else {
                drawLine(SpyColors.Clear, start, knee, stroke, StrokeCap.Round)
                drawLine(SpyColors.Clear, knee, point(knee, end, drawn - first), stroke, StrokeCap.Round)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                done -> SpyColors.IvoryMuted
                active -> SpyColors.Ivory
                else -> SpyColors.IvoryMuted.copy(alpha = 0.7f)
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
