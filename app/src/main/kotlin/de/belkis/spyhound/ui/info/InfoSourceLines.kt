package de.belkis.spyhound.ui.info

import androidx.annotation.StringRes
import de.belkis.spyhound.R
import de.belkis.spyhound.ioc.FeedSource

enum class SourceLicenseKind {
    MIT,
    CC_BY_4,
    BSD_2,
    DEVICE_ONLY,
    ABUSECH_TERMS,
}

const val MALWAREBAZAAR_REPO = "BEKO2210/spyhound"

@StringRes
fun sourceGroupTitleRes(repo: String): Int? = when (repo) {
    "mvt-project/mvt-indicators" -> R.string.info_repo_mvt
    "AssoEchap/stalkerware-indicators" -> R.string.info_repo_echap
    "eset/malware-ioc" -> R.string.info_repo_eset
    "meta/threat-research" -> R.string.info_repo_meta
    "AmnestyTech/investigations" -> R.string.info_repo_amnesty
    "citizenlab/malware-indicators" -> R.string.info_repo_citizenlab
    MALWAREBAZAAR_REPO -> R.string.info_repo_malwarebazaar
    else -> null
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
        MALWAREBAZAAR_REPO,
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
                    MALWAREBAZAAR_REPO -> SourceLicenseKind.ABUSECH_TERMS
                    else -> SourceLicenseKind.DEVICE_ONLY
                },
                bundled = entries.any { it.bundled },
                cites = entries.map { it.cite.trim() }.filter { it.isNotEmpty() }.distinct().sorted(),
                feedNames = entries.map { it.name }.distinct().sorted(),
            )
        }
        .sortedBy { order.indexOf(it.repo).let { i -> if (i < 0) order.size else i } }
}
