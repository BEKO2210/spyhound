package de.belkis.spyhound

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.FeedSource
import de.belkis.spyhound.ioc.IocStatus
import de.belkis.spyhound.scan.GapFix
import de.belkis.spyhound.scan.ScanEngine
import de.belkis.spyhound.scan.TextsDe
import de.belkis.spyhound.scan.TextsEn
import kotlin.test.assertEquals
import org.junit.Test

class IndicatorSourceGapTest {
    @Test
    fun outdatedGapUsesSingularAndPluralInGermanAndEnglish() {
        assertGap(
            outdated = listOf(source("one", "Quelle Eins")),
            german = "1 Indikator-Quelle ist veraltet und wurde nicht benutzt (Quelle Eins).",
            english = "1 indicator source is outdated and was not used (Quelle Eins).",
        )
        assertGap(
            outdated = listOf(source("one", "Quelle Eins"), source("two", "Quelle Zwei")),
            german = "2 Indikator-Quellen sind veraltet und wurden nicht benutzt (Quelle Eins, Quelle Zwei).",
            english = "2 indicator sources are outdated and were not used (Quelle Eins, Quelle Zwei).",
        )
    }

    @Test
    fun corruptGapUsesSingularAndPluralInGermanAndEnglish() {
        assertGap(
            corrupt = listOf(source("one", "Quelle Eins")),
            german = "1 Indikator-Quelle ist beschädigt und wurde nicht benutzt (Quelle Eins).",
            english = "1 indicator source is damaged and was not used (Quelle Eins).",
        )
        assertGap(
            corrupt = listOf(source("one", "Quelle Eins"), source("two", "Quelle Zwei")),
            german = "2 Indikator-Quellen sind beschädigt und wurden nicht benutzt (Quelle Eins, Quelle Zwei).",
            english = "2 indicator sources are damaged and were not used (Quelle Eins, Quelle Zwei).",
        )
    }

    private fun assertGap(
        outdated: List<FeedSource> = emptyList(),
        corrupt: List<FeedSource> = emptyList(),
        german: String,
        english: String,
    ) {
        val status = IocStatus(
            sources = outdated.size + corrupt.size,
            available = 0,
            missing = emptyList(),
            corrupt = corrupt,
            outdated = outdated,
        )

        val gap = ScanEngine.indicatorSourceGaps(status).single()

        assertEquals(status.sources, status.available + status.unavailable)
        assertEquals(GapFix.UPDATE_INDICATORS, gap.fix)
        assertEquals(german, TextsDe.text(gap.detail))
        assertEquals(english, TextsEn.text(gap.detail))
    }

    private fun source(id: String, name: String) = FeedSource(
        id = id,
        name = name,
        repo = "example/repo",
        commit = "a".repeat(40),
        path = "$id.txt",
        format = "domains-txt",
        cite = "",
        category = Category.MERCENARY,
        family = "Test",
        sha256 = "0".repeat(64),
        bundled = false,
        androidArtifacts = true,
    )
}
