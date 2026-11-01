package de.belkis.spyhound.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.SpyTypographyExtras
import de.belkis.spyhound.ui.design.SpyRadii

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

enum class StatusTone { GOOD, OFF, ATTENTION }

data class StatusItem(
    val symbol: String,
    val label: String,
    val explanation: String,
    val tone: StatusTone,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null,
)

@Composable
fun StatusChip(item: StatusItem, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val tint = when (item.tone) {
        StatusTone.GOOD -> SpyColors.Clear
        StatusTone.OFF -> SpyColors.IvoryMuted
        StatusTone.ATTENTION -> SpyColors.Amber
    }
    Surface(
        onClick = { open = true },
        modifier = modifier.defaultMinSize(minHeight = 48.dp).semantics { contentDescription = "${item.label}. ${item.explanation}" },
        shape = RoundedCornerShape(SpyRadii.Pill),
        color = SpyColors.Surface,
        border = BorderStroke(1.dp, SpyColors.Border),
    ) {
        Row(Modifier.padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.symbol, color = tint, style = MaterialTheme.typography.labelLarge)
            Text(item.label, color = SpyColors.Ivory, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(item.label) },
        text = { Text(item.explanation) },
        confirmButton = {
            if (item.action != null && item.actionLabel != null) SpyButton(item.actionLabel, onClick = { open = false; item.action.invoke() }, accent = true, fillWidth = false)
        },
        dismissButton = { TextActionLink(text = androidx.compose.ui.res.stringResource(de.belkis.spyhound.R.string.t_060), onClick = { open = false }) },
        containerColor = SpyColors.SurfaceRaised,
        shape = RoundedCornerShape(SpyRadii.Card),
        tonalElevation = 0.dp,
        titleContentColor = SpyColors.Ivory,
        textContentColor = SpyColors.Ivory,
    )
}

@Composable
fun StatusStrip(items: List<StatusItem>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items.take(3).forEach { StatusChip(it, Modifier.weight(1f)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items.drop(3).take(2).forEach { StatusChip(it, Modifier.weight(1f)) }
        }
    }
}

@Composable
fun SectionHeader(label: String, hint: String, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label.uppercase(), style = SpyTypographyExtras.sectionLabel, modifier = Modifier.weight(1f))
        Text("?", color = SpyColors.Amber, modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).clickable { open = true }.semantics { contentDescription = "$label: $hint" }.wrapContentHeight(Alignment.CenterVertically), style = MaterialTheme.typography.titleMedium)
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
