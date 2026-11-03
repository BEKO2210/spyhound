package de.belkis.spyhound.ui.info

import android.content.Context
import de.belkis.spyhound.R
import de.belkis.spyhound.ioc.FeedCatalog
import de.belkis.spyhound.ioc.FeedSource

sealed class LicenceBody {
    data class Resource(val res: Int) : LicenceBody()
    data class AssetText(val text: String) : LicenceBody()
}

data class InfoLicenceEntry(
    val titleRes: Int,
    val body: LicenceBody,
)

fun infoLicenceEntries(context: Context, indicatorFeeds: List<FeedSource>): List<InfoLicenceEntry> {
    val groups = infoSourceGroups(indicatorFeeds)
    val domainJson = context.assets.open("iocs/domainlists.json").bufferedReader().use { it.readText() }
    val domainFeeds = FeedCatalog.parse(domainJson)
    val repoEntries = groups.map { group ->
        InfoLicenceEntry(
            titleRes = repoTitleRes(group.repo),
            body = LicenceBody.Resource(licenseBodyRes(group.license)),
        )
    }
    val domainEntries = domainFeeds.map { feed ->
        InfoLicenceEntry(
            titleRes = R.string.info_licence_title_domain,
            body = LicenceBody.Resource(R.string.info_licence_domain_body),
        )
    }
    val fontEntries = (context.assets.list("licenses") ?: emptyArray()).sorted().map { file ->
        val text = context.assets.open("licenses/$file").bufferedReader().use { it.readText() }
        InfoLicenceEntry(
            titleRes = when {
                file.contains("Inter", ignoreCase = true) -> R.string.info_licence_title_font_inter
                file.contains("SpaceGrotesk", ignoreCase = true) -> R.string.info_licence_title_font_space_grotesk
                else -> R.string.info_licence_title_font_other
            },
            body = LicenceBody.AssetText(text),
        )
    }
    val appEntry = InfoLicenceEntry(
        titleRes = R.string.info_licence_title_spyhound,
        body = LicenceBody.Resource(R.string.info_licence_spyhound_body),
    )
    return repoEntries + domainEntries + fontEntries + appEntry
}

private fun repoTitleRes(repo: String): Int = when (repo) {
    "mvt-project/mvt-indicators" -> R.string.info_repo_mvt
    "AssoEchap/stalkerware-indicators" -> R.string.info_repo_echap
    "eset/malware-ioc" -> R.string.info_repo_eset
    "meta/threat-research" -> R.string.info_repo_meta
    "AmnestyTech/investigations" -> R.string.info_repo_amnesty
    "citizenlab/malware-indicators" -> R.string.info_repo_citizenlab
    else -> R.string.info_licence_title_other
}

private fun licenseBodyRes(kind: SourceLicenseKind): Int = when (kind) {
    SourceLicenseKind.MIT -> R.string.info_licence_body_mit
    SourceLicenseKind.CC_BY_4 -> R.string.info_licence_body_cc_by
    SourceLicenseKind.BSD_2 -> R.string.info_licence_body_bsd2
    SourceLicenseKind.DEVICE_ONLY -> R.string.info_licence_body_device_only
}
