package de.belkis.spyhound.scan

import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Plain-text report to hand to a helper. Contains app names and SMS senders: share it privately. */
object ReportText {
    fun render(r: Report, version: String, texts: Texts = TextsDe): String =
        if (texts === TextsEn) renderEnglish(r.localized(texts), version) else renderGerman(r, version)

    private fun renderGerman(r: Report, version: String): String = buildString {
        val fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, java.util.Locale.GERMAN)
        val kind = r.title?.let { "$it" } ?: "Prüfbericht"
        appendLine("Spyhound $version – $kind")
        appendLine("Zeit: ${fmt.format(Date(r.startedAt))} (${(r.finishedAt - r.startedAt) / 1000} s)")
        appendLine("Gerät: ${r.device}")
        appendLine("Prüftiefe: ${levelName(r.access)}")
        appendLine("Indikatoren: ${r.indicatorCount} aus ${r.indicatorSources} Quellen" +
            if (r.indicatorSourcesMissing > 0) " (${r.indicatorSourcesMissing} fehlen)" else "")
        appendTimeRange(r, Locale.GERMAN)
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

    private fun renderEnglish(r: Report, version: String): String = buildString {
        val fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, java.util.Locale.ENGLISH)
        appendLine("Spyhound $version – ${r.title ?: "Report"}")
        appendLine("Time: ${fmt.format(Date(r.startedAt))} (${(r.finishedAt - r.startedAt) / 1000} s)")
        appendLine("Device: ${r.device}")
        appendLine("Check depth: ${levelName(r.access, TextsEn)}")
        appendLine("Indicators: ${r.indicatorCount} from ${r.indicatorSources} sources" +
            if (r.indicatorSourcesMissing > 0) " (${r.indicatorSourcesMissing} missing)" else "")
        appendTimeRange(r, Locale.ENGLISH)
        appendLine()
        appendLine("RESULT: ${verdictText(r.verdict, TextsEn)}")
        appendLine()
        if (r.findings.isNotEmpty()) {
            appendLine("FINDINGS (${r.findings.size})")
            r.findings.forEach { f ->
                appendLine("- [${f.check}] ${f.where}")
                appendLine("  ${f.detail}")
                appendLine("  Indicator: ${f.indicator.value} (${f.indicator.type.name.lowercase()}, ${f.indicator.family.ifBlank { "?" }}, source ${f.indicator.sourceId})")
            }
            appendLine()
        }
        if (r.leads.isNotEmpty()) {
            appendLine("WORTH A LOOK (${r.leads.size}) – no finding, but worth checking")
            r.leads.forEach { appendLine("- [${it.check}] ${it.where}: ${it.detail}") }
            appendLine()
        }
        appendLine("NOT CHECKED (${r.gaps.size}) – unchecked areas limit this result")
        r.gaps.forEach { appendLine("- [${it.check}] ${it.detail}") }
        appendLine()
        appendLine("SCOPE")
        r.checks.filter { it.ran }.forEach { appendLine("- ${it.name}: ${it.examined} checked") }
        appendLine()
        appendLine("No findings does not mean your phone is safe. Spyhound detects only published traces")
        appendLine("of known spyware. If you have reason for concern, contact Access Now Digital")
        appendLine("Security Helpline (https://www.accessnow.org/help/) or Amnesty Security Lab.")
    }

    private fun StringBuilder.appendTimeRange(r: Report, locale: java.util.Locale) {
        val range = TimeRange.evaluate(r.oldestEvidence, r.finishedAt, locale)
        if (!range.hasEvidence) return
        appendLine()
        appendLine(if (locale.language == "en") "CHECKED TIME RANGE" else "GEPRÜFTER ZEITRAUM")
        range.sources.forEach { appendLine("- ${it.text}") }
        if (range.warning) appendLine(if (locale.language == "en")
            "Warning: System logs cover less than 48 hours; older infections may not appear there."
        else "Warnung: Systemprotokolle reichen weniger als 48 Stunden zurück; ältere Infektionen sind dort möglicherweise nicht sichtbar.")
    }

    fun levelName(l: AccessLevel, texts: Texts = TextsDe) = if (texts === TextsEn) when (l) {
        AccessLevel.APP -> "Basic (no special access)"
        AccessLevel.SHELL -> "Extended (Shizuku, ADB access)"
        AccessLevel.ROOT -> "Full (root)"
    } else when (l) {
        AccessLevel.APP -> "Basis (ohne Sonderrechte)"
        AccessLevel.SHELL -> "Erweitert (Shizuku, ADB-Rechte)"
        AccessLevel.ROOT -> "Vollständig (Root)"
    }

    fun verdictText(v: Verdict, texts: Texts = TextsDe) = if (texts === TextsEn) when (v) {
        Verdict.SPYWARE_INDICATOR -> "Known spyware trace found"
        Verdict.MALWARE -> "Known malware file found"
        Verdict.STALKERWARE -> "Known monitoring app found"
        Verdict.LEADS_ONLY -> "No known traces. Some items are worth a look"
        Verdict.NOTHING_FOUND -> "No known traces in checked areas"
    } else when (v) {
        Verdict.SPYWARE_INDICATOR -> "Spuren bekannter Spionage-Software gefunden"
        Verdict.MALWARE -> "Bekannte Schadsoftware-Datei gefunden"
        Verdict.STALKERWARE -> "Bekannte Überwachungs-App gefunden"
        Verdict.LEADS_ONLY -> "Keine bekannten Spuren, aber Hinweise zum Anschauen"
        Verdict.NOTHING_FOUND -> "Keine bekannten Spuren im geprüften Bereich"
    }
}
