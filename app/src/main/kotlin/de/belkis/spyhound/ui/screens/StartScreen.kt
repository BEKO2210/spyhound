package de.belkis.spyhound.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.dns.DnsHit
import de.belkis.spyhound.shell.ShizukuShell
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.UiState
import de.belkis.spyhound.ui.components.AccessLevelBar
import de.belkis.spyhound.ui.components.SpyButton
import de.belkis.spyhound.ui.components.SpyCard
import java.text.DateFormat
import java.util.Date

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
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Spyhound", style = MaterialTheme.typography.displayLarge)
        Text(
            "Spuren bekannter Spyware und Stalkerware aufspüren — ruhig, offline, ohne Telemetrie.",
            style = MaterialTheme.typography.bodyLarge,
        )
        state.iocs?.let { loaded ->
            SpyCard {
                Text("Indikatoren", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${loaded.set.size} Einträge aus ${loaded.status.available} von ${loaded.status.sources} Quellen",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (loaded.status.missing.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${loaded.status.missing.size} Quellen fehlen — optional nachladen.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(12.dp))
                SpyButton(
                    text = if (state.updating != null) state.updating else "Weitere Quellen laden",
                    onClick = onUpdateIndicators,
                    enabled = state.updating == null,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.updateMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = SpyColors.Clear)
                }
            }
        }
        SpyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("DNS-Wächter", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Warnt bei DNS-Anfragen zu bekannten Indikator-Domains. Kein Server, kein Blockieren.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = state.dnsRunning,
                    onCheckedChange = onDnsToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SpyColors.Ink,
                        checkedTrackColor = SpyColors.Amber,
                        uncheckedThumbColor = SpyColors.IvoryMuted,
                        uncheckedTrackColor = SpyColors.Border,
                    ),
                )
            }
            if (state.dnsHits.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Letzte Treffer", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                val fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                state.dnsHits.take(5).forEach { hit -> DnsHitLine(hit, fmt) }
            }
        }
        SpyCard {
            AccessLevelBar(current = state.access)
            Spacer(Modifier.height(12.dp))
            PermissionRow("SMS lesen", state.smsGranted, "Links in Nachrichten prüfen", onRequestSms)
            PermissionRow("Alle Dateien", state.filesGranted, "Verdächtige Dateien auf dem Speicher", onRequestFiles)
            Spacer(Modifier.height(8.dp))
            ShizukuRow(state.shizuku, onConnectShizuku)
            Spacer(Modifier.height(8.dp))
            SpyButton(text = "Root-Zugriff anfragen", onClick = onRequestRoot, modifier = Modifier.fillMaxWidth())
            if (state.rootDenied) {
                Spacer(Modifier.height(6.dp))
                Text("Root wurde abgelehnt oder ist nicht verfügbar.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        state.busy?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = SpyColors.Amber) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            RadarScanButton(onClick = onScan, enabled = state.iocs != null && state.progress == null)
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, hint: String, onRequest: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (granted) SpyColors.Clear else SpyColors.Ivory)
            Text(hint, style = MaterialTheme.typography.bodyMedium)
        }
        if (!granted) {
            SpyButton(text = "Erlauben", onClick = onRequest)
        } else {
            Text("OK", style = MaterialTheme.typography.labelLarge, color = SpyColors.Clear)
        }
    }
}

@Composable
private fun ShizukuRow(status: ShizukuShell.Status, onConnect: () -> Unit) {
    val context = LocalContext.current
    when (status) {
        ShizukuShell.Status.NOT_RUNNING, ShizukuShell.Status.TOO_OLD -> {
            Text(
                "Shizuku ist nicht aktiv. Damit lassen sich tiefere Prüfungen ohne PC freischalten.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            SpyButton(
                text = "Shizuku installieren",
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        ShizukuShell.Status.NEEDS_PERMISSION, ShizukuShell.Status.READY -> {
            SpyButton(
                text = if (status == ShizukuShell.Status.READY) "Mit Shizuku verbinden" else "Shizuku-Freigabe erteilen",
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
            )
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
