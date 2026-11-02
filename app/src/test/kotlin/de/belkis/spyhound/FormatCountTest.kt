package de.belkis.spyhound

import de.belkis.spyhound.ui.formatCount
import java.util.Locale
import org.junit.Test
import kotlin.test.assertEquals

class FormatCountTest {
    @Test
    fun usesGivenLocaleNotSystemDefault() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("4.200", formatCount(4200, Locale.GERMAN))
            Locale.setDefault(Locale.GERMANY)
            assertEquals("4,200", formatCount(4200, Locale.ENGLISH))
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun smallNumbersStayPlain() {
        assertEquals("142", formatCount(142, Locale.GERMAN))
        assertEquals("0", formatCount(0, Locale.ENGLISH))
    }
}
