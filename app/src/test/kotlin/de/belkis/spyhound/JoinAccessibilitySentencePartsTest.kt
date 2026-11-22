package de.belkis.spyhound

import de.belkis.spyhound.ui.joinAccessibilitySentenceParts
import org.junit.Test
import kotlin.test.assertEquals

class JoinAccessibilitySentencePartsTest {
    @Test
    fun smsGapCardWithoutDoubledPeriod() {
        val got = joinAccessibilitySentenceParts(
            listOf(
                "SMS-Links",
                "SMS wurden nicht geprüft: Die Berechtigung „SMS lesen“ fehlt.",
                "Aufklappen",
            ),
        )
        assertEquals(
            "SMS-Links. SMS wurden nicht geprüft: Die Berechtigung „SMS lesen“ fehlt. Aufklappen",
            got,
        )
    }

    @Test
    fun fileFingerprintGapCardWithoutDoubledPeriod() {
        val got = joinAccessibilitySentenceParts(
            listOf(
                "Datei-Fingerabdrücke",
                "2 Datei(en) oder Ordner waren nicht lesbar (z. B. Android/data).",
                "Aufklappen",
            ),
        )
        assertEquals(
            "Datei-Fingerabdrücke. 2 Datei(en) oder Ordner waren nicht lesbar (z. B. Android/data). Aufklappen",
            got,
        )
    }

    @Test
    fun dropsEmptyParts() {
        assertEquals(
            "A. B",
            joinAccessibilitySentenceParts(listOf("A", "", "  ", "B")),
        )
    }

    @Test
    fun questionMarkNeedsOnlySpaceBeforeNextPart() {
        assertEquals(
            "Bereich. Schon fertig? Nächster",
            joinAccessibilitySentenceParts(listOf("Bereich", "Schon fertig?", "Nächster")),
        )
    }

    @Test
    fun periodBeforeClosingQuoteNeedsOnlySpace() {
        assertEquals(
            "Kategorie. Text mit „Anführung“. Zustand",
            joinAccessibilitySentenceParts(listOf("Kategorie", "Text mit „Anführung“.", "Zustand")),
        )
        assertEquals(
            "Kategorie. Die Berechtigung „SMS lesen“ fehlt. Zustand",
            joinAccessibilitySentenceParts(listOf("Kategorie", "Die Berechtigung „SMS lesen“ fehlt.", "Zustand")),
        )
    }

    @Test
    fun singlePartTrimmed() {
        assertEquals("Nur einer", joinAccessibilitySentenceParts(listOf("  Nur einer  ")))
    }

    @Test
    fun emptyInput() {
        assertEquals("", joinAccessibilitySentenceParts(emptyList()))
        assertEquals("", joinAccessibilitySentenceParts(listOf("", "   ")))
    }
}