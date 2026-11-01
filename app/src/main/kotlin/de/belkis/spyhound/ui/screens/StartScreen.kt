package de.belkis.spyhound.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.R
import androidx.compose.ui.res.stringResource
import de.belkis.spyhound.dns.DnsHit
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.UiState
import de.belkis.spyhound.ui.components.AccessLevelBar
import de.belkis.spyhound.ui.components.InfoCircleButton
import de.belkis.spyhound.ui.components.RadarScanButton
import de.belkis.spyhound.ui.components.SectionLabel
import de.belkis.spyhound.ui.components.SectionWithHint
import de.belkis.spyhound.ui.components.SpyButton
import de.belkis.spyhound.ui.components.SpyDivider
import de.belkis.spyhound.ui.components.TextActionLink
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartScreen(
    state: UiState,
    onScan: () -> Unit,
    onUpdateIndicators: () -> Unit,
    onRequestRoot: () -> Unit,
    onRequestSms: () -> Unit,
    onRequestFiles: () -> Unit,
    onDnsToggle: (Boolean) -> Unit,
    onPickBugreport: () -> Unit,
    onDismissBanner: () -> Unit,
    onDemoReport: () -> Unit = {},
    onDeepCheckSetup: () -> Unit,
    onOpenInfo: () -> Unit,
) {
    var showBugreportSteps by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = onDemoReport,
                        )
                        .padding(vertical = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.hound_mark),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        "Spyhound",
                        style = MaterialTheme.typography.labelLarge,
                        color = SpyColors.IvoryMuted,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                de.belkis.spyhound.ui.components.LanguageToggle(
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                InfoCircleButton(
                    onClick = onOpenInfo,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
            Spacer(Modifier.height(12.dp))
            RadarScanButton(
                onClick = onScan,
                enabled = state.iocs != null && state.progress == null,
                lastRadar = state.lastRadar,
            )
            Spacer(Modifier.height(12.dp))
            val status = buildString {
                state.iocs?.let { append(stringResource(R.string.status_indicators, it.set.size)) }
                    ?: append(stringResource(R.string.t_059))
                append(" · ")
                append(stringResource(R.string.access_level_short))
                append(" ")
                append(
                    when (state.access) {
                        de.belkis.spyhound.scan.AccessLevel.APP -> stringResource(R.string.t_058)
                        de.belkis.spyhound.scan.AccessLevel.SHELL -> "Shizuku"
                        de.belkis.spyhound.scan.AccessLevel.ROOT -> "Root"
                    },
                )
            }
            Text(status, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            state.busy?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = SpyColors.Amber)
            }
            state.bannerMessage?.let { msg ->
                Spacer(Modifier.height(6.dp))
                Text(msg, style = MaterialTheme.typography.bodyMedium, color = SpyColors.Danger, textAlign = TextAlign.Center)
                TextActionLink(text = stringResource(R.string.t_060), onClick = onDismissBanner, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(R.string.t_061),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            state.iocs?.let { loaded ->
                SectionWithHint(stringResource(R.string.t_062), stringResource(R.string.start_help_indicators)) {
                    Text(
                        stringResource(R.string.indicators_summary, loaded.set.size, loaded.status.available, loaded.status.sources),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (loaded.status.missing.isNotEmpty()) {
                        Text(
                            stringResource(R.string.sources_missing, loaded.status.missing.size),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    SpyButton(
                        text = if (state.updating != null) state.updating else stringResource(R.string.t_063),
                        onClick = onUpdateIndicators,
                        enabled = state.updating == null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.updateMessage?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = SpyColors.Clear)
                    }
                }
                SpyDivider(Modifier.padding(vertical = 16.dp))
            }
            SectionWithHint(stringResource(R.string.t_064), stringResource(R.string.start_help_dns)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .widthIn(min = 0.dp)
                        .padding(end = 12.dp),
                ) {
                    Text(
                        stringResource(R.string.t_065),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Switch(
                    checked = state.dnsRunning,
                    onCheckedChange = onDnsToggle,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SpyColors.Ink,
                        checkedTrackColor = SpyColors.Amber,
                        uncheckedThumbColor = SpyColors.IvoryMuted,
                        uncheckedTrackColor = SpyColors.Border,
                    ),
                )
            }
            if (state.dnsHits.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.t_066), style = MaterialTheme.typography.labelLarge)
                val fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                state.dnsHits.take(5).forEach { hit -> DnsHitLine(hit, fmt) }
            }
            }
            SpyDivider(Modifier.padding(vertical = 16.dp))
            SectionWithHint(stringResource(R.string.t_067), stringResource(R.string.start_help_bugreport)) {
                Text(
                    stringResource(R.string.t_068),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                SpyButton(
                    text = stringResource(R.string.t_069),
                    onClick = onPickBugreport,
                    enabled = state.iocs != null && state.busy == null,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextActionLink(
                    text = if (showBugreportSteps) stringResource(R.string.t_070) else stringResource(R.string.t_071),
                    onClick = { showBugreportSteps = !showBugreportSteps },
                )
                AnimatedVisibility(showBugreportSteps) {
                    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(R.string.t_072),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            stringResource(R.string.t_073),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(stringResource(R.string.t_074), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.t_075) +
                                stringResource(R.string.t_076),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            SpyDivider(Modifier.padding(vertical = 16.dp))
            AccessLevelBar(
                current = state.access,
                onDeepCheckSetup = onDeepCheckSetup,
                onRequestRoot = onRequestRoot,
                accessHint = stringResource(R.string.start_help_access),
            )
            if (state.rootDenied) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.t_077), style = MaterialTheme.typography.bodyMedium)
            }
            SpyDivider(Modifier.padding(vertical = 16.dp))
            SectionLabel(stringResource(R.string.t_078))
            PermissionRow(stringResource(R.string.t_079), state.smsGranted, stringResource(R.string.t_080), onRequestSms)
            SpyDivider()
            PermissionRow(stringResource(R.string.t_081), state.filesGranted, stringResource(R.string.t_082), onRequestFiles)
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, hint: String, onRequest: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .widthIn(min = 0.dp)
                .padding(end = 8.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (granted) SpyColors.Clear else SpyColors.Ivory,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(hint, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
        }
        if (!granted) {
            SpyButton(text = stringResource(R.string.t_083), onClick = onRequest, fillWidth = false)
        } else {
            Text("OK", style = MaterialTheme.typography.labelLarge, color = SpyColors.Clear, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun DnsHitLine(hit: DnsHit, fmt: DateFormat) {
    Text(
        "${fmt.format(Date(hit.atMillis))} · ${hit.domain}",
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = SpyColors.Danger,
    )
}

