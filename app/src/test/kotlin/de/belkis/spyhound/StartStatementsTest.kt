package de.belkis.spyhound

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.FeedSource
import de.belkis.spyhound.ui.activeSourceNames
import de.belkis.spyhound.ui.buildStartStatements
import java.util.Locale
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StartStatementsTest {
    private val texts = listOf(
        "Offline-Analyse auf bekannte Spyware",
        "Scan-Ergebnisse bleiben auf deinem Gerät",
        "%1\$s Spuren aus %2\$d Quellen",
        "Erkennung nach %1\$s",
        "Quellen: %1\$s",
        "Quelloffen und reproduzierbar gebaut",
    )

    private fun feed(id: String, repo: String) = FeedSource(
        id, id, repo, "c", "p", "stix2", "", Category.MERCENARY, "", "", bundled = true, androidArtifacts = false,
    )

    @Test
    fun formatsNumbersWithTheLocaleSeparator() {
        val de = buildStartStatements(texts, 4200, 29, emptyList(), Locale.GERMAN)
        assertTrue("4.200 Spuren aus 29 Quellen" in de)
        val en = buildStartStatements(texts, 4200, 29, emptyList(), Locale.ENGLISH)
        assertTrue("4,200 Spuren aus 29 Quellen" in en)
    }

    @Test
    fun omitsStatementsWithoutAnythingTrueToSay() {
        val list = buildStartStatements(texts, 0, 0, emptyList(), Locale.GERMAN)
        assertEquals(listOf(texts[0], texts[1], texts[5]), list)
    }

    @Test
    fun inactiveSourcesAreNotNamed() {
        val names = activeSourceNames(listOf(feed("a", "AmnestyTech/investigations"), feed("m", "mvt-project/mvt-indicators")))
        assertEquals(listOf("Amnesty", "MVT"), names)
        assertTrue("Erkennung nach Amnesty, MVT" in buildStartStatements(texts, 1, 1, names, Locale.GERMAN))
        assertEquals(emptyList(), activeSourceNames(emptyList()))
    }

    @Test
    fun longSourceListUsesShortVariantAndNothingExceedsTheLimit() {
        val all = activeSourceNames(
            listOf(
                feed("a", "AmnestyTech/x"), feed("c", "citizenlab/x"), feed("m", "mvt-project/x"), feed("malwarebazaar-android", "BEKO2210/spyhound"),
            ),
        )
        assertEquals(listOf("Amnesty", "Citizen Lab", "MVT", "abuse.ch"), all)
        val list = buildStartStatements(texts, 123_456, 45, all, Locale.GERMAN)
        assertTrue("Quellen: Amnesty, Citizen Lab, MVT, abuse.ch" in list)
        assertTrue(list.all { it.length <= 44 })
        val tight = buildStartStatements(texts, 1, 1, all, Locale.GERMAN, maxLength = 40)
        assertTrue(tight.none { "Amnesty" in it })
    }
}
