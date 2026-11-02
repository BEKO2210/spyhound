package de.belkis.spyhound.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import de.belkis.spyhound.AppLanguage
import de.belkis.spyhound.ui.SpyColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import de.belkis.spyhound.R

@Composable
fun LanguagePicker(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val current = AppLanguage.selected(context).substringBefore(',').substringBefore('-')
    val label = when (current) {
        "de" -> stringResource(R.string.language_german)
        "en" -> stringResource(R.string.language_english)
        else -> stringResource(R.string.language_system)
    }
    Box(modifier) {
        TextActionLink(text = stringResource(R.string.language_label, label), onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(
                "" to R.string.language_system,
                "de" to R.string.language_german,
                "en" to R.string.language_english,
            ).forEach { (tag, title) ->
                DropdownMenuItem(
                    text = { Text(stringResource(title)) },
                    onClick = {
                        open = false
                        AppLanguage.select(context, tag)
                    },
                )
            }
        }
    }
}

/** Always-visible DE | EN switch for the start screen header: one tap, no menu to find. */
@Composable
fun LanguageToggle(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val chosen = AppLanguage.selected(context).substringBefore(',').substringBefore('-')
    val effective = chosen.ifEmpty { context.resources.configuration.locales[0].language }
    val isGerman = effective == "de"
    val currentName = stringResource(if (isGerman) R.string.language_german else R.string.language_english)
    val toggleCd = stringResource(R.string.cd_language_toggle, currentName)
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .height(48.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .clickable(onClickLabel = toggleCd) {
                AppLanguage.select(context, if (isGerman) "en" else "de")
            }
            .semantics(mergeDescendants = true) { contentDescription = toggleCd }
            .padding(horizontal = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text("DE", style = MaterialTheme.typography.labelLarge, color = if (isGerman) SpyColors.Amber else SpyColors.IvoryMuted)
        Text("  |  ", style = MaterialTheme.typography.labelLarge, color = SpyColors.Border)
        Text("EN", style = MaterialTheme.typography.labelLarge, color = if (!isGerman) SpyColors.Amber else SpyColors.IvoryMuted)
    }
}
