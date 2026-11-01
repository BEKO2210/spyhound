package de.belkis.spyhound.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.ui.SpyColors

@Composable
fun SectionWithHint(
    label: String,
    hint: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var showHint by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(220)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(label, modifier = Modifier.weight(1f))
            SectionHintButton(expanded = showHint, onToggle = { showHint = !showHint })
        }
        AnimatedVisibility(showHint) {
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium,
                color = SpyColors.IvoryMuted,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        content()
    }
}
