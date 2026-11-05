package de.belkis.spyhound.ui

import de.belkis.spyhound.ioc.FeedSource
import java.util.Locale

/** One statement has to fit a single line on a 360 dp phone; longer ones use their short variant or are left out. */
const val StatementMaxLength = 44

/** The research sources worth naming, in display order, each with the feed that proves it is active. */
private val namedSources: List<Pair<String, (FeedSource) -> Boolean>> = listOf(
    "Amnesty" to { it.repo.startsWith("AmnestyTech/") },
    "Citizen Lab" to { it.repo.startsWith("citizenlab/") },
    "MVT" to { it.repo.startsWith("mvt-project/") },
    "abuse.ch" to { it.id.startsWith("malwarebazaar") },
)

/** Names of the named sources that have at least one usable feed on this device. */
fun activeSourceNames(active: List<FeedSource>): List<String> =
    namedSources.filter { (_, matches) -> active.any(matches) }.map { it.first }

/**
 * The rotating start screen statements. [texts] is the `start_statements` array: offline, stays on device,
 * count, sources, short sources, open source. Numbers come from the loaded state, so a statement about
 * indicators or sources only appears when there is something true to say.
 */
fun buildStartStatements(
    texts: List<String>,
    indicatorCount: Int,
    activeSources: Int,
    sourceNames: List<String>,
    locale: Locale,
    maxLength: Int = StatementMaxLength,
): List<String> {
    require(texts.size == 6) { "start_statements needs 6 items" }
    val count = if (indicatorCount > 0 && activeSources > 0) {
        String.format(locale, texts[2], formatCount(indicatorCount, locale), activeSources)
    } else null
    val names = sourceNames.joinToString(", ")
    val sources = if (names.isEmpty()) null else {
        String.format(locale, texts[3], names).takeIf { it.length <= maxLength }
            ?: String.format(locale, texts[4], names)
    }
    return listOfNotNull(texts[0], texts[1], count, sources, texts[5]).filter { it.length <= maxLength }
}
