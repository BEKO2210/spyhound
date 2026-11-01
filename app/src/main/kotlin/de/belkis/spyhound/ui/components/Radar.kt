package de.belkis.spyhound.ui.components

import de.belkis.spyhound.R
import androidx.compose.ui.res.stringResource

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.belkis.spyhound.scan.ElementStatus
import de.belkis.spyhound.scan.RadarLegendCounts
import de.belkis.spyhound.scan.RadarMath
import de.belkis.spyhound.scan.RadarProgress
import de.belkis.spyhound.scan.RadarSector
import de.belkis.spyhound.scan.TextsEn
import androidx.compose.ui.platform.LocalContext
import de.belkis.spyhound.ui.LocalMotionEnabled
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.SpyTypographyExtras
import kotlin.math.cos
import kotlin.math.sin

private const val SWEEP_DEG = 60f
private const val SWEEP_FRAC = SWEEP_DEG / 360f
private val LegendSlotHeight = 28.dp
private val LegendItemSpacing = 20.dp

private data class DrawBlip(
    val key: Long,
    val angleDeg: Float,
    val radiusFraction: Float,
    val status: ElementStatus,
)

private fun blipsForDraw(radar: RadarProgress?): List<DrawBlip> {
    if (radar == null) return emptyList()
    val out = ArrayList<DrawBlip>()
    radar.sectors.forEach { sector ->
        if (sector.sector == RadarSector.DEEP && radar.deepLocked) return@forEach
        sector.blips.forEachIndexed { i, status ->
            val layout = RadarMath.layoutBlipInSector(sector.sector.ordinal, i, sector.blips.size)
            out += DrawBlip(
                key = sector.sector.ordinal * 100L + i,
                angleDeg = layout.angleDeg,
                radiusFraction = layout.radiusFraction,
                status = status,
            )
        }
    }
    return out
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSectorGrid(
    center: Offset,
    radius: Float,
    radar: RadarProgress?,
) {
    val span = 360f / RadarMath.SECTOR_COUNT
    for (i in 0 until RadarMath.SECTOR_COUNT) {
        val start = -90f + i * span
        val sector = RadarSector.entries[i]
        val active = radar?.activeSector == sector
        val locked = radar?.deepLocked == true && sector == RadarSector.DEEP
        if (active && radar != null && !locked) {
            val wedge = Path().apply {
                moveTo(center.x, center.y)
                arcTo(
                    Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                    start,
                    span,
                    false,
                )
                close()
            }
            drawPath(
                wedge,
                color = SpyColors.Amber.copy(alpha = 0.06f),
            )
        }
        val lineRad = Math.toRadians(start.toDouble())
        drawLine(
            color = SpyColors.Border.copy(alpha = if (locked) 0.35f else 0.65f),
            start = center,
            end = Offset(
                center.x + cos(lineRad).toFloat() * radius,
                center.y + sin(lineRad).toFloat() * radius,
            ),
            strokeWidth = 0.6.dp.toPx(),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSectorLabels(
    center: Offset,
    radius: Float,
    radar: RadarProgress?,
    labels: List<String>,
) {
    val span = 360f / RadarMath.SECTOR_COUNT
    val labelR = radius * 1.02f
    RadarSector.entries.forEachIndexed { i, sector ->
        val locked = radar?.deepLocked == true && sector == RadarSector.DEEP
        val mid = -90f + span * (i + 0.5f)
        val rad = Math.toRadians(mid.toDouble())
        val x = center.x + cos(rad).toFloat() * labelR
        val y = center.y + sin(rad).toFloat() * labelR
        val label = if (locked) {
            "🔒 ${labels[i].uppercase()}"
        } else {
            labels[i].uppercase()
        }
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(
                    if (locked) 120 else if (radar?.activeSector == sector) 230 else 170,
                    184,
                    176,
                    166,
                )
                textSize = (if (locked) 7.5f else 9f).sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawText(label, x, y + paint.textSize * 0.35f, paint)
        }
    }
}

private fun statusColor(status: ElementStatus, alpha: Float): Color = when (status) {
    ElementStatus.CLEAN -> SpyColors.IvoryMuted.copy(alpha = alpha * 0.75f)
    ElementStatus.LEAD -> SpyColors.Amber.copy(alpha = alpha)
    ElementStatus.FINDING -> SpyColors.Danger.copy(alpha = alpha)
}

@Composable
fun RadarCanvas(
    modifier: Modifier = Modifier,
    radar: RadarProgress? = null,
    showSweep: Boolean = true,
    showLabels: Boolean = true,
    sweepLeadAlpha: Float = 0.55f,
) {
    val english = LocalContext.current.resources.configuration.locales[0].language != "de"
    val sectorLabels = RadarSector.entries.map { if (english) TextsEn.text(it.label) else it.label }
    val motion = LocalMotionEnabled.current
    val blips = remember(radar) { blipsForDraw(radar) }
    val infinite = rememberInfiniteTransition(label = "radar")
    val sweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val angle = if (motion && showSweep) sweep else 0f
    var revealed by remember { mutableStateOf(setOf<Long>()) }
    LaunchedEffect(motion) {
        if (!motion) {
            revealed = blips.map { it.key }.toSet()
        }
    }
    LaunchedEffect(blips, motion) {
        if (!motion) {
            revealed = blips.map { it.key }.toSet()
        }
    }
    LaunchedEffect(angle, motion, blips) {
        if (!motion) return@LaunchedEffect
        val head = angle % 360f
        blips.forEach { b ->
            val a = (b.angleDeg % 360f + 360f) % 360f
            if (a <= head || head < 6f) {
                revealed = revealed + b.key
            }
        }
    }

    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f * 0.88f
        drawCircle(color = SpyColors.SurfaceRaised, radius = r * 1.06f, center = c)
        drawSectorGrid(c, r, radar)
        for (i in 1..5) {
            drawCircle(
                color = SpyColors.Border.copy(alpha = 0.55f + i * 0.05f),
                radius = r * i / 5f,
                center = c,
                style = Stroke(width = 0.75.dp.toPx()),
            )
        }
        if (showSweep && motion) {
            rotate(angle, pivot = c) {
                drawRadarSweep(c, r, sweepLeadAlpha)
            }
        }
        blips.forEach { b ->
            if (b.key !in revealed) return@forEach
            val rad = Math.toRadians(b.angleDeg.toDouble())
            val p = Offset(
                c.x + cos(rad).toFloat() * r * b.radiusFraction,
                c.y + sin(rad).toFloat() * r * b.radiusFraction,
            )
            val pulseScale = if (motion && b.status != ElementStatus.CLEAN) pulse else 1f
            val core = 3.2.dp.toPx() * pulseScale
            val alpha = if (b.status == ElementStatus.CLEAN) 0.9f else 1f
            drawCircle(color = statusColor(b.status, alpha), radius = core, center = p)
            if (b.status != ElementStatus.CLEAN) {
                drawCircle(
                    color = statusColor(b.status, 0.28f * pulseScale),
                    radius = 7.dp.toPx() * pulseScale,
                    center = p,
                )
            }
        }
        if (showLabels && size.minDimension >= 120.dp.toPx()) {
            drawSectorLabels(c, r, radar, sectorLabels)
        }
    }
}

@Composable
fun RadarLegend(
    legend: RadarLegendCounts,
    modifier: Modifier = Modifier,
) {
    val countStyle = SpyTypographyExtras.tabularMedium.copy(fontSize = 15.sp, lineHeight = 18.sp)
    Row(
        modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LegendItem(stringResource(R.string.t_039), SpyColors.IvoryMuted, legend.clean, countStyle, Modifier.weight(1f))
        LegendItem(stringResource(R.string.t_040), SpyColors.Amber, legend.lead, countStyle, Modifier.weight(1f))
        LegendItem(stringResource(R.string.t_041), SpyColors.Danger, legend.finding, countStyle, Modifier.weight(1f))
    }
}

@Composable
private fun LegendItem(
    label: String,
    dotColor: Color,
    count: Int,
    countStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = dotColor, style = MaterialTheme.typography.labelLarge)
            Text(
                count.toString().padStart(4),
                style = countStyle,
                color = dotColor,
                maxLines = 1,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = SpyColors.IvoryMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun RadarScanButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.t_038),
    diameter: Dp = 176.dp,
    lastRadar: RadarProgress? = null,
) {
    val motion = LocalMotionEnabled.current
    val infinite = rememberInfiniteTransition(label = "btn")
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )
    val halo = diameter * 0.1f
    Box(
        modifier = modifier
            .size(diameter + halo * 2)
            .drawBehind {
                if (motion && enabled) {
                    val base = diameter.toPx() / 2
                    drawCircle(
                        color = SpyColors.Amber.copy(alpha = 0.35f * (1f - pulse)),
                        radius = base + halo.toPx() * pulse,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(diameter)
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
                    modifier = Modifier.size(diameter * 0.92f),
                    radar = lastRadar,
                    showLabels = false,
                    sweepLeadAlpha = if (lastRadar == null) 0.25f else 0.2f,
                )
                Text(label, style = MaterialTheme.typography.titleMedium, color = SpyColors.Amber)
            }
        }
    }
}
