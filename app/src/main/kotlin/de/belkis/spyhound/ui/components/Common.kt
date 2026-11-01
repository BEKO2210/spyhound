package de.belkis.spyhound.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.ui.SpyColors

@Composable
fun SpyCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SpyColors.Surface,
        border = BorderStroke(1.dp, SpyColors.Border),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        content = { Column(Modifier.padding(16.dp), content = content) },
    )
}

@Composable
fun SpyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    enabled: Boolean = true,
) {
    val bg = if (accent) SpyColors.Amber else SpyColors.SurfaceRaised
    val fg = if (accent) SpyColors.Ink else SpyColors.Ivory
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .semantics { contentDescription = text },
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(1.dp, if (accent) SpyColors.Amber else SpyColors.Border),
        contentColor = fg,
    ) {
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

@Composable
fun AccessLevelBar(current: AccessLevel, modifier: Modifier = Modifier) {
    val stages = listOf(
        Triple(AccessLevel.APP, "Basis", "Apps, Gerät, mitgelieferte Indikatoren"),
        Triple(AccessLevel.SHELL, "Shizuku", "Prozesse, alle Pakete, Logs, Dateien"),
        Triple(AccessLevel.ROOT, "Root", "Volle Shell wie am PC, tiefste Spuren"),
    )
    Column(modifier) {
        Text(
            "Zugriffsstufe",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            color = SpyColors.Ivory,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
        stages.forEach { (level, title, hint) ->
            val active = current.ordinal >= level.ordinal
            val isCurrent = current == level
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isCurrent) SpyColors.SurfaceRaised else SpyColors.Surface,
                border = BorderStroke(
                    1.dp,
                    when {
                        isCurrent -> SpyColors.Amber
                        active -> SpyColors.Clear.copy(alpha = 0.5f)
                        else -> SpyColors.Border
                    },
                ),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "$title${if (isCurrent) " · aktiv" else if (active) " · freigeschaltet" else ""}",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        color = if (active) SpyColors.Ivory else SpyColors.IvoryMuted,
                    )
                    Text(hint, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
