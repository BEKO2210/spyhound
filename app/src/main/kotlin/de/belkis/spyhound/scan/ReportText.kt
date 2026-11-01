package de.belkis.spyhound.scan

import java.text.DateFormat
import java.util.Date

/** Plain-text report to hand to a helper. Contains app names and SMS senders: share it privately. */
object ReportText {
    fun render(r: Report, version: String): String = buildString {
        val fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        val kind = r.title?.let { "$it" } ?: "Prüfbericht"
        appendLine("Spyhound $version – $kind")
        appendLine("Zeit: ${fmt.format(Date(r.startedAt))} (${(r.finishedAt - r.startedAt) / 1000} s)")
        appendLine("Gerät: ${r.device}")
        appendLine("Prüftiefe: ${levelName(r.access)}")
        appendLine("Indikatoren: ${r.indicatorCount} aus ${r.indicatorSources} Quellen" +
            if (r.indicatorSourcesMissing > 0) " (${r.indicatorSourcesMissing} fehlen)" else "")
        appendLine()
        appendLine("ERGEBNIS: ${verdictText(r.verdict)}")
        appendLine()
        if (r.findings.isNotEmpty()) {
            appendLine("FUNDE (${r.findings.size})")
            r.findings.forEach { f ->
                appendLine("- [${f.check}] ${f.where}")
                appendLine("  ${f.detail}")
                appendLine("  Indikator: ${f.indicator.value} (${f.indicator.type.name.lowercase()}, ${f.indicator.family.ifBlank { "?" }}, Quelle ${f.indicator.sourceId})")
            }
            appendLine()
        }
        if (r.leads.isNotEmpty()) {
            appendLine("ZUM ANSCHAUEN (${r.leads.size}) – keine Funde, aber prüfenswert")
            r.leads.forEach { appendLine("- [${it.check}] ${it.where}: ${it.detail}") }
            appendLine()
        }
        appendLine("NICHT GEPRÜFT (${r.gaps.size}) – eine Lücke ist kein sauberes Ergebnis")
        r.gaps.forEach { appendLine("- [${it.check}] ${it.detail}") }
        appendLine()
        appendLine("UMFANG")
        r.checks.filter { it.ran }.forEach { appendLine("- ${it.name}: ${it.examined} geprüft") }
        appendLine()
        appendLine("Ein Ergebnis ohne Funde ist keine Unbedenklichkeitsbescheinigung. Spyhound erkennt nur")
        appendLine("veröffentlichte Spuren bekannter Spyware. Bei begründetem Verdacht: Access Now Digital")
        appendLine("Security Helpline (https://www.accessnow.org/help/) oder Amnesty Security Lab.")
    }

    fun levelName(l: AccessLevel) = when (l) {
        AccessLevel.APP -> "Basis (ohne Sonderrechte)"
        AccessLevel.SHELL -> "Erweitert (Shizuku, ADB-Rechte)"
        AccessLevel.ROOT -> "Vollständig (Root)"
    }

    fun verdictText(v: Verdict) = when (v) {
        Verdict.SPYWARE_INDICATOR -> "Spuren bekannter Spionage-Software gefunden"
        Verdict.STALKERWARE -> "Bekannte Überwachungs-App gefunden"
        Verdict.LEADS_ONLY -> "Keine bekannten Spuren, aber Hinweise zum Anschauen"
        Verdict.NOTHING_FOUND -> "Keine bekannten Spuren im geprüften Bereich"
    }
}
