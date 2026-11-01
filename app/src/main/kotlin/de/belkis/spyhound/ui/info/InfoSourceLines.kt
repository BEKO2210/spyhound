package de.belkis.spyhound.ui.info

import de.belkis.spyhound.ioc.FeedSource

enum class SourceLicenseKind {
    MIT,
    CC_BY_4,
    DEVICE_ONLY,
}

data class InfoSourceGroup(
    val repo: String,
    val license: SourceLicenseKind,
    val bundled: Boolean,
    val cites: List<String>,
    val feedNames: List<String>,
)

fun infoSourceGroups(sources: List<FeedSource>): List<InfoSourceGroup> {
    val order = listOf(
        "mvt-project/mvt-indicators",
        "AssoEchap/stalkerware-indicators",
        "AmnestyTech/investigations",
        "citizenlab/malware-indicators",
    )
    return sources
        .groupBy { it.repo }
        .map { (repo, entries) ->
            InfoSourceGroup(
                repo = repo,
                license = when (repo) {
                    "mvt-project/mvt-indicators" -> SourceLicenseKind.MIT
                    "AssoEchap/stalkerware-indicators" -> SourceLicenseKind.CC_BY_4
                    else -> SourceLicenseKind.DEVICE_ONLY
                },
                bundled = entries.any { it.bundled },
                cites = entries.map { it.cite.trim() }.filter { it.isNotEmpty() }.distinct().sorted(),
                feedNames = entries.map { it.name }.distinct().sorted(),
            )
        }
        .sortedBy { order.indexOf(it.repo).let { i -> if (i < 0) order.size else i } }
}
