package de.belkis.spyhound

import androidx.compose.ui.text.font.FontWeight
import de.belkis.spyhound.ui.components.emphasizeLabels
import org.junit.Test
import kotlin.test.assertEquals

class GuideTextTest {
    private fun bold(text: String) = emphasizeLabels(text).let { s ->
        s.spanStyles.filter { it.item.fontWeight == FontWeight.SemiBold }.map { s.text.substring(it.start, it.end) }
    }

    @Test
    fun germanAndEnglishLabelsAreEmphasized() {
        assertEquals(listOf("„Build-Nummer“", "„Softwareinformationen“"),
            bold("Tippe siebenmal auf „Build-Nummer“ (Samsung: unter „Softwareinformationen“)."))
        assertEquals(listOf("“Wireless debugging”"), bold("Switch on “Wireless debugging”. Wi-Fi must be on."))
    }

    @Test
    fun textStaysUnchangedAndPlainTextHasNoSpans() {
        val t = "Öffne Einstellungen → „Über das Telefon“."
        assertEquals(t, emphasizeLabels(t).text)
        assertEquals(emptyList(), bold("No labels here."))
    }
}
