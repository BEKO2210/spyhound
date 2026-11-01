package de.belkis.spyhound.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.scan.Gap
import de.belkis.spyhound.scan.GapFix
import de.belkis.spyhound.scan.Lead
import de.belkis.spyhound.scan.Report
import de.belkis.spyhound.scan.ReportText
import de.belkis.spyhound.scan.Verdict
import de.belkis.spyhound.ui.LocalMotionEnabled
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.components.SpyButton
import de.belkis.spyhound.ui.components.SpyCard

@Composable
fun ResultScreen(
    report: Report,
    onShare: () -> Unit,
    onRescan: () -> Unit,
    onGapFix: (GapFix) -> Unit,
    onHelp: () -> Unit,
) {
    val motion = LocalMotionEnabled.current
    val enter = if (motion) {
        fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
    } else {
        fadeIn(tween(0))
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AnimatedVisibility(visible = true, enter = enter) {
            VerdictHero(report.verdict)
        }
        AnimatedVisibility(visible = true, enter = enter) {
            SpyCard {
                Text("Urteil", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(ReportText.verdictText(report.verdict), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(6.dp))
                Text(ReportText.levelName(report.access), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (report.findings.isNotEmpty()) {
            AnimatedVisibility(visible = true, enter = enter) {
                SpyCard {
                    Text(
                        "Funde (${report.findings.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = SpyColors.Danger,
                    )
                    report.findings.forEach { f ->
                        Spacer(Modifier.height(10.dp))
                        Text(f.where, style = MaterialTheme.typography.labelLarge, color = SpyColors.Danger)
                        Text(f.detail, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (report.leads.isNotEmpty()) {
            AnimatedVisibility(visible = true, enter = enter) {
                SpyCard {
                    Text("Zum Anschauen", style = MaterialTheme.typography.titleMedium)
                    Text("Keine Funde — aber prüfenswerte Hinweise.", style = MaterialTheme.typography.bodyMedium)
                    report.leads.forEach { lead -> LeadRow(lead) }
                }
            }
        }
        if (report.gaps.isNotEmpty()) {
            AnimatedVisibility(visible = true, enter = enter) {
                SpyCard {
                    Text("Nicht geprüft", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Was fehlt, ist keine Entwarnung. Schließe Lücken, wenn du kannst.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    report.gaps.forEach { gap -> GapRow(gap, onGapFix) }
                }
            }
        }
        SpyButton(text = "Hilfe (Access Now)", onClick = onHelp, modifier = Modifier.fillMaxWidth())
        SpyButton(text = "Bericht teilen", onClick = onShare, accent = true, modifier = Modifier.fillMaxWidth())
        SpyButton(text = "Neu prüfen", onClick = onRescan, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun VerdictHero(verdict: Verdict) {
    val (icon, tint) = when (verdict) {
        Verdict.SPYWARE_INDICATOR -> Icons.Outlined.Report to SpyColors.Danger
        Verdict.STALKERWARE -> Icons.Outlined.Visibility to SpyColors.Danger
        Verdict.LEADS_ONLY -> Icons.Outlined.Info to SpyColors.Amber
        Verdict.NOTHING_FOUND -> Icons.Outlined.Shield to SpyColors.Clear
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            ReportText.verdictText(verdict),
            style = MaterialTheme.typography.headlineMedium,
            color = tint,
        )
    }
}

@Composable
private fun LeadRow(lead: Lead) {
    var open by remember { mutableStateOf(false) }
    Spacer(Modifier.height(10.dp))
    Text(lead.where, style = MaterialTheme.typography.labelLarge)
    Text(lead.detail, style = MaterialTheme.typography.bodyMedium)
    SpyButton(text = if (open) "Warum? ausblenden" else "Warum?", onClick = { open = !open })
    AnimatedVisibility(open) {
        Text(lead.why, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun GapRow(gap: Gap, onFix: (GapFix) -> Unit) {
    Spacer(Modifier.height(10.dp))
    Text("[${gap.check}]", style = MaterialTheme.typography.labelLarge)
    Text(gap.detail, style = MaterialTheme.typography.bodyMedium)
    gapFixLabel(gap.fix)?.let { label ->
        Spacer(Modifier.height(6.dp))
        SpyButton(text = label, onClick = { onFix(gap.fix) })
    }
}

private fun gapFixLabel(fix: GapFix): String? = when (fix) {
    GapFix.PERMISSION_SMS -> "SMS-Berechtigung erteilen"
    GapFix.PERMISSION_FILES -> "Voller Dateizugriff"
    GapFix.SHIZUKU -> "Shizuku verbinden"
    GapFix.ROOT -> "Root anfragen"
    GapFix.UPDATE_INDICATORS -> "Quellen laden"
    GapFix.NONE -> null
}
