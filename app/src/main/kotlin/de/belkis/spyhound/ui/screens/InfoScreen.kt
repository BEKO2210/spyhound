package de.belkis.spyhound.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.BuildConfig
import de.belkis.spyhound.R
import de.belkis.spyhound.ioc.FeedCatalog
import de.belkis.spyhound.ui.components.CollapsibleSection
import de.belkis.spyhound.ui.components.LanguagePicker
import de.belkis.spyhound.ui.components.ScrollEdge
import de.belkis.spyhound.ui.components.SectionHeader
import de.belkis.spyhound.ui.components.SpyButton
import de.belkis.spyhound.ui.components.SpyCard
import de.belkis.spyhound.ui.components.SpyDivider
import de.belkis.spyhound.ui.components.SpyNavigateBack
import de.belkis.spyhound.ui.components.SpySurface
import de.belkis.spyhound.ui.components.TextActionLink
import de.belkis.spyhound.ui.design.SpyColors
import de.belkis.spyhound.ui.design.SpyMotion
import de.belkis.spyhound.ui.design.SpySpacing
import de.belkis.spyhound.ui.design.rememberReducedMotion
import de.belkis.spyhound.ui.design.staggeredAppear
import de.belkis.spyhound.ui.info.InfoLicenceEntry
import de.belkis.spyhound.ui.info.LicenceBody
import de.belkis.spyhound.ui.info.InfoSourceGroup
import de.belkis.spyhound.ui.info.SourceLicenseKind
import de.belkis.spyhound.ui.info.infoLicenceEntries
import de.belkis.spyhound.ui.info.infoSourceGroups

@Composable
fun InfoScreen(
    onBack: () -> Unit,
    onReplayIntro: () -> Unit,
    modifier: Modifier = Modifier,
    displayVersion: String = BuildConfig.VERSION_NAME,
    sourcesSectionExpanded: Boolean = false,
    licencesSectionExpanded: Boolean = false,
    testInitialScrollPx: Int = 0,
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    LaunchedEffect(testInitialScrollPx) {
        if (testInitialScrollPx > 0) scroll.scrollTo(testInitialScrollPx)
    }
    val scrolled by remember { derivedStateOf { scroll.value > 0 } }
    val indicatorFeeds = remember {
        val json = context.assets.open("iocs/sources.json").bufferedReader().use { it.readText() }
        FeedCatalog.parse(json)
    }
    val sourceGroups = remember(indicatorFeeds) { infoSourceGroups(indicatorFeeds) }
    val sourceCount = indicatorFeeds.size
    val licenceEntries = remember(indicatorFeeds) { infoLicenceEntries(context, indicatorFeeds) }
    val licenceCount = licenceEntries.size

    Column(modifier = modifier.fillMaxSize()) {
        SpyNavigateBack(onClick = onBack)
        Text(
            stringResource(R.string.info_title),
            style = MaterialTheme.typography.headlineLarge,
            color = SpyColors.Ivory,
            modifier = Modifier.padding(bottom = SpySpacing.Unit * 2),
        )
        ScrollEdge(scrolled = scrolled)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(SpySpacing.Section),
        ) {
            WhatsNewSection(displayVersion = displayVersion, modifier = staggeredAppear(0))
            InfoSection(
                title = R.string.info_section_what,
                body = R.string.info_what_body,
                modifier = staggeredAppear(1),
            )
            InfoSection(
                title = R.string.info_section_limits,
                body = R.string.info_limits_body,
                modifier = staggeredAppear(2),
            )
            InfoSection(
                title = R.string.info_section_depths,
                body = R.string.info_depths_body,
                modifier = staggeredAppear(3),
            )
            InfoSection(
                title = R.string.info_section_privacy,
                body = R.string.info_privacy_body,
                modifier = staggeredAppear(4),
            )

            Column(modifier = Modifier.fillMaxWidth().then(staggeredAppear(5))) {
                SectionHeader(
                    label = stringResource(R.string.info_section_legal),
                    hint = stringResource(R.string.info_hint_legal),
                )
                SpySurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SpySpacing.Unit * 4, vertical = SpySpacing.Unit * 2),
                    ) {
                        LegalSection(R.string.legal_imprint_title, R.string.legal_imprint_body)
                        SpyDivider()
                        LegalSection(R.string.legal_privacy_title, R.string.legal_privacy_body)
                        SpyDivider()
                        LegalSection(R.string.legal_disclaimer_title, R.string.legal_disclaimer_body)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().then(staggeredAppear(6))) {
                SectionHeader(
                    label = stringResource(R.string.info_section_sources),
                    hint = stringResource(R.string.info_hint_sources),
                )
                InfoCollapsibleCard(
                    sectionLabel = stringResource(R.string.info_section_sources),
                    summary = pluralStringResource(R.plurals.info_sources_summary, sourceCount, sourceCount),
                    initiallyExpanded = sourcesSectionExpanded,
                    testTag = "info_sources_collapsible",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(SpySpacing.Unit * 3)) {
                        sourceGroups.forEach { group ->
                            SourceGroupBlock(group)
                        }
                        Text(
                            stringResource(R.string.info_sources_notice),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SpyColors.IvoryMuted,
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().then(staggeredAppear(7))) {
                SectionHeader(
                    label = stringResource(R.string.info_section_licences),
                    hint = stringResource(R.string.info_hint_licences),
                )
                InfoCollapsibleCard(
                    sectionLabel = stringResource(R.string.info_section_licences),
                    summary = pluralStringResource(R.plurals.info_licences_summary, licenceCount, licenceCount),
                    initiallyExpanded = licencesSectionExpanded,
                    testTag = "info_licences_collapsible",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(SpySpacing.Unit * 3)) {
                        licenceEntries.forEach { entry ->
                            LicenceNoticeBlock(entry)
                        }
                    }
                }
            }

            // Specialist help
            Column(modifier = Modifier.fillMaxWidth().then(staggeredAppear(8))) {
                SectionHeader(
                    label = stringResource(R.string.info_section_help),
                    hint = stringResource(R.string.info_hint_help),
                )
                SpySurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SpySpacing.Unit * 4),
                    ) {
                        Text(
                            stringResource(R.string.info_help_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SpyColors.IvoryMuted,
                        )
                        Spacer(Modifier.height(SpySpacing.Unit * 2))
                        HelpLink(stringResource(R.string.info_help_access_now), "https://www.accessnow.org/help/")
                        HelpLink(stringResource(R.string.info_help_amnesty), "https://securitylab.amnesty.org/")
                    }
                }
            }

            // 7+: Support, language, and app metadata (clamped at MaxStagger = 8)
            Column(
                modifier = Modifier.fillMaxWidth().then(staggeredAppear(8)),
                verticalArrangement = Arrangement.spacedBy(SpySpacing.Section),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        label = stringResource(R.string.info_section_support),
                        hint = stringResource(R.string.info_hint_support),
                    )
                    SpySurface(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(SpySpacing.Unit * 4),
                        ) {
                            Text(
                                stringResource(R.string.info_support_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SpyColors.IvoryMuted,
                            )
                            Spacer(Modifier.height(SpySpacing.Unit * 2))
                            HelpLink(stringResource(R.string.info_support_link), "https://github.com/sponsors/BEKO2210")
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        label = stringResource(R.string.info_section_language),
                        hint = stringResource(R.string.info_hint_language),
                    )
                    SpySurface(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SpySpacing.Unit * 4, vertical = SpySpacing.Unit * 2),
                        ) {
                            LanguagePicker()
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        label = stringResource(R.string.info_section_about),
                        hint = stringResource(R.string.info_hint_about),
                    )
                    SpySurface(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(SpySpacing.Unit * 4),
                            verticalArrangement = Arrangement.spacedBy(SpySpacing.Unit * 2),
                        ) {
                            Text(
                                stringResource(R.string.info_version, displayVersion),
                                style = MaterialTheme.typography.labelLarge,
                                color = SpyColors.Ivory,
                            )
                            Text(
                                stringResource(R.string.info_license_apache),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SpyColors.IvoryMuted,
                            )
                            Text(
                                stringResource(R.string.info_toolkit_origin),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SpyColors.IvoryMuted,
                            )
                        }
                    }
                }

                SpyButton(
                    text = stringResource(R.string.info_replay_intro),
                    onClick = onReplayIntro,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(SpySpacing.Unit * 3))
            }
        }
    }
}

@Composable
private fun LegalSection(title: Int, body: Int) {
    val context = LocalContext.current
    val email = stringResource(R.string.legal_email)
    CollapsibleSection(title = stringResource(title)) {
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = SpyColors.IvoryMuted,
        )
        TextActionLink(
            text = email,
            onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WhatsNewSection(displayVersion: String, modifier: Modifier = Modifier) {
    val items = stringArrayResource(R.array.info_whats_new_items)
    val version = displayVersion
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            label = stringResource(R.string.info_section_whats_new),
            hint = stringResource(R.string.info_hint_whats_new, version),
        )
        SpySurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SpySpacing.Unit * 4),
                verticalArrangement = Arrangement.spacedBy(SpySpacing.Unit * 2),
            ) {
                Text(
                    stringResource(R.string.info_version, version),
                    style = MaterialTheme.typography.labelLarge,
                    color = SpyColors.Ivory,
                )
                items.forEach { item ->
                    Text(
                        text = "\u2022 $item",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpyColors.IvoryMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoSection(title: Int, body: Int, modifier: Modifier = Modifier) {
    val label = stringResource(title)
    val text = stringResource(body)
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(label = label, hint = text)
        SpySurface(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = SpyColors.Ivory,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SpySpacing.Unit * 4),
            )
        }
    }
}

@Composable
private fun LicenceNoticeBlock(entry: InfoLicenceEntry) {
    SpySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpySpacing.Unit * 4),
        ) {
            Text(
                stringResource(entry.titleRes),
                style = MaterialTheme.typography.labelLarge,
                color = SpyColors.Ivory,
            )
            when (val body = entry.body) {
                is LicenceBody.Resource -> Text(
                    stringResource(body.res),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.IvoryMuted,
                    modifier = Modifier.padding(top = SpySpacing.Unit),
                )
                is LicenceBody.AssetText -> Text(
                    body.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.IvoryMuted,
                    modifier = Modifier.padding(top = SpySpacing.Unit),
                )
            }
        }
    }
}

@Composable
private fun SourceGroupBlock(group: InfoSourceGroup) {
    val repoTitle = when (group.repo) {
        "mvt-project/mvt-indicators" -> stringResource(R.string.info_repo_mvt)
        "AssoEchap/stalkerware-indicators" -> stringResource(R.string.info_repo_echap)
        "eset/malware-ioc" -> stringResource(R.string.info_repo_eset)
        "meta/threat-research" -> stringResource(R.string.info_repo_meta)
        "AmnestyTech/investigations" -> stringResource(R.string.info_repo_amnesty)
        "citizenlab/malware-indicators" -> stringResource(R.string.info_repo_citizenlab)
        else -> group.repo
    }
    val license = when (group.license) {
        SourceLicenseKind.MIT -> stringResource(R.string.source_license_mit)
        SourceLicenseKind.CC_BY_4 -> stringResource(R.string.source_license_cc_by)
        SourceLicenseKind.BSD_2 -> stringResource(R.string.source_license_bsd2)
        SourceLicenseKind.DEVICE_ONLY -> stringResource(R.string.source_license_device_only)
    }
    val bundled = if (group.bundled) {
        stringResource(R.string.info_sources_bundled)
    } else {
        stringResource(R.string.info_sources_on_device)
    }
    SpySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpySpacing.Unit * 4),
        ) {
            Text(
                repoTitle,
                style = MaterialTheme.typography.labelLarge,
                color = SpyColors.Ivory,
            )
            Text(
                "$license · $bundled",
                style = MaterialTheme.typography.bodyMedium,
                color = SpyColors.IvoryMuted,
                modifier = Modifier.padding(top = SpySpacing.Unit, bottom = SpySpacing.Unit),
            )
            if (group.cites.isNotEmpty()) {
                Text(
                    stringResource(R.string.info_sources_cite, group.cites.joinToString("; ")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.IvoryMuted,
                )
            }
            if (group.repo == "eset/malware-ioc" || group.repo == "meta/threat-research") {
                Text(
                    stringResource(R.string.info_malware_coverage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.IvoryMuted,
                    modifier = Modifier.padding(top = SpySpacing.Unit),
                )
            }
        }
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

@Composable
private fun InfoCollapsibleCard(
    sectionLabel: String,
    summary: String,
    initiallyExpanded: Boolean,
    modifier: Modifier = Modifier,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by remember(initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    val reduced = rememberReducedMotion()
    val stateLabel = stringResource(if (open) R.string.result_collapse else R.string.result_expand)
    val headerCd = "$sectionLabel. $summary. $stateLabel"
    val chevronRotation by animateFloatAsState(
        if (open) 180f else 0f,
        tween(if (reduced) 0 else SpyMotion.Element, easing = SpyMotion.Enter),
        label = "info section chevron",
    )
    SpyCard(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable(role = Role.Button) { open = !open }
                .semantics {
                    contentDescription = headerCd
                    stateDescription = stateLabel
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                summary,
                style = MaterialTheme.typography.bodyLarge,
                color = SpyColors.Ivory,
                modifier = Modifier
                    .weight(1f)
                    .testTag(testTag),
            )
            Spacer(Modifier.width(SpySpacing.Unit * 2))
            Icon(
                painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = SpyColors.Amber,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = chevronRotation },
            )
        }
        AnimatedVisibility(visible = open) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SpySpacing.Unit * 2),
                content = content,
            )
        }
    }
}
