package de.belkis.spyhound.scan

import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocType

/**
 * Checks that need a shell (Shizuku = ADB rights, or root). They read what AndroidQF
 * would collect over USB: running processes, every package including removed ones,
 * system service state and logs, file listings and file fingerprints.
 * Pure functions over the command output, so they are testable without a phone.
 */
object ShellAnalyzer {
    const val PROCESSES = "Laufende Prozesse"
    const val PACKAGES = "Alle Pakete"
    const val LOGS = "Systemprotokolle"
    const val FILES = "Dateien"
    const val HASHES = "Datei-Fingerabdrücke"

    /** `ps -A -o NAME` (one process name per line). */
    fun processes(psOutput: String, iocs: IocSet): CheckResult {
        val r = CheckResult(PROCESSES)
        for (line in psOutput.lineSequence().drop(1)) {
            val name = line.trim().ifEmpty { continue }
            r.examined++
            // An Android process is named after its app, optionally with ":suffix".
            val app = name.substringBefore(':')
            val ind = iocs.lookup(IocType.PROCESS, name) ?: iocs.lookup(IocType.APP_ID, app)
            if (ind != null) r.hit("Prozess $name", "Ein Prozess mit einem Namen, den Forscher einer Spyware zuordnen, läuft gerade.", ind)
        }
        r.ran = r.examined > 0
        if (!r.ran) r.gap("Die Prozessliste war leer oder nicht lesbar.")
        return r
    }

    /**
     * `pm list packages -u -i`: "package:com.x  installer=y". -u includes apps that were
     * uninstalled but kept their data, and apps of other users / work profiles.
     */
    fun packages(pmOutput: String, installed: Set<String>, iocs: IocSet): CheckResult {
        val r = CheckResult(PACKAGES)
        for (line in pmOutput.lineSequence()) {
            if (!line.startsWith("package:")) continue
            val name = line.removePrefix("package:").substringBefore(' ').trim()
            if (name.isEmpty()) continue
            r.examined++
            if (name in installed) continue // already compared by the app check
            iocs.lookup(IocType.APP_ID, name)?.let {
                r.hit("Paket $name", "Ein Paket, das Forscher einer Spyware zuordnen, ist auf dem Gerät registriert (z. B. gelöscht mit behaltenen Daten oder in einem anderen Profil).", it)
            }
        }
        r.ran = r.examined > 0
        if (!r.ran) r.gap("Die Paketliste war leer oder nicht lesbar.")
        return r
    }

    /** Feeds every output line of a command to the callback; false if the command failed or timed out. */
    fun interface LineSource {
        fun feed(onLine: (String) -> Unit): Boolean
    }

    private fun fromText(text: String?) = LineSource { onLine ->
        text?.lineSequence()?.forEach(onLine)
        text != null
    }

    /** Domains, app ids, paths and URLs mentioned in dumpsys output and logcat. */
    /** [installed] apps are compared by the app check; here only their absence matters (removed spyware). */
    fun logs(sources: Map<String, String>, iocs: IocSet, installed: Set<String> = emptySet()): CheckResult =
        logs(sources.map { (label, text) -> label to fromText(text) }, iocs, installed)

    /** Same as above, but each log is read and scanned line by line, so only one line is in memory at a time. */
    fun logs(sources: List<Pair<String, LineSource>>, iocs: IocSet, installed: Set<String> = emptySet()): CheckResult {
        val r = CheckResult(LOGS)
        val scanner = TextScanner(iocs)
        val reported = HashSet<String>()
        for ((label, source) in sources) {
            var lines = 0
            val ok = source.feed { line ->
                if (line.isNotBlank()) lines++
                val declaration = DECLARATION.containsMatchIn(line)
                for (m in scanner.all(line)) {
                    if (m.indicator.type == IocType.APP_ID && (m.token in installed || declaration)) continue
                    if (!reported.add(m.indicator.value)) continue
                    val kind = when (m.indicator.type) {
                        IocType.DOMAIN -> "die Adresse ${m.token}"
                        IocType.APP_ID -> "die App ${m.token}"
                        IocType.FILE_PATH -> "den Pfad ${m.token}"
                        IocType.IP -> "die IP-Adresse ${m.token}"
                        else -> m.token
                    }
                    r.hit(label, "Die Systemaufzeichnung nennt $kind, die Forscher einer Spyware zuordnen. Eine Nennung beweist keinen Kontakt, ist aber ein starker Hinweis.", m.indicator)
                }
            }
            if (lines == 0) { r.gap("$label konnte nicht gelesen werden."); continue }
            r.examined++
            if (!ok) r.gap("$label wurde nur teilweise gelesen (Zeitüberschreitung oder Abbruch).")
        }
        r.ran = r.examined > 0
        return r
    }

    /**
     * Lines where an app only declares which other packages it wants to see or talk to
     * (`<queries>` in its manifest). Microsoft apps, for example, list com.microsoft.familysafety
     * there; naming a package in such a list says nothing about it being on the phone.
     */
    private val DECLARATION = Regex("""\b(queriesPackages|queriesProviders|queriesIntents|usesLibraries|usesOptionalLibraries|usesStaticLibraries|usesSdkLibraries)=""")

    /** `find ... -type f` output: one absolute path per line. */
    fun files(listing: String, iocs: IocSet): CheckResult = files(fromText(listing), iocs)

    fun files(source: LineSource, iocs: IocSet): CheckResult {
        val r = CheckResult(FILES)
        val ok = source.feed { line ->
            val path = line.trim()
            if (path.startsWith("/")) {
                r.examined++
                val ind = iocs.lookup(IocType.FILE_PATH, path)
                    ?: iocs.lookup(IocType.FILE_NAME, path.substringAfterLast('/'))
                if (ind != null) r.hit("Datei $path", "Eine Datei mit einem Namen oder Pfad, den Forscher einer Spyware zuordnen, liegt auf dem Gerät.", ind)
            }
        }
        r.finish(ok, "Die Dateiliste")
        return r
    }

    /** `sha256sum` output: "<hash>  <path>". */
    fun hashes(sumOutput: String, iocs: IocSet): CheckResult = hashes(fromText(sumOutput), iocs)

    fun hashes(source: LineSource, iocs: IocSet): CheckResult {
        val r = CheckResult(HASHES)
        val ok = source.feed { line ->
            val hash = line.substringBefore(' ').trim().lowercase()
            if (hash.length == 64) {
                val path = line.substringAfter(' ').trim().removePrefix("*")
                r.examined++
                iocs.lookup(IocType.SHA256, hash)?.let {
                    r.hit("Datei $path", "Die Datei hat den Fingerabdruck einer veröffentlichten Spyware-Probe.", it)
                }
            }
        }
        r.finish(ok, "Die Liste der Fingerabdrücke")
        return r
    }

    /** A check counts as run only if it examined something; a failed or empty command is a gap. */
    private fun CheckResult.finish(ok: Boolean, what: String) {
        ran = examined > 0
        if (!ran) gap("$what war leer oder der Befehl ist fehlgeschlagen. Es wurde nichts geprüft.")
        else if (!ok) gap("$what wurde nur teilweise gelesen (Zeitüberschreitung oder Abbruch).")
    }
}
