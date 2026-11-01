package de.belkis.spyhound.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import de.belkis.spyhound.scan.TextsEn
import de.belkis.spyhound.scan.TextsDe
import de.belkis.spyhound.scan.Progress
import de.belkis.spyhound.scan.RadarLegendCounts
import de.belkis.spyhound.scan.RadarSector
import de.belkis.spyhound.ui.LocalMotionEnabled
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.SpyTypographyExtras
import de.belkis.spyhound.ui.components.RadarCanvas
import de.belkis.spyhound.ui.components.RadarLegend
import de.belkis.spyhound.ui.components.ScanStepRow
import de.belkis.spyhound.ui.components.SpyButton
import de.belkis.spyhound.ui.components.ThinProgressLine
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

private fun stepIndex(progress: Progress): Int = if (progress.fraction >= 1f) SCAN_STEPS.size else when (progress.radar?.activeSector) {
    RadarSector.APPS -> 0
    RadarSector.DEVICE -> 1
    RadarSector.SMS -> 2
    RadarSector.FILES -> 3
    RadarSector.DEEP -> 4
    null -> 0
}

private val ScanRadarSize = 260.dp
private val ScanRadarBlockHeight = ScanRadarSize + 40.dp

@OptIn(FlowPreview::class)
@Composable
fun ScanScreen(
    progress: Progress,
    onCancel: () -> Unit,
) {
    val texts = if (LocalContext.current.resources.configuration.locales[0].language == "de") TextsDe else TextsEn
    val motion = LocalMotionEnabled.current
    val current = stepIndex(progress)
    val radar = progress.radar

    val liveDetail by rememberUpdatedState(progress.detail)
    var shownDetail by remember { mutableStateOf(progress.detail) }
    LaunchedEffect(Unit) {
        snapshotFlow { liveDetail }
            .sample(125)
            .collect { shownDetail = it }
    }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.scan_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.scan_intro),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier.height(ScanRadarBlockHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RadarCanvas(
                radar = radar,
                modifier = Modifier.size(ScanRadarSize),
            )
            RadarLegend(
                legend = radar?.legend ?: RadarLegendCounts(),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        val pct = (progress.fraction.coerceIn(0f, 1f) * 100).toInt()
        Box(
            modifier = Modifier
                .width(ScanPercentSlotWidth)
                .height(ScanPercentSlotHeight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${pct.toString().padStart(3)} %",
                style = SpyTypographyExtras.tabularHeadline,
                color = SpyColors.Amber,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        ThinProgressLine(progress.fraction, Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ScanStepSlotHeight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                texts.text(progress.step),
                style = MaterialTheme.typography.titleMedium,
                color = SpyColors.Amber,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        AnimatedContent(
            targetState = shownDetail,
            transitionSpec = {
                if (!motion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    (fadeIn(tween(150)) togetherWith fadeOut(tween(150))).using(
                        SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> tween(0) }),
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(ScanDetailSlotHeight)
                .clipToBounds(),
            label = "scan-detail",
        ) { detail ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    texts.text(detail),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth()) {
            SCAN_STEPS.forEachIndexed { index, label ->
                val done = progress.fraction >= 1f || current > index
                val active = when (index) {
                    4 -> progress.fraction in 0.72f..0.99f
                    else -> current == index && progress.fraction < 1f
                }
                ScanStepRow(label = stringResource(label), done = done, active = active)
            }
        }
        Spacer(Modifier.weight(1f))
        SpyButton(text = stringResource(R.string.scan_cancel), onClick = onCancel, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
    }
}
