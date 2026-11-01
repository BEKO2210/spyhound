package de.belkis.spyhound.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import de.belkis.spyhound.ui.SpyTypographyExtras
import de.belkis.spyhound.ui.components.AnimatedCount
import de.belkis.spyhound.ui.components.CollapsibleSection
import de.belkis.spyhound.ui.components.InfoChip
import de.belkis.spyhound.ui.components.SpyButton
import de.belkis.spyhound.ui.components.SpyDivider
import de.belkis.spyhound.ui.components.TextActionLink
import de.belkis.spyhound.ui.components.WhyLink
import kotlinx.coroutines.delay

@Composable
fun ResultScreen(
    report: Report,
    onShare: () -> Unit,
    onRescan: () -> Unit,
    onGapFix: (GapFix) -> Unit,
    onHelp: () -> Unit,
) {
    val motion = LocalMotionEnabled.current
    var stage by remember(report) { mutableIntStateOf(if (motion) 0 else 4) }
    LaunchedEffect(report, motion) {
        if (!motion) {
            stage = 4
            return@LaunchedEffect
        }
        stage = 0
        repeat(4) {
            delay(90)
            stage++
        }
    }
    val enter = if (motion) {
        fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 5 }
    } else {
        fadeIn(tween(0))
    }
    val examined = report.checks.sumOf { it.examined }
    val durationSec = ((report.finishedAt - report.startedAt) / 1000).coerceAtLeast(1)

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (stage >= 0) {
                AnimatedVisibility(visible = true, enter = enter) {
                    VerdictHero(
                        verdict = report.verdict,
                        subtitle = "${ReportText.levelName(report.access)} · ${durationSec} s · $examined geprüft",
                    )
                }
            }
            if (stage >= 1) {
                AnimatedVisibility(visible = true, enter = enter) {
                    CountBar(
                        findings = report.findings.size,
                        leads = report.leads.size,
                        gaps = report.gaps.size,
                    )
                }
            }
            SpyDivider(Modifier.padding(vertical = 8.dp))
            if (stage >= 2 && report.findings.isNotEmpty()) {
                AnimatedVisibility(visible = true, enter = enter) {
                    CollapsibleSection(
                        title = "Funde",
                        count = report.findings.size,
                        titleColor = SpyColors.Danger,
                        initiallyExpanded = true,
                    ) {
                        report.findings.forEach { f ->
                            FindingBlock(f.where, f.detail, f.indicator.family, f.indicator.sourceId)
                        }
                    }
                }
                SpyDivider()
            }
            if (stage >= 2 && report.leads.isNotEmpty()) {
                AnimatedVisibility(visible = true, enter = enter) {
                    CollapsibleSection(
                        title = "Zum Anschauen",
                        count = report.leads.size,
                    ) {
                        Text(
                            "Keine Funde — aber prüfenswerte Hinweise.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        report.leads.forEach { lead -> LeadRow(lead) }
                    }
                }
                SpyDivider()
            }
            if (stage >= 3 && report.gaps.isNotEmpty()) {
                AnimatedVisibility(visible = true, enter = enter) {
                    CollapsibleSection(
                        title = "Nicht geprüft",
                        count = report.gaps.size,
                    ) {
                        Text(
                            "Was fehlt, ist keine Entwarnung.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        report.gaps.forEach { gap -> GapRow(gap, onGapFix) }
                    }
                }
            }
            if (stage >= 3) {
                TextActionLink(text = "Hilfe (Access Now)", onClick = onHelp, color = SpyColors.IvoryMuted)
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SpyButton(
                text = "Neu prüfen",
                onClick = onRescan,
                modifier = Modifier.weight(1f),
            )
            SpyButton(
                text = "Bericht teilen",
                onClick = onShare,
                accent = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun VerdictHero(verdict: Verdict, subtitle: String) {
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
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            ReportText.verdictText(verdict),
            style = MaterialTheme.typography.headlineMedium,
            color = tint,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun CountBar(findings: Int, leads: Int, gaps: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CountCell("Funde", findings, SpyColors.Danger)
        CountCell("Hinweise", leads, SpyColors.Amber)
        CountCell("Lücken", gaps, SpyColors.IvoryMuted)
    }
}

@Composable
private fun CountCell(label: String, value: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedCount(value, style = SpyTypographyExtras.tabularMedium.copy(color = color))
        Text(label, style = MaterialTheme.typography.labelLarge, color = SpyColors.IvoryMuted)
    }
}

@Composable
private fun FindingBlock(where: String, detail: String, family: String, source: String) {
    Spacer(Modifier.height(12.dp))
    Text(where, style = MaterialTheme.typography.labelLarge, color = SpyColors.Danger)
    Text(detail, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (family.isNotBlank()) InfoChip(family)
        InfoChip(if (source.isNotBlank()) "Quelle $source" else "Quelle unbekannt")
    }
}

@Composable
private fun LeadRow(lead: Lead) {
    var open by remember { mutableStateOf(false) }
    Spacer(Modifier.height(12.dp))
    Text(lead.where, style = MaterialTheme.typography.labelLarge)
    Text(lead.detail, style = MaterialTheme.typography.bodyMedium)
    WhyLink(expanded = open, onToggle = { open = !open })
    AnimatedVisibility(open) {
        Text(lead.why, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun GapRow(gap: Gap, onFix: (GapFix) -> Unit) {
    Spacer(Modifier.height(12.dp))
    Text(gap.check, style = MaterialTheme.typography.labelLarge)
    Text(gap.detail, style = MaterialTheme.typography.bodyMedium)
    gapFixLabel(gap.fix)?.let { label ->
        TextActionLink(text = label, onClick = { onFix(gap.fix) }, modifier = Modifier.padding(top = 4.dp))
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
