package de.belkis.spyhound.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.BuildConfig
import de.belkis.spyhound.R
import de.belkis.spyhound.ioc.FeedCatalog
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.components.LanguagePicker
import de.belkis.spyhound.ui.components.SectionLabel
import de.belkis.spyhound.ui.components.SpyButton
import de.belkis.spyhound.ui.components.SpyDivider
import de.belkis.spyhound.ui.components.SpyNavigateBack
import de.belkis.spyhound.ui.components.TextActionLink
import de.belkis.spyhound.ui.info.InfoSourceGroup
import de.belkis.spyhound.ui.info.SourceLicenseKind
import de.belkis.spyhound.ui.info.infoSourceGroups

@Composable
fun InfoScreen(
    onBack: () -> Unit,
    onReplayIntro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val sourceGroups = remember {
        val json = context.assets.open("iocs/sources.json").bufferedReader().use { it.readText() }
        infoSourceGroups(FeedCatalog.parse(json))
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll),
    ) {
        SpyNavigateBack(onClick = onBack)
        Text(
            stringResource(R.string.info_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        InfoSection(R.string.info_section_what, R.string.info_what_body)
        InfoSection(R.string.info_section_limits, R.string.info_limits_body)
        InfoSection(R.string.info_section_depths, R.string.info_depths_body)
        InfoSection(R.string.info_section_privacy, R.string.info_privacy_body)
        SpyDivider(Modifier.padding(vertical = 16.dp))
        SectionLabel(stringResource(R.string.info_section_sources))
        sourceGroups.forEach { group ->
            SourceGroupBlock(group)
            Spacer(Modifier.height(12.dp))
        }
        Text(
            stringResource(R.string.info_sources_notice),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        SpyDivider(Modifier.padding(vertical = 16.dp))
        SectionLabel(stringResource(R.string.info_section_help))
        Text(stringResource(R.string.info_help_body), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        HelpLink(stringResource(R.string.info_help_access_now), "https://www.accessnow.org/help/")
        HelpLink(stringResource(R.string.info_help_amnesty), "https://securitylab.amnesty.org/")
        SpyDivider(Modifier.padding(vertical = 16.dp))
        SectionLabel(stringResource(R.string.info_section_language))
        LanguagePicker()
        SpyDivider(Modifier.padding(vertical = 16.dp))
        Text(
            stringResource(R.string.info_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.info_license_apache), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.info_toolkit_origin), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        SpyButton(
            text = stringResource(R.string.info_replay_intro),
            onClick = onReplayIntro,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun InfoSection(title: Int, body: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
    Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))
}

@Composable
private fun SourceGroupBlock(group: InfoSourceGroup) {
    val repoTitle = when (group.repo) {
        "mvt-project/mvt-indicators" -> stringResource(R.string.info_repo_mvt)
        "AssoEchap/stalkerware-indicators" -> stringResource(R.string.info_repo_echap)
        "AmnestyTech/investigations" -> stringResource(R.string.info_repo_amnesty)
        "citizenlab/malware-indicators" -> stringResource(R.string.info_repo_citizenlab)
        else -> group.repo
    }
    val license = when (group.license) {
        SourceLicenseKind.MIT -> stringResource(R.string.source_license_mit)
        SourceLicenseKind.CC_BY_4 -> stringResource(R.string.source_license_cc_by)
        SourceLicenseKind.DEVICE_ONLY -> stringResource(R.string.source_license_device_only)
    }
    val bundled = if (group.bundled) {
        stringResource(R.string.info_sources_bundled)
    } else {
        stringResource(R.string.info_sources_on_device)
    }
    Text(repoTitle, style = MaterialTheme.typography.labelLarge, color = SpyColors.Ivory)
    Text(
        "$license · $bundled",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
    )
    if (group.cites.isNotEmpty()) {
        Text(
            stringResource(R.string.info_sources_cite, group.cites.joinToString("; ")),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HelpLink(label: String, url: String) {
    val context = LocalContext.current
    TextActionLink(
        text = label,
        onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
