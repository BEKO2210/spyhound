package de.belkis.spyhound.bugreport

import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.scan.CheckResult
import de.belkis.spyhound.scan.ShellAnalyzer
import java.io.InputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit

const val BUGREPORT_PREFIX = "Fehlerbericht: "

/**
 * Public entry: parse a bugreport.zip and run the shell checks on what it contains.
 * [installed] apps are skipped in the package/log APP_ID comparison (already covered
 * by the live app check). Pass an empty set when the report is analysed on its own.
 */
fun analyzeBugreport(input: InputStream, iocs: IocSet, installed: Set<String>): List<CheckResult> =
    BugreportAnalyzer.analyze(BugreportParser.parse(input), iocs, installed)

object BugreportAnalyzer {
    const val PROPS = "Systemeigenschaften"
    const val REPORT = "Bericht"

    private const val WHY_OLD =
        "Ein alter Bericht zeigt den heutigen Zustand nicht. In den Entwickleroptionen " +
            "einen neuen Fehlerbericht erstellen und die ZIP an Spyhound teilen."

    fun analyze(
        report: Bugreport,
        iocs: IocSet,
        installed: Set<String>,
        today: LocalDate = LocalDate.now(),
    ): List<CheckResult> {
        val out = ArrayList<CheckResult>()

        val meta = CheckResult(BUGREPORT_PREFIX + REPORT)
        for (w in report.warnings) meta.gap(w)
        if (report.truncated) {
            meta.gap("Mindestens ein Abschnitt wurde bei 32 MB abgeschnitten; spätere Zeilen fehlen.")
        }
        report.createdAt?.toLocalDate()?.let { created ->
            val days = ChronoUnit.DAYS.between(created, today)
            if (days > 7) {
                meta.lead(
                    "Alter des Berichts",
                    "Der Fehlerbericht ist vom $created und damit $days Tage alt.",
                    WHY_OLD,
                )
            }
        }
        if (report.device.isNotEmpty()) meta.examined++
        meta.ran = true
        if (meta.gaps.isNotEmpty() || meta.leads.isNotEmpty()) out += meta

        val psNames = BugreportParser.toPsNames(report.psText)
        if (psNames.lineSequence().drop(1).none { it.isNotBlank() }) {
            out += CheckResult(BUGREPORT_PREFIX + ShellAnalyzer.PROCESSES).apply {
                gap("Der Abschnitt PROCESSES AND THREADS fehlt im Fehlerbericht.")
            }
        } else {
            out += named(ShellAnalyzer.processes(psNames, iocs), BUGREPORT_PREFIX + ShellAnalyzer.PROCESSES)
        }

        if (report.packages.isEmpty()) {
            out += CheckResult(BUGREPORT_PREFIX + ShellAnalyzer.PACKAGES).apply {
                gap("Die Paketliste (DUMP OF SERVICE package) fehlt im Fehlerbericht.")
            }
        } else {
            val pm = report.packages.joinToString("\n") { "package:$it" }
            out += named(ShellAnalyzer.packages(pm, installed, iocs), BUGREPORT_PREFIX + ShellAnalyzer.PACKAGES)
        }

        val sources = LinkedHashMap<String, String>()
        if (report.logcat.isNotEmpty()) sources["logcat"] = report.logcat
        for ((label, text) in report.dumpsys) sources["dumpsys $label"] = text
        if (report.boardText.isNotEmpty()) sources["dumpstate_board"] = report.boardText
        if (report.fsPaths.isNotEmpty()) sources["FS"] = report.fsPaths.joinToString("\n")
        if (sources.isEmpty()) {
            out += CheckResult(BUGREPORT_PREFIX + ShellAnalyzer.LOGS).apply {
                gap("Die Systemprotokolle (logcat, dumpsys) fehlen im Fehlerbericht.")
            }
        } else {
            val logs = named(ShellAnalyzer.logs(sources, iocs, installed), BUGREPORT_PREFIX + ShellAnalyzer.LOGS)
            if (report.logcat.isEmpty()) {
                logs.gap("Der Abschnitt SYSTEM LOG (logcat) fehlt im Fehlerbericht.")
            }
            out += logs
        }

        val props = CheckResult(BUGREPORT_PREFIX + PROPS)
        if (report.props.isEmpty()) {
            props.gap("Der Abschnitt SYSTEM PROPERTIES (getprop) fehlt im Fehlerbericht.")
        } else {
            for (name in report.props.keys) {
                props.examined++
                iocs.lookup(IocType.ANDROID_PROP, name)?.let {
                    props.hit(
                        "Systemeigenschaft $name",
                        "Eine Systemeigenschaft, die Forscher einer Spyware zuordnen, ist gesetzt.",
                        it,
                    )
                }
            }
            props.ran = true
        }
        out += props
        return out
    }

    private fun named(source: CheckResult, name: String): CheckResult {
        val r = CheckResult(name)
        r.examined = source.examined
        r.ran = source.ran
        source.findings.forEach { r.findings += it.copy(check = name) }
        source.leads.forEach { r.leads += it.copy(check = name) }
        source.gaps.forEach { r.gaps += it.copy(check = name) }
        return r
    }
}
