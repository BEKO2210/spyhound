package de.belkis.spyhound.scan

/** Pure text layer. Checks keep their evidence; presentation picks a language. */
fun interface Texts {
    fun text(source: String): String
}

object TextsDe : Texts {
    override fun text(source: String) = source
}

object TextsEn : Texts {
    private val exact = mapOf(
        "Apps" to "Apps", "Gerät" to "Device", "SMS" to "SMS", "SMS-Links" to "SMS links",
        "Dateien" to "Files", "Tiefenprüfung" to "Deep check", "Root-Prüfung" to "Root check",
        "Laufende Prozesse" to "Running processes", "Alle Pakete" to "All packages",
        "Systemprotokolle" to "System logs", "Datei-Fingerabdrücke" to "File fingerprints",
        "Bericht" to "Bug report", "Start" to "Starting", "Fertig" to "Done",
        "Prozesse" to "Processes", "Pakete" to "Packages", "Fingerabdrücke" to "Fingerprints",
        "Einstellungen und Systemeigenschaften …" to "Settings and system properties…",
        "Gerät ausgewertet" to "Device checked", "SMS nicht lesbar" to "SMS unavailable",
        "Nur mit Shizuku oder Root" to "Needs Shizuku or root",
        "Alle Pakete, auch gelöschte …" to "All packages, including removed apps…",
        "Programmdateien werden gehasht …" to "Checking file fingerprints…",
        "Tiefenprüfung ausgewertet" to "Deep check complete",
        "Systemeigenschaft" to "System property", "Debugging über WLAN" to "Wireless debugging",
        "USB-Debugging" to "USB debugging", "Bootloader" to "Bootloader", "System-Build" to "System build",
        "Unbekannte Quellen" to "Unknown sources", "Die Dateiliste" to "The file list",
        "Die Liste der Fingerabdrücke" to "The fingerprint list",
        "Die Prozessliste war leer oder nicht lesbar." to "Process list was empty or unreadable.",
        "Die Paketliste war leer oder nicht lesbar." to "Package list was empty or unreadable.",
        "Die Systemeigenschaften (getprop) konnten nicht gelesen werden." to "System properties (getprop) could not be read.",
        "SMS wurden nicht geprüft: Die Berechtigung „SMS lesen“ fehlt." to "SMS was not checked. Read SMS permission is missing.",
        "Spyware nutzt bekannte Sicherheitslücken. Ein aktueller Sicherheitspatch schließt viele davon." to
            "Spyware can use known security flaws. A recent security patch closes many of them.",
        "Mit USB- oder WLAN-Debugging kann jemand mit kurzem Zugriff aufs Gerät Apps unbemerkt installieren. Für Entwickler und Shizuku ist es normal." to
            "USB or wireless debugging can allow someone with brief access to install apps. It is normal for developers and Shizuku.",
        "Ein entsperrter Bootloader, Root oder abgeschaltetes SELinux schwächen den Schutz des Systems. Wenn Sie das nicht selbst eingerichtet haben, ist das ein deutliches Warnzeichen." to
            "An unlocked bootloader, root or disabled SELinux weakens system protection. If you did not set this up, look into it.",
        "Debugging über WLAN ist eingeschaltet." to "Wireless debugging is on.",
        ScanEngine.BUILTIN_ADB_RESTART to "The deep check did not run: wireless debugging is off (for example after a restart of the phone). " +
            "Restart the deep check: turn wireless debugging on and scan once more.",
        ScanEngine.SHELL_DEAD to "The deep check did not run: Shizuku or root is connected but does not respond. " +
            "Restart Shizuku (or allow root again) and scan once more.",
        "USB-Debugging ist eingeschaltet." to "USB debugging is on.",
        "Der Bootloader ist entsperrt (verifiedbootstate=orange)." to "Bootloader is unlocked (verifiedbootstate=orange).",
        "Der Bootloader ist entsperrt (flash.locked=0)." to "Bootloader is unlocked (flash.locked=0).",
        "Das System ist ein Debug- oder Test-Build (ro.debuggable/test-keys)." to "This is a debug or test system build (ro.debuggable/test-keys).",
        "SELinux ist abgeschaltet (permissive)." to "SELinux is permissive.",
        "Damit lassen sich Apps am Store vorbei installieren. Bei Browsern und Dateimanagern ist das oft gewollt." to
            "This allows apps to be installed outside the store. Browsers and file managers often need it.",
        "Eine Systemeigenschaft, die Forscher einer Spyware zuordnen, ist gesetzt." to
            "A system property linked to spyware by researchers is set.",
        "Überwachungs-Apps werden fast immer per Hand installiert, nicht aus einem App-Store. Viele harmlose Apps kommen aber auch so aufs Gerät (z. B. von der Herstellerseite)." to
            "Monitoring apps are often installed outside an app store. Many harmless apps are installed this way too.",
        "Bedienungshilfe, Benachrichtigungszugriff und Geräteadministrator erlauben, den Bildschirm mitzulesen oder das Löschen zu verhindern. Seriöse Apps wie Passwortmanager nutzen sie auch." to
            "Accessibility, notification access and device admin can read your screen or prevent removal. Trusted apps may use them too.",
        "Die Prozessliste war leer oder nicht lesbar." to "Process list was empty or unreadable.",
        "Die Datei hat den Fingerabdruck einer veröffentlichten Spyware-Probe." to
            "This file matches the fingerprint of a published spyware sample.",
        "Die App-Datei hat den Fingerabdruck einer veröffentlichten Spyware-Probe." to
            "This app file matches the fingerprint of a published spyware sample.",
        "Die App ist mit einem Zertifikat signiert, das Forscher einer Spyware zuordnen." to
            "This app is signed with a certificate linked to spyware by researchers.",
        "Ein Prozess mit einem Namen, den Forscher einer Spyware zuordnen, läuft gerade." to
            "A process with a name linked to spyware by researchers is running.",
        "Ein Paket, das Forscher einer Spyware zuordnen, ist auf dem Gerät registriert (z. B. gelöscht mit behaltenen Daten oder in einem anderen Profil)." to
            "A package linked to spyware by researchers is registered on this device. It may be removed with data kept or in another profile.",
        "Eine Datei mit einem Namen oder Pfad, den Forscher einer Spyware zuordnen, liegt auf dem Gerät." to
            "A file with a name or path linked to spyware by researchers is on this device.",
        "Die Datei ist kein gültiges ZIP." to "This file is not a valid ZIP.",
        "Die ZIP enthält zu viele Einträge und wurde abgelehnt." to "ZIP has too many entries and was rejected.",
        "Die entpackte Größe überschreitet die Grenze und wurde abgelehnt." to "Uncompressed size exceeds the limit. ZIP was rejected.",
        "Mindestens ein Abschnitt wurde bei 32 MB abgeschnitten; spätere Zeilen fehlen." to
            "At least one section was cut off at 32 MB. Later lines are missing.",
        "Der Abschnitt PROCESSES AND THREADS fehlt im Fehlerbericht." to "PROCESSES AND THREADS section is missing from bug report.",
        "Die Paketliste (DUMP OF SERVICE package) fehlt im Fehlerbericht." to "Package list (DUMP OF SERVICE package) is missing from bug report.",
        "Die Systemprotokolle (logcat, dumpsys) fehlen im Fehlerbericht." to "System logs (logcat, dumpsys) are missing from bug report.",
        "Der Abschnitt SYSTEM LOG (logcat) fehlt im Fehlerbericht." to "SYSTEM LOG (logcat) section is missing from bug report.",
        "Der Abschnitt SYSTEM PROPERTIES (getprop) fehlt im Fehlerbericht." to "SYSTEM PROPERTIES (getprop) section is missing from bug report.",
        "App-Datenordner (/data/data), Absturzberichte (/data/tombstones) und Systemdateien wurden nicht geprüft. Dafür braucht es Root." to
            "App data (/data/data), crash reports (/data/tombstones) and system files were not checked. Root is needed.",
        "Laufende Prozesse, Systemprotokolle (logcat, dumpsys), gelöschte Apps und Dateien außerhalb des gemeinsamen Speichers wurden nicht geprüft. Dafür braucht es Shizuku (ohne Root) oder Root." to
            "Running processes, system logs, removed apps and private files were not checked. Shizuku or root is needed.",
        "Dateien im Speicher (Downloads, Messenger-Anhänge) wurden nicht geprüft: Die Berechtigung „Zugriff auf alle Dateien“ fehlt." to
            "Files in storage (downloads and message attachments) were not checked. All files access is missing.",
        "Fehlerbericht-Prüfung" to "Bug report check",
        "Shizuku wurde beendet – nach Neustart des Telefons neu starten" to "Shizuku stopped. Restart it after reboot.",
        "Indikatoren" to "Indicators", "Systemeigenschaften" to "System properties",
        "Fehlerbericht: Bericht" to "Bug report: report",
        "Alter des Berichts" to "Age of bug report",
        "Ein alter Bericht zeigt den heutigen Zustand nicht. In den Entwickleroptionen einen neuen Fehlerbericht erstellen und die ZIP an Spyhound teilen." to
            "An old report does not show your phone's current state. Create a new bug report in Developer options and share ZIP with Spyhound.",
        "Keine Berechtigung" to "Permission missing", "Gemeinsamer Speicher …" to "Shared storage…",
        "Laufende Prozesse …" to "Running processes…", "Dateiliste …" to "File list…",
        "Installierte Apps werden gelesen …" to "Reading installed apps…",
        "Links in Nachrichten …" to "Links in messages…",
        "DNS-Wächter / System" to "DNS watch / System",
        "Anfrage zu bekannter Indikator-Domain (nur Demo)." to "Request to a known indicator domain (demo only).",
        "Bootloader-Status konnte nicht ausgelesen werden." to "Bootloader status could not be read.",
        "Ein offener Bootloader erleichtert persistente Spyware – hier nur ein Demo-Hinweis." to
            "An unlocked bootloader can help spyware persist. This is a demo item.",
        "SMS-Berechtigung fehlt – Links in Nachrichten wurden nicht gelesen." to "SMS permission is missing. Links in messages were not checked.",
        "Prozesse und Systemprotokolle ohne Shizuku nicht geprüft (Demo-Lücke)." to
            "Processes and system logs were not checked without Shizuku (demo).",
        "Demo-Gerät, Android 14 (Patch 2024-09-05)" to "Demo device, Android 14 (patch 2024-09-05)",
        "Verbinde …" to "Connecting…", "Verbinde mit Shizuku …" to "Connecting to Shizuku…",
        "Frage Root-Zugriff an … (Root-Manager bestätigen)" to "Requesting root access… (confirm in root manager)",
        "Geteilte Datei ist kein Fehlerbericht (bugreport….zip)." to "Shared file is not a bug report (bugreport….zip).",
        "Indikatoren werden noch geladen – bitte kurz warten." to "Indicators are still loading. Please wait.",
        "Keine gültige Fehlerbericht-ZIP (bugreport….zip)." to "No valid bug report ZIP (bugreport….zip).",
        "Fehlerbericht wird ausgewertet …" to "Checking bug report…",
        "Fehlerbericht konnte nicht gelesen werden." to "Bug report could not be read.",
        "Fehlerbericht wird nach der laufenden Prüfung ergänzt." to "Bug report will be added after this scan.",
        "Die Nachricht enthält einen Link zu" to "This message contains a link to",
        "alle Benachrichtigungen lesen" to "read all notifications",
        "Bedienungshilfe (kann den Bildschirm lesen)" to "accessibility (can read the screen)",
        "Geräteadministrator (erschwert Deinstallation)" to "device admin (makes removal harder)",
        "SMS lesen" to "read SMS", "SMS mitlesen" to "read incoming SMS",
        "Mikrofon" to "microphone", "Kamera" to "camera",
        "Standort im Hintergrund" to "background location", "genauer Standort" to "precise location",
        "Anrufliste" to "call log", "Kontakte" to "contacts",
        "Apps installieren" to "install apps", "Telefonstatus" to "phone state",
        "über anderen Apps zeichnen" to "draw over other apps", "alle Dateien" to "all files",
        "unbekannt" to "unknown",

    )

    private val patterns = listOf(

        Regex("^(\\d+) Apps ausgewertet$") to { m: MatchResult -> "${m.groupValues[1]} apps checked" },
        Regex("^(\\d+) Nachrichten$") to { m: MatchResult -> "${m.groupValues[1]} messages" },
        Regex("^(\\d+) Pfade ausgewertet$") to { m: MatchResult -> "${m.groupValues[1]} paths checked" },
        Regex("^(\\d+) Quellen geladen und geprüft\\.$") to { m: MatchResult -> "${m.groupValues[1]} sources loaded and checked." },
        Regex("^(\\d+) geladen, (\\d+) fehlgeschlagen: (.*)$") to { m: MatchResult ->
            "${m.groupValues[1]} loaded, ${m.groupValues[2]} failed: ${m.groupValues[3]}" },
        Regex("^Eine bekannte Überwachungs-App ist installiert(.*)\\. Manche davon werden offen als Kinderschutz-App verkauft\\.$") to { m: MatchResult ->
            "A known monitoring app is installed${m.groupValues[1]}. Some are sold openly as parental control apps." },
        Regex("^Eine App, die Forscher einer Spionagesoftware zuordnen, ist installiert(.*)\\.$") to { m: MatchResult ->
            "An app linked to spyware by researchers is installed${m.groupValues[1]}." },
        Regex("^Die App-Datei hat den Fingerabdruck einer bekannten Schadsoftware-Probe(.*), Quelle: (.*)\\.$") to { m: MatchResult ->
            "This app file matches the fingerprint of a known malware sample${m.groupValues[1]}, source: ${m.groupValues[2]}." },
        Regex("^Die Datei hat den Fingerabdruck einer bekannten Schadsoftware-Probe(.*), Quelle: (.*)\\.$") to { m: MatchResult ->
            "This file matches the fingerprint of a known malware sample${m.groupValues[1]}, source: ${m.groupValues[2]}." },
        Regex("^Die Schadsoftware-Liste enthält (\\d+) Fingerabdrücke aus ausgewählten Kampagnen \\((.*)\\)\\. Alles andere erkennt sie nicht\\. Kein Ersatz für Play Protect\\.$") to { m: MatchResult ->
            "The malware list holds ${m.groupValues[1]} fingerprints from selected campaigns (${m.groupValues[2]}). It does not detect anything else. No replacement for Play Protect." },
        Regex("^Die App-Datei hat den Fingerabdruck einer veröffentlichten Spyware-Probe(.*)\\.$") to { m: MatchResult ->
            "This app file matches a published spyware sample${m.groupValues[1]}." },
        Regex("^Die App ist mit einem Zertifikat signiert, das Forscher einer Spyware zuordnen(.*)\\.$") to { m: MatchResult ->
            "This app is signed with a certificate linked to spyware by researchers${m.groupValues[1]}." },
        Regex("^Nicht aus einem App-Store installiert \\(Installiert von: (.*)\\)(.*)$") to { m: MatchResult ->
            "Installed outside an app store (installer: ${text(m.groupValues[1])})" +
                m.groupValues[2].replace(". Darf: ", ". Can: ").replace(". Hat kein Symbol im App-Menü", ". No launcher icon")
                    .split(", ").joinToString(", ") { text(it) } },
        Regex("^Hat (.*)\\.$") to { m: MatchResult ->
            "Has " + m.groupValues[1].split(" und ").joinToString(" and ") { text(it) } + "." },
        Regex("^Die Nachricht enthält einen Link zu (.*), einer Adresse, die Forscher einer Spyware zuordnen\\. Wurde der Link geöffnet, kann das Gerät infiziert worden sein\\.$") to { m: MatchResult ->
            "This message has a link to ${m.groupValues[1]}, an address linked to spyware by researchers. If you opened it, your device may have been infected." },
        Regex("^Die Systemaufzeichnung nennt (.*), die Forscher einer Spyware zuordnen\\. Eine Nennung beweist keinen Kontakt, ist aber ein starker Hinweis\\.$") to { m: MatchResult ->
            "System log mentions ${text(m.groupValues[1])}, which researchers link to spyware. A mention alone does not prove contact." },
        Regex("^Der Bootloader ist entsperrt \\(.*") to { m: MatchResult ->
            m.value.replace("Der Bootloader ist entsperrt", "Bootloader is unlocked") },
        Regex("^(\\d+) Indikator-Quellen sind noch nicht geladen \\((.*)\\)\\.$") to { m: MatchResult ->
            "${m.groupValues[1]} indicator sources are not loaded (${m.groupValues[2]})." },
        Regex("^(\\d+) Indikator-Quellen sind beschädigt und wurden nicht benutzt \\((.*)\\)\\.$") to { m: MatchResult ->
            "${m.groupValues[1]} indicator sources are damaged and were not used (${m.groupValues[2]})." },
        Regex("^(\\d+) App-Datei\\(en\\) konnten nicht gelesen und daher nicht mit den Fingerabdrücken verglichen werden\\.$") to { m: MatchResult ->
            "${m.groupValues[1]} app files could not be read or compared with fingerprints." },
        Regex("^Bei (\\d+) App\\(s\\) war das Signatur-Zertifikat nicht lesbar\\.$") to { m: MatchResult ->
            "Signing certificate could not be read for ${m.groupValues[1]} apps." },
        Regex("^(\\d+) Datei\\(en\\) über 120 MB wurden nicht mit den Fingerabdrücken verglichen\\.$") to { m: MatchResult ->
            "${m.groupValues[1]} files over 120 MB were not compared with fingerprints." },
        Regex("^(\\d+) Datei\\(en\\) oder Ordner waren nicht lesbar.*$") to { m: MatchResult ->
            "${m.groupValues[1]} files or folders could not be read (for example Android/data)." },
        Regex("^Der Sicherheitspatch ist (\\d+) Tage alt\\.$") to { m: MatchResult ->
            "Security patch is ${m.groupValues[1]} days old." },
        Regex("^Der Fehlerbericht ist vom (.*) und damit (\\d+) Tage alt\\.$") to { m: MatchResult ->
            "Bug report is from ${m.groupValues[1]} and is ${m.groupValues[2]} days old." },
        Regex("^(.+) · (\\d+) Zeilen$") to { m: MatchResult ->
            "${m.groupValues[1]} · ${m.groupValues[2]} lines" },
        Regex("^(.+) konnte nicht gelesen werden\\.$") to { m: MatchResult ->
            "${text(m.groupValues[1])} could not be read." },
        Regex("^(.+) wurde nur teilweise gelesen \\(Zeitüberschreitung oder Abbruch\\)\\.$") to { m: MatchResult ->
            "${text(m.groupValues[1])} was only partly read (timeout or cancellation)." },
        Regex("^(.+) war leer oder der Befehl ist fehlgeschlagen\\. Es wurde nichts geprüft\\.$") to { m: MatchResult ->
            "${text(m.groupValues[1])} was empty or command failed. Nothing was checked." },
    )

    override fun text(source: String): String {
        exact[source]?.let { return it }
        if (source.startsWith("Fehlerbericht: ")) return "Bug report: ${text(source.removePrefix("Fehlerbericht: "))}"
        patterns.forEach { (pattern, render) -> pattern.matchEntire(source)?.let { return render(it) } }
        return source
            .replace("SMS lesen", "read SMS")
            .replace("SMS mitlesen", "read incoming SMS")
            .replace("Mikrofon", "microphone")
            .replace("Kamera", "camera")
            .replace("Standort im Hintergrund", "background location")
            .replace("genauer Standort", "precise location")
            .replace("Anrufliste", "call log")
            .replace("Kontakte", "contacts")
            .replace("Apps installieren", "install apps")
            .replace("Telefonstatus", "phone state")
            .replace("über anderen Apps zeichnen", "draw over other apps")
            .replace("alle Dateien", "all files")
            .replace("alle Benachrichtigungen lesen", "read all notifications")
            .replace("Bedienungshilfe (kann den Bildschirm lesen)", "accessibility (can read screen)")
            .replace("Geräteadministrator (erschwert Deinstallation)", "device admin (makes removal harder)")
            .replace("Prüfsumme stimmt nicht", "Checksum does not match")
            .replace(" statt ", " instead of ")
            .replace("Datei zu groß", "File is too large")
            .replace("nicht erlaubte Quelle", "Source is not allowed")
            .replace(" (Patch ", " (patch ")
            .replace("Systemeigenschaft ", "System property ")
            .replace("Sicherheitspatch ", "Security patch ")
            .replace("Prozess ", "Process ")
            .replace("Paket ", "Package ")
            .replace("Datei ", "File ")
            .replace("SMS von ", "SMS from ")
            .replace("Fehlerbericht: ", "Bug report: ")
            .replace("die Adresse ", "address ")
            .replace("die App ", "app ")
            .replace("den Pfad ", "path ")
            .replace("die IP-Adresse ", "IP address ")
            .replace("Root-Werkzeugen: ", "root tools: ")
            .replace("Spuren von ", "Traces of ")
            .replace("Diese Apps dürfen andere Apps installieren: ", "These apps can install other apps: ")
            .replace(" (wird für Shizuku gebraucht)", " (needed for Shizuku)")
            .replace("Verified Boot meldet „", "Verified Boot reports “")
            .replace("“. ", "”. ")
            .replace("„", "“")
            .replace("“.", "”.")
    }
}

fun Report.localized(texts: Texts): Report = copy(
    title = title?.let(texts::text),
    device = texts.text(device),
    checks = checks.map { check ->
        CheckResult(check.id, texts).also { out ->
            out.examined = check.examined
            out.ran = check.ran
            check.findings.forEach { out.hit(texts.text(it.where), texts.text(it.detail), it.indicator) }
            check.leads.forEach { out.lead(texts.text(it.where), texts.text(it.detail), texts.text(it.why), it.kind) }
            check.gaps.forEach { out.gap(texts.text(it.detail), it.fix, it.kind) }
        }
    },
)
