package de.belkis.spyhound.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.ui.LocalMotionEnabled
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.SpyTypographyExtras

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
) {
    val bg = if (accent) SpyColors.Amber else SpyColors.SurfaceRaised
    val fg = if (accent) SpyColors.Ink else SpyColors.Ivory
    val widthMod = if (fillWidth) Modifier.fillMaxWidth() else Modifier.wrapContentWidth()
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(widthMod)
            .defaultMinSize(minHeight = 48.dp)
            .semantics { contentDescription = text },
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(1.dp, if (accent) SpyColors.Amber else SpyColors.Border),
        contentColor = fg,
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
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
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
    )
}

@Composable
fun WhyLink(expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Warum?", style = MaterialTheme.typography.bodyMedium, color = SpyColors.Amber)
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(220))
            .clickable { open = !open },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (count != null) "$title ($count)" else title,
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = SpyColors.SurfaceRaised,
        border = BorderStroke(1.dp, SpyColors.Border),
    ) {
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
    val motion = LocalMotionEnabled.current
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = if (motion) tween(700) else tween(0),
        label = "count",
    )
    Text(
        animated.toString(),
        style = style,
        modifier = modifier,
    )
}

@Composable
fun AccessLevelBar(
    current: AccessLevel,
    onDeepCheckSetup: () -> Unit,
    onRequestRoot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stages = listOf(
        AccessLevel.APP to "Basis",
        AccessLevel.SHELL to "Shizuku",
        AccessLevel.ROOT to "Root",
    )
    Column(modifier) {
        SectionLabel("Zugriffsstufe")
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
                    "Die tiefere Prüfung findet mehr — zum Beispiel laufende Hintergrundprozesse. " +
                        "Dafür richtest du einmal Shizuku ein, ohne Root.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                SpyButton(
                    text = "Tiefere Prüfung einrichten",
                    onClick = onDeepCheckSetup,
                    accent = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Root? Dann einfach ‚Root anfragen‘ tippen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.IvoryMuted,
                )
                SpyButton(
                    text = "Root anfragen",
                    onClick = onRequestRoot,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AccessLevel.SHELL -> {
                Text(
                    "Tiefere Prüfung ist aktiv. Root geht noch einen Schritt weiter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.Clear,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Root? Dann einfach ‚Root anfragen‘ tippen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.IvoryMuted,
                )
                SpyButton(text = "Root anfragen", onClick = onRequestRoot, modifier = Modifier.fillMaxWidth())
            }
            AccessLevel.ROOT -> {
                Text(
                    "Höchste Stufe aktiv — alle verfügbaren Prüfungen laufen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.Clear,
                )
            }
        }
    }
}

@Composable
fun ScanStepRow(label: String, done: Boolean, active: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
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
