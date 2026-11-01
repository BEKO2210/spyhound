package de.belkis.spyhound.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.R
import de.belkis.spyhound.dns.DnsHit
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.UiState
import de.belkis.spyhound.ui.components.AccessLevelBar
import de.belkis.spyhound.ui.components.RadarScanButton
import de.belkis.spyhound.ui.components.SectionLabel
import de.belkis.spyhound.ui.components.SpyButton
import de.belkis.spyhound.ui.components.SpyDivider
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartScreen(
    state: UiState,
    onScan: () -> Unit,
    onUpdateIndicators: () -> Unit,
    onConnectShizuku: () -> Unit,
    onRequestRoot: () -> Unit,
    onRequestSms: () -> Unit,
    onRequestFiles: () -> Unit,
    onDnsToggle: (Boolean) -> Unit,
    onDemoReport: () -> Unit = {},
) {
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onDemoReport,
                    )
                    .padding(vertical = 4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.hound_mark),
                    contentDescription = null,
                    tint = SpyColors.Amber,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    "Spyhound",
                    style = MaterialTheme.typography.labelLarge,
                    color = SpyColors.IvoryMuted,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            RadarScanButton(
                onClick = onScan,
                enabled = state.iocs != null && state.progress == null,
            )
            Spacer(Modifier.height(12.dp))
            val status = buildString {
                state.iocs?.let { append("${it.set.size} Indikatoren") }
                    ?: append("Indikatoren werden geladen …")
                append(" · Stufe ")
                append(
                    when (state.access) {
                        de.belkis.spyhound.scan.AccessLevel.APP -> "Basis"
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
        }
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Spuren bekannter Spyware und Stalkerware — offline, ohne Telemetrie.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            state.iocs?.let { loaded ->
                SectionLabel("Indikatoren")
                Text(
                    "${loaded.set.size} Einträge aus ${loaded.status.available} von ${loaded.status.sources} Quellen",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (loaded.status.missing.isNotEmpty()) {
                    Text(
                        "${loaded.status.missing.size} Quellen fehlen — optional nachladen.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(8.dp))
                SpyButton(
                    text = if (state.updating != null) state.updating else "Weitere Quellen laden",
                    onClick = onUpdateIndicators,
                    enabled = state.updating == null,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.updateMessage?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = SpyColors.Clear)
                }
                SpyDivider(Modifier.padding(vertical = 16.dp))
            }
            SectionLabel("DNS-Wächter")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        "Warnt bei DNS-Anfragen zu bekannten Indikator-Domains.",
                        style = MaterialTheme.typography.bodyMedium,
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
                Text("Letzte Treffer", style = MaterialTheme.typography.labelLarge)
                val fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                state.dnsHits.take(5).forEach { hit -> DnsHitLine(hit, fmt) }
            }
            SpyDivider(Modifier.padding(vertical = 16.dp))
            AccessLevelBar(
                current = state.access,
                shizuku = state.shizuku,
                onConnectShizuku = onConnectShizuku,
                onRequestRoot = onRequestRoot,
            )
            if (state.rootDenied) {
                Spacer(Modifier.height(6.dp))
                Text("Root wurde abgelehnt oder ist nicht verfügbar.", style = MaterialTheme.typography.bodyMedium)
            }
            SpyDivider(Modifier.padding(vertical = 16.dp))
            SectionLabel("Berechtigungen")
            PermissionRow("SMS lesen", state.smsGranted, "Links in Nachrichten prüfen", onRequestSms)
            SpyDivider()
            PermissionRow("Alle Dateien", state.filesGranted, "Verdächtige Dateien auf dem Speicher", onRequestFiles)
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, hint: String, onRequest: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (granted) SpyColors.Clear else SpyColors.Ivory)
            Text(hint, style = MaterialTheme.typography.bodyMedium)
        }
        if (!granted) {
            SpyButton(text = "Erlauben", onClick = onRequest)
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
