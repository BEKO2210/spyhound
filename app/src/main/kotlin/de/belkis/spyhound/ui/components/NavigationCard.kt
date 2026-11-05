package de.belkis.spyhound.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.R
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.SpyTypographyExtras
import de.belkis.spyhound.ui.design.SpySpacing

/**
 * Card that leads to another screen: icon badge, title and text, trailing chevron.
 * The whole card is the touch target; [onClickLabel] is announced for screen readers.
 */
@Composable
fun NavigationCard(
    iconRes: Int,
    title: String,
    text: String,
    onClickLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (pressed) SpyColors.SurfaceRaised else SpyColors.Surface)
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
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                role = Role.Button,
                onClickLabel = onClickLabel,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) { contentDescription = "$title. $text" }
            .defaultMinSize(minHeight = 72.dp)
            .padding(SpySpacing.PhoneEdge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardRowIconBadge(iconRes = iconRes, active = false)
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 12.dp)) {
            Text(title.uppercase(), style = SpyTypographyExtras.sectionLabel)
            Spacer(Modifier.height(SpySpacing.Unit))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = SpyColors.IvoryMuted)
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = SpyColors.IvoryMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}
