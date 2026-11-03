package de.belkis.spyhound

import de.belkis.spyhound.bugreport.Bugreport
import de.belkis.spyhound.bugreport.BugreportAnalyzer
import de.belkis.spyhound.bugreport.BugreportParser
import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.AppAnalyzer
import de.belkis.spyhound.scan.AppRecord
import de.belkis.spyhound.scan.ApkFile
import de.belkis.spyhound.scan.CheckResult
import de.belkis.spyhound.scan.DeviceAnalyzer
import de.belkis.spyhound.scan.DeviceState
import de.belkis.spyhound.scan.GapFix
import de.belkis.spyhound.scan.LogSource
import de.belkis.spyhound.scan.Message
import de.belkis.spyhound.scan.MessageAnalyzer
import de.belkis.spyhound.scan.RadarSector
import de.belkis.spyhound.scan.Report
import de.belkis.spyhound.scan.ReportText
import de.belkis.spyhound.scan.ScanEngine
import de.belkis.spyhound.scan.ShellAnalyzer
import de.belkis.spyhound.scan.TextsDe
import de.belkis.spyhound.scan.TextsEn
import de.belkis.spyhound.scan.Verdict
import de.belkis.spyhound.scan.localized
import de.belkis.spyhound.shell.SHIZUKU_STOPPED_BANNER
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Drives the real analyzers through every branch that writes a user-visible text (German source) and asserts that
 * the English layer leaves no German behind. A new German sentence without a translation fails here, not in the UI.
 */
class TextsEnCoverageTest {
    private fun ind(value: String, type: IocType, category: Category = Category.MERCENARY, family: String = "Fam", source: String = "mvt-android") =
        Indicator(value, type, category, family, source)

    private val sha = { c: Char -> c.toString().repeat(64) }

    private val iocs = IocSet(listOf(
        ind("com.stalker.app", IocType.APP_ID, Category.STALKERWARE, "Stalk"),
        ind("com.stalker.nofam", IocType.APP_ID, Category.STALKERWARE, ""),
        ind("com.merc.app", IocType.APP_ID, family = "Pred"),
        ind("com.merc.nofam", IocType.APP_ID, family = ""),
        ind(sha('a'), IocType.SHA256, family = "Pred"),
        ind(sha('b'), IocType.SHA256, family = ""),
        ind(sha('c'), IocType.SHA256, Category.MALWARE, "Mal", "eset-campaign"),
        ind(sha('d'), IocType.SHA256, Category.MALWARE, "Mal", "meta-campaign"),
        ind(sha('e'), IocType.SHA256, Category.MALWARE, "", "other-list"),
        ind(sha('f'), IocType.APP_CERT, family = "Pred"),
        ind(sha('9'), IocType.APP_CERT, family = ""),
        ind("sys.brand.note", IocType.ANDROID_PROP),
        ind("evil.example", IocType.DOMAIN),
        ind("/data/local/tmp/spy", IocType.FILE_PATH),
        ind("spyfile.bin", IocType.FILE_NAME),
        ind("203.0.113.9", IocType.IP),
        ind("fservernetd", IocType.PROCESS),
        ind("https://evil.example/landing", IocType.URL),
        // malware list: coverage gap with families
        ind(sha('1'), IocType.SHA256, Category.MALWARE, "Zeta", "eset-x"),
    ))

    // ---- fixtures ---------------------------------------------------------------------------------------

    private fun allPermissions() = AppAnalyzer.POWERS.keys

    private fun appChecks(): List<CheckResult> {
        val store = "com.android.vending"
        val apps = listOf(
            AppRecord("com.stalker.app", "Stalk", installer = store),
            AppRecord("com.stalker.nofam", "Stalk2", installer = store),
            AppRecord("com.merc.app", "Merc", installer = store),
            AppRecord("com.merc.nofam", "Merc2", installer = store),
            AppRecord("com.hash.app", "Hash", installer = store, certSha256 = listOf("x"), apks = listOf(
                ApkFile("/data/app/a.apk", sha('a')), ApkFile("/data/app/b.apk", sha('b')), ApkFile("/data/app/c.apk", sha('c')),
                ApkFile("/data/app/d.apk", sha('d')), ApkFile("/data/app/e.apk", sha('e')), ApkFile("/data/app/n.apk", null),
            )),
            AppRecord("com.cert.app", "Cert", installer = store, certSha1 = listOf(sha('f')), certSha256 = listOf(sha('9'))),
            AppRecord("com.side.all", "Side", installer = "com.unknown.installer", certSha1 = listOf("x"),
                grantedPermissions = allPermissions(), notificationAccess = true, accessibility = true, deviceAdmin = true,
                hiddenFromLauncher = true),
            AppRecord("com.side.null", "SideNull", installer = null, certSha1 = listOf("x"),
                grantedPermissions = setOf("android.permission.CAMERA")),
            AppRecord("com.side.hidden", "Hidden", installer = null, certSha1 = listOf("x"), hiddenFromLauncher = true),
            AppRecord("com.store.acc", "Acc", installer = store, certSha1 = listOf("x"), accessibility = true),
            AppRecord("com.store.adm", "Adm", installer = store, certSha1 = listOf("x"), deviceAdmin = true),
            AppRecord("com.store.both", "Both", installer = store, certSha1 = listOf("x"), accessibility = true, deviceAdmin = true),
            AppRecord("com.sys.app", "Sys", isSystem = true),
        )
        return listOf(
            AppAnalyzer.analyze(apps, iocs, "de.belkis.spyhound"),
            AppAnalyzer.analyze(emptyList(), IocSet(emptyList()), "de.belkis.spyhound"),
        )
    }

    private fun state(
        props: Map<String, String> = mapOf("x" to "y"), patch: String = "2026-10-01", adb: Boolean? = null,
        wifi: Boolean? = null, selinux: String? = null, root: List<String> = emptyList(), sources: List<String> = emptyList(),
    ) = DeviceState(props, patch, 36, adb, wifi, null, sources, root, selinux)

    private fun deviceChecks(): List<CheckResult> {
        val today = LocalDate.of(2026, 11, 1)
        fun a(s: DeviceState, shell: Boolean = false) = DeviceAnalyzer.analyze(s, iocs, today, shell)
        return listOf(
            a(state(props = emptyMap(), patch = "")),
            a(state(props = mapOf("sys.brand.note" to "1"))),
            a(state(patch = "2025-01-01")),
            a(state(wifi = true)),
            a(state(wifi = true), shell = true),
            a(state(adb = true)),
            a(state(props = mapOf("ro.boot.verifiedbootstate" to "orange"))),
            a(state(props = mapOf("ro.boot.verifiedbootstate" to "yellow"))),
            a(state(props = mapOf("ro.boot.verifiedbootstate" to "red"))),
            a(state(props = mapOf("ro.boot.flash.locked" to "0"))),
            a(state(props = mapOf("ro.debuggable" to "1"))),
            a(state(props = mapOf("ro.build.tags" to "test-keys"))),
            a(state(selinux = "Permissive")),
            a(state(root = listOf("/system/xbin/su", "magisk"))),
            a(state(sources = listOf("Browser", "Files"))),
        )
    }

    private fun messageChecks(): List<CheckResult> = listOf(
        MessageAnalyzer.analyze(null, iocs),
        MessageAnalyzer.analyze(
            listOf(Message("+49123", 1L, "see http://cdn.evil.example/x and https://evil.example/landing/a"), Message("Bob", 2L, "hi")),
            iocs,
        ),
    )

    private fun lines(vararg l: String) = l.joinToString("\n")

    private fun shellChecks(): List<CheckResult> {
        val out = ArrayList<CheckResult>()
        out += ShellAnalyzer.processes("", iocs)
        out += ShellAnalyzer.processes("NAME\ncom.ok\nfservernetd\ncom.merc.app:remote\n", iocs)
        out += ShellAnalyzer.packages("", emptySet(), iocs)
        out += ShellAnalyzer.packages("package:com.merc.app  installer=x\npackage:com.ok\n", setOf("com.ok"), iocs)
        out += ShellAnalyzer.files("", iocs)
        out += ShellAnalyzer.files(lines("/data/local/tmp/spy", "/sdcard/spyfile.bin", "/sdcard/ok.txt"), iocs)
        out += ShellAnalyzer.files(ShellAnalyzer.LineSource { it("/sdcard/ok.txt"); false }, iocs)
        out += ShellAnalyzer.hashes("", iocs)
        out += ShellAnalyzer.hashes(
            ('a'..'e').joinToString("\n") { "${sha(it)}  /sdcard/$it.apk" } + "\n" + "${sha('1')}  /sdcard/z.apk", iocs,
        )
        out += ShellAnalyzer.hashes(ShellAnalyzer.LineSource { it("${sha('2')}  /sdcard/q.bin"); false }, iocs)
        val logs = mapOf(
            "logcat" to lines("12-01 10:00:00.000 I x: cdn.evil.example", "com.merc.app", "/data/local/tmp/spy", "ping 203.0.113.9",
                "http://evil.example/landing/q"),
            "dumpsys package" to "",
            "dumpsys dbinfo" to "all fine",
        )
        out += ShellAnalyzer.logs(logs, iocs)
        out += ShellAnalyzer.logs(mapOf("dumpsys appops" to "fine"), iocs, setOf("com.merc.app"))
        out += ShellAnalyzer.logs(emptyMap(), iocs)
        out += ShellAnalyzer.logs(listOf("logcat" to ShellAnalyzer.LineSource { it("a line"); false }), iocs)
        out += ShellAnalyzer.logs(listOf("logcat" to ShellAnalyzer.LineSource { false }), iocs)
        return out
    }

    private fun zip(entries: Int): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z -> repeat(entries) { z.putNextEntry(ZipEntry("e$it.txt")); z.write(1); z.closeEntry() } }
        return bos.toByteArray()
    }

    private fun bugreportChecks(): List<CheckResult> {
        val out = ArrayList<CheckResult>()
        val today = LocalDate.of(2026, 11, 1)
        fun a(r: Bugreport, installed: Set<String> = emptySet()) = BugreportAnalyzer.analyze(r, iocs, installed, today)
        // everything missing
        out += a(Bugreport())
        // parser warnings: not a zip, too many entries; size limit text comes from the parser as well
        val notZip = BugreportParser.parse(ByteArrayInputStream(zip(1).also { it[8] = 99 }))
        val tooMany = BugreportParser.parse(ByteArrayInputStream(zip(BugreportParser.MAX_ENTRIES + 1)))
        val tooBig = Bugreport(warnings = listOf("Die entpackte Größe überschreitet die Grenze und wurde abgelehnt."))
        assertTrue(notZip.warnings.isNotEmpty() && tooMany.warnings.isNotEmpty())
        out += a(notZip); out += a(tooMany); out += a(tooBig)
        // old report, truncated, only logs missing logcat
        out += a(Bugreport(createdAt = LocalDateTime.of(2026, 10, 1, 9, 0), truncated = true, device = "dev",
            dumpsys = mapOf("package" to "x"), psText = "", props = mapOf("a" to "b")))
        // hits everywhere
        out += a(Bugreport(
            psText = "USER PID NAME\nroot 1 fservernetd\n", packages = listOf("com.ok", "com.merc.app"),
            logcat = "cdn.evil.example", dumpsys = mapOf("package" to "ping 203.0.113.9"), fsPaths = listOf("/data/local/tmp/spy"),
            boardText = "b", props = mapOf("sys.brand.note" to "1"), createdAt = LocalDateTime.of(2026, 10, 31, 9, 0), device = "d",
        ), setOf("com.ok"))
        return out
    }

    // ---- engine, UI and report strings that are not reachable without a phone --------------------------

    private fun mirroredEngineTexts(): List<String> = listOf(
        // ScanEngine / RadarProgress / LogSource / shell: step names and details shown while scanning
        "Apps", "Installierte Apps werden gelesen …", "12 Apps ausgewertet", "Gerät", "Einstellungen und Systemeigenschaften …",
        "Gerät ausgewertet", "SMS", "Links in Nachrichten …", "SMS nicht lesbar", "40 Nachrichten", "Tiefenprüfung",
        "Nur mit Shizuku oder Root", "Fertig", "Start", "", "Dateien", "Keine Berechtigung", "Gemeinsamer Speicher …",
        "100 Pfade ausgewertet", "Dateiliste …", "Fingerabdrücke", "Programmdateien werden gehasht …", "Prozesse",
        "Laufende Prozesse …", "Pakete", "Alle Pakete, auch gelöschte …", "Systemprotokolle", "Tiefenprüfung ausgewertet",
        LogSource.progressDetail("logcat", 1200), LogSource.progressDetail("dumpsys package", 0),
        SHIZUKU_STOPPED_BANNER, ScanEngine.BUILTIN_ADB_RESTART, ScanEngine.SHELL_DEAD,
        // check ids
        "Root-Prüfung", "Indikatoren", "Fehlerbericht-Prüfung", "Fehlerbericht: Bericht",
        ShellAnalyzer.PROCESSES, ShellAnalyzer.PACKAGES, ShellAnalyzer.LOGS, ShellAnalyzer.FILES, ShellAnalyzer.HASHES,
        AppAnalyzer.NAME, DeviceAnalyzer.NAME, MessageAnalyzer.NAME,
        // gaps created inside ScanEngine
        "Laufende Prozesse, Systemprotokolle (logcat, dumpsys), gelöschte Apps und Dateien außerhalb des " +
            "gemeinsamen Speichers wurden nicht geprüft. Dafür braucht es Shizuku (ohne Root) oder Root.",
        "3 Indikator-Quellen sind noch nicht geladen (mvt, echap, toolkit).",
        "2 Indikator-Quellen sind beschädigt und wurden nicht benutzt (mvt, echap).",
        "Dateien im Speicher (Downloads, Messenger-Anhänge) wurden nicht geprüft: Die Berechtigung „Zugriff auf alle Dateien“ fehlt.",
        "5 Datei(en) über 120 MB wurden nicht mit den Fingerabdrücken verglichen.",
        "7 Datei(en) oder Ordner waren nicht lesbar (z. B. Android/data).",
        "App-Datenordner (/data/data), Absturzberichte (/data/tombstones) und Systemdateien wurden nicht geprüft. Dafür braucht es Root.",
    ) + RadarSector.entries.map { it.label }

    // ---- the test ---------------------------------------------------------------------------------------

    private val germanWords = Regex(
        "\\b(und|nicht|wurde|wurden|ist|sind|der|die|das|dem|den|ein|eine|einen|mit|von|auf|bei|oder|aber|noch|auch|kein|keine|" +
            "konnte|konnten|werden|wird|fehlt|fehlen|hat|darf|kann|läuft|Gerät|geprüft|Datei|Dateien|Prüfung|Fehlerbericht|" +
            "Berechtigung|Bericht|Quelle|Quellen|Spuren|Nachricht|Nachrichten|Zeilen|Tage|Stunden|Schadsoftware|Spionage|" +
            "Überwachungs|Sicherheit|Zugriff|Zeitüberschreitung|Abbruch|Fingerabdruck|Fingerabdrücke|Abschnitt|Paketliste|" +
            "Prozessliste|Systemeigenschaft|Bedienungshilfe|Benachrichtigungen|Standort|Anrufliste|Kontakte|Kamera|Mikrofon|" +
            "Telefon|Neustart|einschalten|Debugging über|Einstellungen|installiert|gelesen|lesbar|entsperrt|abgeschaltet)\\b",
    )

    private fun german(text: String): String? {
        if (text.any { it in "äöüÄÖÜß" }) return "umlaut"
        germanWords.find(text)?.let { return "word '${it.value}'" }
        if (text.contains('„')) return "German quote"
        return null
    }

    private fun collect(c: CheckResult, into: MutableSet<String>) {
        into += c.id
        c.findings.forEach { into += it.where; into += it.detail }
        c.leads.forEach { into += it.where; into += it.detail; into += it.why }
        c.gaps.forEach { into += it.detail }
    }

    private fun allChecks(): List<CheckResult> =
        appChecks() + deviceChecks() + messageChecks() + shellChecks() + bugreportChecks()

    @Test fun everyProducedTextIsTranslated() {
        val sources = linkedSetOf<String>()
        val checks = allChecks()
        checks.forEach { collect(it, sources) }
        sources += mirroredEngineTexts()
        println("TextsEnCoverageTest: collected ${sources.size} distinct texts from ${checks.size} checks")
        assertTrue(sources.size > 100, "fixtures must cover the branches, got ${sources.size}")
        val bad = sources.mapNotNull { s -> german(TextsEn.text(s))?.let { "[$it] $s  ->  ${TextsEn.text(s)}" } }
        assertTrue(bad.isEmpty(), "German left in English texts (${bad.size}):\n" + bad.joinToString("\n"))
    }

    @Test fun germanSourcesReallyAreGerman() {
        // Guards the test itself: the German layer must not be mistaken for clean English.
        val sources = linkedSetOf<String>()
        allChecks().forEach { collect(it, sources) }
        val detected = sources.count { german(it) != null }
        assertTrue(detected > sources.size / 2, "detector found German in only $detected of ${sources.size} sources")
    }

    @Test fun localizedReportAndPlainTextHaveNoGerman() {
        val checks = allChecks()
        val now = 1_800_000_000_000L
        for (verdictChecks in listOf(checks, checks.filter { it.findings.isEmpty() }, checks.filter { it.findings.isEmpty() && it.leads.isEmpty() })) {
            for (level in AccessLevel.entries) {
                val report = Report(
                    startedAt = now - 5_000, finishedAt = now, access = level, indicatorCount = 100, indicatorSources = 5,
                    indicatorSourcesMissing = 2, checks = verdictChecks, device = "Maker Model, Android 16 (Patch 2026-10-05)",
                    oldestEvidence = mapOf("logs" to now - 3_600_000L, "files" to now - 9 * 86_400_000L,
                        "apps" to now - 90 * 86_400_000L, "sms" to now - 30 * 86_400_000L),
                    title = "Fehlerbericht-Prüfung",
                )
                val en = ReportText.render(report, "1.0", TextsEn)
                val bad = en.lines().mapNotNull { l -> german(l)?.let { "[$it] $l" } }
                assertTrue(bad.isEmpty(), "German in English report text:\n" + bad.joinToString("\n"))
                val localized = report.localized(TextsEn)
                (localized.findings.flatMap { listOf(it.check, it.where, it.detail) } +
                    localized.leads.flatMap { listOf(it.check, it.where, it.detail, it.why) } +
                    localized.gaps.flatMap { listOf(it.check, it.detail) } + listOfNotNull(localized.title, localized.device))
                    .forEach { assertTrue(german(it) == null, "German in localized report: $it") }
            }
        }
        Verdict.entries.forEach { v -> assertTrue(german(ReportText.verdictText(v, TextsEn)) == null, v.name) }
        AccessLevel.entries.forEach { assertTrue(german(ReportText.levelName(it, TextsEn)) == null, it.name) }
        // Fix is language independent
        assertTrue(GapFix.entries.isNotEmpty())
    }
}
