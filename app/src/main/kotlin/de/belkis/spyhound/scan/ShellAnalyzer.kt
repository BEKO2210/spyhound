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

    /** Domains, app ids, paths and URLs mentioned in dumpsys output and logcat. */
    /** [installed] apps are compared by the app check; here only their absence matters (removed spyware). */
    fun logs(sources: Map<String, String>, iocs: IocSet, installed: Set<String> = emptySet()): CheckResult {
        val r = CheckResult(LOGS)
        val scanner = TextScanner(iocs)
        val reported = HashSet<String>()
        for ((label, text) in sources) {
            if (text.isEmpty()) { r.gap("$label konnte nicht gelesen werden."); continue }
            r.examined++
            for (m in scanner.all(text)) {
                if (m.indicator.type == IocType.APP_ID && m.token in installed) continue
                if (!reported.add(m.indicator.value)) continue
                val kind = when (m.indicator.type) {
                    IocType.DOMAIN -> "die Adresse ${m.token}"
                    IocType.APP_ID -> "die App ${m.token}"
                    IocType.FILE_PATH -> "den Pfad ${m.token}"
                    else -> m.token
                }
                r.hit(label, "Die Systemaufzeichnung nennt $kind, die Forscher einer Spyware zuordnen. Eine Nennung beweist keinen Kontakt, ist aber ein starker Hinweis.", m.indicator)
            }
        }
        r.ran = r.examined > 0
        return r
    }

    /** `find ... -type f` output: one absolute path per line. */
    fun files(listing: String, iocs: IocSet): CheckResult {
        val r = CheckResult(FILES)
        for (line in listing.lineSequence()) {
            val path = line.trim()
            if (!path.startsWith("/")) continue
            r.examined++
            val ind = iocs.lookup(IocType.FILE_PATH, path)
                ?: iocs.lookup(IocType.FILE_NAME, path.substringAfterLast('/'))
            if (ind != null) r.hit("Datei $path", "Eine Datei mit einem Namen oder Pfad, den Forscher einer Spyware zuordnen, liegt auf dem Gerät.", ind)
        }
        r.ran = true
        return r
    }

    /** `sha256sum` output: "<hash>  <path>". */
    fun hashes(sumOutput: String, iocs: IocSet): CheckResult {
        val r = CheckResult(HASHES)
        for (line in sumOutput.lineSequence()) {
            val hash = line.substringBefore(' ').trim().lowercase()
            if (hash.length != 64) continue
            val path = line.substringAfter(' ').trim().removePrefix("*")
            r.examined++
            iocs.lookup(IocType.SHA256, hash)?.let {
                r.hit("Datei $path", "Die Datei hat den Fingerabdruck einer veröffentlichten Spyware-Probe.", it)
            }
        }
        r.ran = true
        return r
    }
}
