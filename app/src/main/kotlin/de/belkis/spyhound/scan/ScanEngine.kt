package de.belkis.spyhound.scan

import android.content.Context
import android.os.Build
import de.belkis.spyhound.collect.AppCollector
import de.belkis.spyhound.collect.DeviceCollector
import de.belkis.spyhound.collect.SmsCollector
import de.belkis.spyhound.collect.StorageCollector
import de.belkis.spyhound.ioc.LoadedIocs
import de.belkis.spyhound.shell.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class Progress(
    val step: String,
    val detail: String,
    val fraction: Float,
    val radar: RadarProgress? = null,
)

/** Runs every check the current access level allows, and records every one it could not. */
class ScanEngine(private val context: Context, private val iocs: LoadedIocs, private val shell: Shell?) {
    private val level = shell?.level ?: AccessLevel.APP

    suspend fun run(onProgress: (Progress) -> Unit): Report = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        val set = iocs.set
        val checks = ArrayList<CheckResult>()
        val tracker = RadarTracker(deepLocked = shell == null)
        val ownPackage = context.packageName

        fun emit(step: String, detail: String, fraction: Float, sector: RadarSector) {
            tracker.setActive(sector)
            onProgress(tracker.progress(step, detail, fraction))
        }

        tracker.replaceSector(RadarSector.APPS, emptyList())
        emit("Apps", "Installierte Apps werden gelesen …", 0f, RadarSector.APPS)
        val apps = AppCollector(context).collect { done, total, label ->
            tracker.appendClean(RadarSector.APPS)
            emit("Apps", label, 0.45f * done / total.coerceAtLeast(1), RadarSector.APPS)
        }
        val appCheck = AppAnalyzer.analyze(apps, set, ownPackage)
        checks += appCheck
        tracker.replaceSector(RadarSector.APPS, RadarMath.appStatuses(apps, appCheck, ownPackage))
        emit("Apps", "${appCheck.examined} Apps ausgewertet", 0.45f, RadarSector.APPS)
        coroutineContext.ensureActive()

        emit("Gerät", "Einstellungen und Systemeigenschaften …", 0.46f, RadarSector.DEVICE)
        var device = DeviceCollector(context).collect(apps)
        shell?.read("getprop", 4 shl 20, 30)?.let { full ->
            device = device.copy(props = device.props + DeviceCollector.parseGetprop(full))
        }
        val deviceCheck = DeviceAnalyzer.analyze(device, set, shellExpected = level == AccessLevel.SHELL)
        checks += deviceCheck
        tracker.replaceSector(RadarSector.DEVICE, RadarMath.statusesFromCheck(deviceCheck))
        emit("Gerät", "Gerät ausgewertet", 0.48f, RadarSector.DEVICE)

        emit("SMS", "Links in Nachrichten …", 0.5f, RadarSector.SMS)
        val messages = SmsCollector(context).collect()
        val smsCheck = MessageAnalyzer.analyze(messages, set)
        checks += smsCheck
        if (messages != null) {
            tracker.replaceSector(RadarSector.SMS, RadarMath.smsStatuses(messages, smsCheck))
        } else {
            tracker.replaceSector(RadarSector.SMS, emptyList())
        }
        emit("SMS", if (messages == null) "SMS nicht lesbar" else "${smsCheck.examined} Nachrichten", 0.52f, RadarSector.SMS)
        coroutineContext.ensureActive()

        // A shell can be connected but dead (Shizuku restarted, stale service): then every
        // command comes back empty. Probe once and report one clear gap instead of fifteen.
        val shellAlive = shell != null && shell.read("echo spyhound-ok", 64, 15)?.contains("spyhound-ok") == true
        if (shell != null && shellAlive) {
            checks += shellChecks(shell, apps.map { it.packageName }.toSet(), tracker, ::emit)
        } else if (shell != null) {
            checks += storageCheck(tracker, ::emit)
            checks += CheckResult("Tiefenprüfung").apply {
                gap(SHELL_DEAD, if (shell.level == AccessLevel.ROOT) GapFix.ROOT else GapFix.SHIZUKU)
            }
        } else {
            checks += storageCheck(tracker, ::emit)
            checks += CheckResult("Tiefenprüfung").apply {
                gap(
                    "Laufende Prozesse, Systemprotokolle (logcat, dumpsys), gelöschte Apps und Dateien außerhalb des " +
                        "gemeinsamen Speichers wurden nicht geprüft. Dafür braucht es Shizuku (ohne Root) oder Root.",
                    GapFix.SHIZUKU,
                )
            }
            emit("Tiefenprüfung", "Nur mit Shizuku oder Root", 0.88f, RadarSector.DEEP)
        }
        val gapsForIocs = CheckResult("Indikatoren")
        iocs.status.missing.takeIf { it.isNotEmpty() }?.let { m ->
            gapsForIocs.gap("${m.size} Indikator-Quellen sind noch nicht geladen (${m.joinToString { it.name }}).", GapFix.UPDATE_INDICATORS)
        }
        iocs.status.corrupt.takeIf { it.isNotEmpty() }?.let { c ->
            gapsForIocs.gap("${c.size} Indikator-Quellen sind beschädigt und wurden nicht benutzt (${c.joinToString { it.name }}).", GapFix.UPDATE_INDICATORS)
        }
        if (gapsForIocs.gaps.isNotEmpty()) checks += gapsForIocs

        emit("Fertig", "", 1f, RadarSector.DEEP)
        Report(
            startedAt = started,
            finishedAt = System.currentTimeMillis(),
            access = level,
            indicatorCount = set.size,
            indicatorSources = iocs.status.available,
            indicatorSourcesMissing = iocs.status.missing.size + iocs.status.corrupt.size,
            checks = checks,
            device = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (Patch ${Build.VERSION.SECURITY_PATCH})",
        )
    }

    private fun storageCheck(
        tracker: RadarTracker,
        emit: (String, String, Float, RadarSector) -> Unit,
    ): List<CheckResult> {
        val storage = StorageCollector()
        if (!storage.canRead()) {
            tracker.replaceSector(RadarSector.FILES, emptyList())
            emit("Dateien", "Keine Berechtigung", 0.55f, RadarSector.FILES)
            return listOf(CheckResult(ShellAnalyzer.FILES).apply {
                gap("Dateien im Speicher (Downloads, Messenger-Anhänge) wurden nicht geprüft: Die Berechtigung „Zugriff auf alle Dateien“ fehlt.", GapFix.PERMISSION_FILES)
            })
        }
        tracker.replaceSector(RadarSector.FILES, emptyList())
        emit("Dateien", "Gemeinsamer Speicher …", 0.55f, RadarSector.FILES)
        var hashed = 0
        val res = storage.collect {
            hashed++
            tracker.appendClean(RadarSector.FILES)
            emit("Dateien", it, 0.55f + 0.15f * hashed.coerceAtMost(500) / 500f, RadarSector.FILES)
        }
        val files = ShellAnalyzer.files(res.listing, iocs.set)
        val hashes = ShellAnalyzer.hashes(res.hashes, iocs.set)
        if (res.skippedLarge > 0) hashes.gap("${res.skippedLarge} Datei(en) über 120 MB wurden nicht mit den Fingerabdrücken verglichen.")
        if (res.unreadable > 0) hashes.gap("${res.unreadable} Datei(en) oder Ordner waren nicht lesbar (z. B. Android/data).", GapFix.SHIZUKU)
        val listingPaths = res.listing.lineSequence().filter { it.startsWith("/") }.toList()
        tracker.replaceSector(RadarSector.FILES, RadarMath.filePathStatuses(listingPaths, files, hashes))
        emit("Dateien", "${listingPaths.size} Pfade ausgewertet", 0.72f, RadarSector.FILES)
        return listOf(files, hashes)
    }

    private fun shellChecks(
        shell: Shell,
        installed: Set<String>,
        tracker: RadarTracker,
        emit: (String, String, Float, RadarSector) -> Unit,
    ): List<CheckResult> {
        val out = ArrayList<CheckResult>()
        val root = shell.level == AccessLevel.ROOT
        tracker.replaceSector(RadarSector.DEEP, emptyList())

        emit("Prozesse", "Laufende Prozesse …", 0.52f, RadarSector.DEEP)
        val processes = ShellAnalyzer.processes(shell.read("ps -A -o NAME", 4 shl 20, 30).orEmpty(), iocs.set)
        out += processes
        tracker.appendSector(RadarSector.DEEP, RadarMath.statusesFromCheck(processes))

        emit("Pakete", "Alle Pakete, auch gelöschte …", 0.55f, RadarSector.DEEP)
        val packages = ShellAnalyzer.packages(
            shell.read("pm list packages -u -i", 4 shl 20, 60).orEmpty(),
            installed,
            iocs.set,
        )
        out += packages
        tracker.appendSector(RadarSector.DEEP, RadarMath.statusesFromCheck(packages))

        val logCommands = LOG_COMMANDS + if (root) ROOT_LOG_COMMANDS else emptyList()
        val logSources = logCommands.mapIndexed { i, (label, cmd) ->
            label to ShellAnalyzer.LineSource { onLine ->
                emit("Systemprotokolle", label, 0.58f + 0.17f * i / logCommands.size.coerceAtLeast(1), RadarSector.DEEP)
                shell.readLines(cmd, 32 shl 20, 180, onLine)
            }
        }
        val logs = ShellAnalyzer.logs(logSources, iocs.set, installed)
        out += logs
        tracker.appendSector(RadarSector.DEEP, RadarMath.statusesFromCheck(logs))

        emit("Dateien", "Dateiliste …", 0.76f, RadarSector.FILES)
        tracker.replaceSector(RadarSector.FILES, emptyList())
        val listRoots = if (root) SHELL_FILE_ROOTS + ROOT_FILE_ROOTS else SHELL_FILE_ROOTS
        val listCmd = "find ${listRoots.joinToString(" ")} -type f 2>/dev/null"
        val listedPaths = ArrayList<String>()
        val files = ShellAnalyzer.files(
            ShellAnalyzer.LineSource { onLine ->
                shell.readLines(listCmd, 64 shl 20, 300) { line ->
                    val path = line.trim()
                    if (path.startsWith("/")) {
                        listedPaths.add(path)
                        tracker.appendClean(RadarSector.FILES)
                    }
                    onLine(line)
                }
            },
            iocs.set,
        )
        out += files
        tracker.replaceSector(RadarSector.FILES, RadarMath.filePathStatuses(listedPaths, files))

        emit("Fingerabdrücke", "Programmdateien werden gehasht …", 0.82f, RadarSector.DEEP)
        val hashRoots = if (root) SHELL_HASH_ROOTS + ROOT_HASH_ROOTS else SHELL_HASH_ROOTS
        val exec = if (root) " -o -perm -u+x" else ""
        val names = StorageCollector.CANDIDATES.joinToString(" -o ") { "-iname '*.$it'" }
        val hashCmd = "find ${hashRoots.joinToString(" ")} -type f -size -122880k \\( $names$exec \\) -exec sha256sum {} + 2>/dev/null"
        val hashes = ShellAnalyzer.hashes(ShellAnalyzer.LineSource { shell.readLines(hashCmd, 64 shl 20, 900, it) }, iocs.set)
        out += hashes
        tracker.appendSector(RadarSector.DEEP, RadarMath.statusesFromCheck(hashes))

        if (!root) {
            out += CheckResult("Root-Prüfung").apply {
                gap("App-Datenordner (/data/data), Absturzberichte (/data/tombstones) und Systemdateien wurden nicht geprüft. Dafür braucht es Root.", GapFix.ROOT)
            }
        }
        emit("Tiefenprüfung", "Tiefenprüfung ausgewertet", 0.9f, RadarSector.DEEP)
        return out
    }

    companion object {
        const val SHELL_DEAD = "Die Tiefenprüfung lief nicht: Shizuku bzw. Root ist verbunden, antwortet aber nicht. " +
            "Shizuku neu starten (oder Root erneut erlauben) und noch einmal prüfen."

        val LOG_COMMANDS = listOf(
            "logcat" to "logcat -d -b all -v brief",
            "dumpsys package" to "dumpsys package",
            "dumpsys accessibility" to "dumpsys accessibility",
            "dumpsys notification" to "dumpsys notification --noredact",
            "dumpsys usagestats" to "dumpsys usagestats",
            "dumpsys batterystats" to "dumpsys batterystats --history",
            "dumpsys connectivity" to "dumpsys connectivity",
            "dumpsys dropbox" to "dumpsys dropbox --print",
            "dumpsys appops" to "dumpsys appops",
            "dumpsys dbinfo" to "dumpsys dbinfo",
        )
        val ROOT_LOG_COMMANDS = listOf(
            "Absturzberichte" to "cat /data/tombstones/* 2>/dev/null",
            "packages.xml" to "cat /data/system/packages.xml 2>/dev/null",
        )
        val SHELL_FILE_ROOTS = listOf("/sdcard/", "/data/local/tmp/")
        val ROOT_FILE_ROOTS = listOf("/data/data/", "/data/user_de/", "/data/system/", "/data/misc/", "/data/adb/", "/data/local/", "/system/", "/vendor/")
        val SHELL_HASH_ROOTS = listOf("/data/local/tmp/", "/sdcard/")
        val ROOT_HASH_ROOTS = listOf("/data/data/", "/data/app/", "/data/adb/", "/data/local/")
    }
}
