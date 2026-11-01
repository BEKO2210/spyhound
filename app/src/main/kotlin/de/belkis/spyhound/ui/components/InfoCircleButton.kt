package de.belkis.spyhound.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.R
import de.belkis.spyhound.ui.SpyColors

@Composable
fun InfoCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cd = stringResource(R.string.cd_info_button)
    Surface(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { contentDescription = cd },
        shape = CircleShape,
        color = SpyColors.SurfaceRaised,
        border = BorderStroke(1.dp, SpyColors.Border),
    ) {
        Box(
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = SpyColors.Amber,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun SectionHintButton(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.section_hint_open)
    Surface(
        shape = CircleShape,
        color = SpyColors.SurfaceRaised,
        border = BorderStroke(1.dp, SpyColors.Border),
        modifier = modifier
            .defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            )
            .semantics { contentDescription = label },
    ) {
        Box(
            Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Text(
                "?",
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                color = SpyColors.Amber,
            )
        }
    }
}
