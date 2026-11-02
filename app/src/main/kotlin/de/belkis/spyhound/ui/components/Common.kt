package de.belkis.spyhound.ui.components

import de.belkis.spyhound.R
import androidx.compose.ui.res.stringResource

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.ui.LocalMotionEnabled
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.SpyTypographyExtras
import de.belkis.spyhound.ui.design.SpyHaptics
import de.belkis.spyhound.ui.design.SpyMotion
import de.belkis.spyhound.ui.design.SpyRadii
import de.belkis.spyhound.ui.design.countUp
import de.belkis.spyhound.ui.design.rememberReducedMotion

enum class SpyButtonStyle { PRIMARY, SECONDARY, TEXT }

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = SpyTypographyExtras.sectionLabel,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun SpyDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = SpyColors.Border, thickness = 1.dp)
}

@Composable
fun SpyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
    singleLine: Boolean = false,
    style: SpyButtonStyle = if (accent) SpyButtonStyle.PRIMARY else SpyButtonStyle.SECONDARY,
) {
    val bg = when (style) { SpyButtonStyle.PRIMARY -> SpyColors.Amber; SpyButtonStyle.SECONDARY -> SpyColors.SurfaceRaised; SpyButtonStyle.TEXT -> Color.Transparent }
    val fg = if (style == SpyButtonStyle.PRIMARY) SpyColors.Ink else if (style == SpyButtonStyle.TEXT) SpyColors.Amber else SpyColors.Ivory
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, SpyMotion.Press, label = "button press")
    val reducedMotion = rememberReducedMotion()
    val context = LocalContext.current
    val view = LocalView.current
    val haptics = remember(context) { SpyHaptics(context) }
    val widthMod = if (fillWidth) Modifier.fillMaxWidth() else Modifier.wrapContentWidth()
    Surface(
        onClick = { haptics.tick(view); onClick() },
        enabled = enabled,
        modifier = modifier
            .then(widthMod)
            .graphicsLayer { scaleX = if (reducedMotion) 1f else scale; scaleY = if (reducedMotion) 1f else scale }
            .defaultMinSize(minHeight = 48.dp)
            .semantics { contentDescription = text },
        shape = RoundedCornerShape(SpyRadii.Card),
        color = bg,
        border = if (style == SpyButtonStyle.TEXT) null else BorderStroke(1.dp, if (style == SpyButtonStyle.PRIMARY) SpyColors.Amber else SpyColors.Border),
        contentColor = fg,
        interactionSource = interaction,
    ) {
        Box(
            modifier = Modifier
                .then(widthMod)
                .defaultMinSize(minHeight = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                textAlign = TextAlign.Center,
                maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                softWrap = !singleLine,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
    }
}

@Composable
fun SpyNavigateBack(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val backLabel = label ?: stringResource(R.string.back)
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = backLabel }
            .padding(end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = SpyColors.Amber,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = backLabel,
            style = MaterialTheme.typography.labelLarge,
            color = SpyColors.Amber,
        )
    }
}

@Composable
fun TextActionLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SpyColors.Amber,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            color = color,
            textDecoration = TextDecoration.Underline,
        ),
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                role = Role.Button,
            )
            .semantics { contentDescription = text }
            .padding(vertical = 8.dp),
    )
}

@Composable
fun WhyLink(expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val stateLabel = stringResource(if (expanded) R.string.result_collapse else R.string.result_expand)
    val label = stringResource(R.string.t_001)
    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(onClick = onToggle, role = Role.Button)
            .semantics {
                contentDescription = "$label. $stateLabel"
                stateDescription = stateLabel
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SpyColors.Amber)
        Icon(
            Icons.Default.ExpandMore,
            contentDescription = null,
            tint = SpyColors.Amber,
            modifier = Modifier
                .size(20.dp)
                .rotate(if (expanded) 180f else 0f),
        )
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    count: Int? = null,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    titleColor: Color = SpyColors.Ivory,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by remember { mutableStateOf(initiallyExpanded) }
    val stateLabel = stringResource(if (open) R.string.result_collapse else R.string.result_expand)
    val headerLabel = if (count != null) stringResource(R.string.section_count, title, count) else title
    val headerCd = "$headerLabel. $stateLabel"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(220)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable(role = Role.Button) { open = !open }
                .semantics {
                    contentDescription = headerCd
                    stateDescription = stateLabel
                }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (count != null) stringResource(R.string.section_count, title, count) else title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
            )
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = null,
                tint = SpyColors.IvoryMuted,
                modifier = Modifier.rotate(if (open) 180f else 0f),
            )
        }
        if (open) {
            Column(Modifier.padding(bottom = 12.dp), content = content)
        }
    }
}

@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier) {
    SpySurface(modifier = modifier, raised = true) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = SpyColors.IvoryMuted,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun AnimatedCount(value: Int, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle) {
    val animated = countUp(value)
    Text(
        animated.toString(),
        style = style,
        modifier = modifier,
    )
}

@Composable
fun DepthProgressBar(
    current: AccessLevel,
    modifier: Modifier = Modifier,
) {
    val stageLabels = listOf(
        stringResource(R.string.depth_basic),
        stringResource(R.string.depth_deep),
        stringResource(R.string.depth_root),
    )
    val filledStages = when (current) {
        AccessLevel.APP -> 1
        AccessLevel.SHELL -> 2
        AccessLevel.ROOT -> 3
    }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(3) { index ->
                val filled = index < filledStages
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
                    2 -> RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
                    else -> RoundedCornerShape(0.dp)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(shape)
                        .background(if (filled) SpyColors.Amber else SpyColors.SurfaceRaised)
                        .then(
                            if (filled) Modifier else Modifier.border(1.dp, SpyColors.Border, shape),
                        ),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            stageLabels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = SpyColors.IvoryMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun AccessLevelBar(
    current: AccessLevel,
    onDeepCheckSetup: () -> Unit,
    onRequestRoot: () -> Unit,
    accessHint: String,
    modifier: Modifier = Modifier,
) {
    val stages = listOf(
        AccessLevel.APP to stringResource(R.string.t_002),
        AccessLevel.SHELL to "Shizuku",
        AccessLevel.ROOT to "Root",
    )
    Column(modifier) {
        SectionWithHint(label = stringResource(R.string.t_003), hint = accessHint) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(40.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            stages.forEachIndexed { index, (level, label) ->
                val filled = current.ordinal >= level.ordinal
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    stages.lastIndex -> RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                    else -> RoundedCornerShape(0.dp)
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = shape,
                    color = if (filled) SpyColors.Amber else SpyColors.SurfaceRaised,
                    border = BorderStroke(1.dp, if (filled) SpyColors.Amber else SpyColors.Border),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (filled) SpyColors.Ink else SpyColors.IvoryMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        when (current) {
            AccessLevel.APP -> {
                Text(
                    stringResource(R.string.t_009) +
                        stringResource(R.string.t_010),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                SpyButton(
                    text = stringResource(R.string.t_004),
                    onClick = onDeepCheckSetup,
                    accent = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.t_005),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.IvoryMuted,
                )
                SpyButton(
                    text = stringResource(R.string.t_006),
                    onClick = onRequestRoot,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AccessLevel.SHELL -> {
                Text(
                    stringResource(R.string.t_007),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.Clear,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.t_005),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.IvoryMuted,
                )
                SpyButton(text = stringResource(R.string.t_006), onClick = onRequestRoot, modifier = Modifier.fillMaxWidth())
            }
            AccessLevel.ROOT -> {
                Text(
                    stringResource(R.string.t_008),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.Clear,
                )
            }
        }
        }
    }
}

@Composable
fun ScanStepRow(label: String, done: Boolean, active: Boolean) {
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
            .defaultMinSize(minHeight = 48.dp)
            .semantics { contentDescription = description }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (done) Icons.Default.Check else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = when {
                done -> SpyColors.Clear
                active -> SpyColors.Amber
                else -> SpyColors.Border
            },
            modifier = Modifier.size(22.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                done -> SpyColors.IvoryMuted
                active -> SpyColors.Ivory
                else -> SpyColors.IvoryMuted.copy(alpha = 0.7f)
            },
        )
    }
}

@Composable
fun ThinProgressLine(fraction: Float, modifier: Modifier = Modifier) {
    val f = fraction.coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp),
    ) {
        drawRect(color = SpyColors.Border, size = size)
        if (f > 0f) {
            drawRect(color = SpyColors.Amber, size = size.copy(width = size.width * f))
        }
    }
}
