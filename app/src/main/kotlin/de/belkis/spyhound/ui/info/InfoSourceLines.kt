package de.belkis.spyhound.ui.info

import de.belkis.spyhound.ioc.FeedSource

enum class SourceLicenseKind {
    MIT,
    CC_BY_4,
    BSD_2,
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
        "eset/malware-ioc",
        "meta/threat-research",
        "AmnestyTech/investigations",
        "citizenlab/malware-indicators",
    )
    return sources
        .groupBy { it.repo }
        .map { (repo, entries) ->
            InfoSourceGroup(
                repo = repo,
                license = when (repo) {
                    "mvt-project/mvt-indicators", "meta/threat-research" -> SourceLicenseKind.MIT
                    "AssoEchap/stalkerware-indicators" -> SourceLicenseKind.CC_BY_4
                    "eset/malware-ioc" -> SourceLicenseKind.BSD_2
                    else -> SourceLicenseKind.DEVICE_ONLY
                },
                bundled = entries.any { it.bundled },
                cites = entries.map { it.cite.trim() }.filter { it.isNotEmpty() }.distinct().sorted(),
                feedNames = entries.map { it.name }.distinct().sorted(),
            )
        }
        .sortedBy { order.indexOf(it.repo).let { i -> if (i < 0) order.size else i } }
}
