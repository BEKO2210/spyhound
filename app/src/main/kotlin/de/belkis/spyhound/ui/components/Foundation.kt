package de.belkis.spyhound.ui.components

import kotlin.math.roundToInt
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import de.belkis.spyhound.R
import de.belkis.spyhound.ui.design.SpyMotion
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.design.SpyTypeScale
import de.belkis.spyhound.ui.SpyTypographyExtras
import de.belkis.spyhound.ui.design.SpyRadii
import de.belkis.spyhound.ui.design.SpySpacing

/** Card surface matching [StatusStrip]: 20 dp radius, border, soft top light, 20 dp inner padding. */
@Composable
fun SpyCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SpyColors.Surface)
            .drawWithContent {
                drawContent()
                drawLine(
                    SpyColors.Ivory.copy(alpha = 0.06f),
                    Offset(20.dp.toPx(), 1.dp.toPx()),
                    Offset(size.width - 20.dp.toPx(), 1.dp.toPx()),
                    1.dp.toPx(),
                )
            }
            .border(1.dp, SpyColors.Border, shape)
            .padding(SpySpacing.PhoneEdge),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** One-line feedback under a card action; omitted entirely when there is nothing to show. */
@Composable
fun CardFeedbackLine(
    message: String?,
    isError: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    if (message == null) return
    Box(
        modifier
            .fillMaxWidth()
            .height(20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            },
        ) {
            Icon(
                painter = painterResource(if (isError) R.drawable.ic_feedback_error else R.drawable.ic_feedback_ok),
                contentDescription = null,
                tint = if (isError) SpyColors.Danger else SpyColors.Clear,
                modifier = Modifier.size(16.dp),
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) SpyColors.IvoryMuted else SpyColors.Clear,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 22 dp icon in a 40 dp raised circle for card rows (permissions, DNS, etc.). */
@Composable
fun CardRowIconBadge(
    iconRes: Int,
    active: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val tint = if (active) SpyColors.Clear else SpyColors.IvoryMuted
    Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(SpyColors.SurfaceRaised)
            .border(1.dp, SpyColors.Border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun SpySurface(modifier: Modifier = Modifier, raised: Boolean = false, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(SpyRadii.Card)
    Surface(
        modifier = modifier.drawWithContent {
            drawContent()
            drawLine(SpyColors.Ivory.copy(alpha = 0.06f), Offset(SpyRadii.Card.toPx(), 1.dp.toPx()), Offset(size.width - SpyRadii.Card.toPx(), 1.dp.toPx()), 1.dp.toPx())
        },
        shape = shape,
        color = if (raised) SpyColors.SurfaceRaised else SpyColors.Surface,
        border = BorderStroke(1.dp, SpyColors.Border),
        content = content,
    )
}

enum class StatusKind { DEPTH, SMS, FILES, SOURCES, DNS }

enum class StatusState { ACTIVE, OFF, PARTIAL }

data class StatusItem(
    val kind: StatusKind,
    val label: String,
    val explanation: String,
    val state: StatusState,
    val depthLevel: Int = 1,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null,
)

private fun StatusKind.iconRes(): Int = when (this) {
    StatusKind.DEPTH -> R.drawable.ic_status_depth
    StatusKind.SMS -> R.drawable.ic_status_sms
    StatusKind.FILES -> R.drawable.ic_status_files
    StatusKind.SOURCES -> R.drawable.ic_status_sources
    StatusKind.DNS -> R.drawable.ic_status_dns
}

/**
 * One reading of the status panel: icon, value, name - on fixed baselines so all five line up exactly.
 * SMS, files and DNS read "on/off"; depth and sources show their own value.
 */
@Composable
private fun StatusCell(item: StatusItem, modifier: Modifier = Modifier, collapse: () -> Float = { 0f }) {
    var open by remember { mutableStateOf(false) }
    val tint by animateColorAsState(
        when (item.state) {
            StatusState.ACTIVE -> SpyColors.Clear
            StatusState.PARTIAL -> SpyColors.Amber
            StatusState.OFF -> SpyColors.Ivory.copy(alpha = 0.38f)
        },
        tween(SpyMotion.Element, easing = SpyMotion.Enter),
        label = "status tint",
    )
    val glow by animateFloatAsState(
        if (item.state == StatusState.OFF) 0f else 1f,
        tween(SpyMotion.Element, easing = SpyMotion.Enter),
        label = "status glow",
    )
    val name = when (item.kind) {
        StatusKind.DEPTH -> stringResource(R.string.status_name_depth)
        StatusKind.SOURCES -> stringResource(R.string.status_sources_name)
        StatusKind.SMS, StatusKind.FILES -> item.label
        StatusKind.DNS -> stringResource(R.string.status_name_dns)
    }
    val value = when (item.kind) {
        StatusKind.DEPTH -> stringResource(
            when (item.depthLevel.coerceIn(1, 3)) { 1 -> R.string.depth_basic; 2 -> R.string.depth_deep; else -> R.string.depth_root },
        )
        StatusKind.SOURCES -> item.label
        else -> stringResource(if (item.state == StatusState.OFF) R.string.status_value_off else R.string.status_value_on)
    }
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable { open = true }
            .semantics { contentDescription = "$name: $value. ${item.explanation}" }
            .padding(top = 14.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .drawBehind {
                    // A lit reading: soft halo in its own color, drawn so nothing moves.
                    if (glow > 0f) drawCircle(
                        Brush.radialGradient(listOf(tint.copy(alpha = 0.22f * glow), Color.Transparent)),
                        radius = size.minDimension * 0.75f,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(item.kind.iconRes()), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(
            value,
            style = SpyTypographyExtras.statusValue,
            color = if (item.state == StatusState.OFF) SpyColors.IvoryMuted else tint,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(top = 10.dp).graphicsLayer { alpha = textAlpha(collapse()) },
        )
        Text(
            name,
            style = SpyTypographyExtras.statusName,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(top = 2.dp).graphicsLayer { alpha = textAlpha(collapse()) },
        )
    }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(name) },
        text = { Text(item.explanation) },
        confirmButton = {
            if (item.action != null && item.actionLabel != null) {
                SpyButton(item.actionLabel, onClick = { open = false; item.action.invoke() }, accent = true, fillWidth = false)
            }
        },
        dismissButton = { TextActionLink(text = stringResource(R.string.t_060), onClick = { open = false }) },
        containerColor = SpyColors.SurfaceRaised,
        shape = RoundedCornerShape(SpyRadii.Card),
        tonalElevation = 0.dp,
        titleContentColor = SpyColors.Ivory,
        textContentColor = SpyColors.Ivory,
    )
}

/**
 * What this scan can use, as one instrument panel: equal columns, hairline dividers, one surface.
 * Separate pills read as five buttons of different sizes; a panel reads as one calm status.
 */
@Composable
fun StatusStrip(
    items: List<StatusItem>,
    modifier: Modifier = Modifier,
    /** 0 = full, 1 = icons only. Read during layout and drawing, so scrolling never recomposes the strip. */
    collapse: () -> Float = { 0f },
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val f = collapse().coerceIn(0f, 1f)
                val iconsOnly = minOf(StatusIconsOnlyHeight.roundToPx(), placeable.height)
                val height = (placeable.height + (iconsOnly - placeable.height) * f).roundToInt()
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
            .clip(shape)
            .background(SpyColors.Surface)
            .drawWithContent {
                drawContent()
                val inset = 14.dp.toPx()
                val step = size.width / items.size
                for (i in 1 until items.size) {
                    drawLine(SpyColors.Border, Offset(step * i, inset), Offset(step * i, size.height - inset), 1.dp.toPx())
                }
                // Soft top light, like the other surfaces.
                drawLine(SpyColors.Ivory.copy(alpha = 0.06f), Offset(20.dp.toPx(), 1.dp.toPx()), Offset(size.width - 20.dp.toPx(), 1.dp.toPx()), 1.dp.toPx())
            }
            .border(1.dp, SpyColors.Border, shape),
    ) {
        items.forEach { StatusCell(it, Modifier.weight(1f).fillMaxHeight(), collapse) }
    }
}

@Composable
fun SectionHeader(label: String, hint: String, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label.uppercase(), style = SpyTypographyExtras.sectionLabel, modifier = Modifier.weight(1f))
        SectionHintButton(
            expanded = open,
            onToggle = { open = !open },
        )
    }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(label) }, text = { Text(hint) },
        confirmButton = { TextActionLink(androidx.compose.ui.res.stringResource(de.belkis.spyhound.R.string.t_060), { open = false }) },
        containerColor = SpyColors.SurfaceRaised,
    )
}

@Composable
fun ScrollEdge(scrolled: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(8.dp).drawWithContent {
        if (scrolled) {
            drawLine(SpyColors.Amber.copy(alpha = 0.35f), Offset.Zero, Offset(size.width, 0f), 1.dp.toPx())
            drawRect(Brush.verticalGradient(listOf(SpyColors.Ink, SpyColors.Ink.copy(alpha = 0f))))
        }
    })
}

/** Shown when Android's restricted settings block READ_SMS for a sideloaded app. */
@Composable
fun SmsBlockedDialog(onOpenAppInfo: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sms_blocked_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.sms_blocked_intro))
                Text(emphasizeLabels(stringResource(R.string.sms_blocked_step1)))
                Text(emphasizeLabels(stringResource(R.string.sms_blocked_step2)))
                Text(emphasizeLabels(stringResource(R.string.sms_blocked_step3)))
            }
        },
        confirmButton = {
            SpyButton(stringResource(R.string.sms_blocked_open), onClick = onOpenAppInfo, accent = true, fillWidth = false)
        },
        dismissButton = { TextActionLink(text = stringResource(R.string.t_060), onClick = onDismiss) },
        containerColor = SpyColors.SurfaceRaised,
        shape = RoundedCornerShape(SpyRadii.Card),
        tonalElevation = 0.dp,
        titleContentColor = SpyColors.Ivory,
        textContentColor = SpyColors.Ivory,
    )
}

/** Height of the status strip when only the icons show: top padding, icon and the same space below. */
private val StatusIconsOnlyHeight = 56.dp

/** The labels fade out gently over most of the fold, smoothed at both ends, and are gone before the strip gets too low to hold them. */
private fun textAlpha(collapse: Float): Float {
    val t = (1f - collapse / 0.7f).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
