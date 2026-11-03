package de.belkis.spyhound.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import de.belkis.spyhound.ui.design.SpyColors

/** German „…“ or English “…”: the words a guide names exactly as the phone shows them. */
private val Label = Regex("„[^“]+“|“[^”]+”")

/** Sets the quoted on-screen labels in a guide text in bold ivory, so the words to look for stand out. */
fun emphasizeLabels(text: String, color: Color = SpyColors.Ivory): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in Label.findAll(text)) {
        append(text, last, m.range.first)
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = color)) { append(m.value) }
        last = m.range.last + 1
    }
    append(text, last, text.length)
}
