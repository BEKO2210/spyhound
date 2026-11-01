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

data class Progress(val step: String, val detail: String, val fraction: Float)

/** Runs every check the current access level allows, and records every one it could not. */
class ScanEngine(private val context: Context, private val iocs: LoadedIocs, private val shell: Shell?) {
    private val level = shell?.level ?: AccessLevel.APP

    suspend fun run(onProgress: (Progress) -> Unit): Report = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        val set = iocs.set
        val checks = ArrayList<CheckResult>()

        onProgress(Progress("Apps", "Installierte Apps werden gelesen …", 0f))
        val apps = AppCollector(context).collect { done, total, label ->
            onProgress(Progress("Apps", label, 0.45f * done / total.coerceAtLeast(1)))
        }
        checks += AppAnalyzer.analyze(apps, set, context.packageName)
        coroutineContext.ensureActive()

        onProgress(Progress("Gerät", "Einstellungen und Systemeigenschaften …", 0.46f))
        var device = DeviceCollector(context).collect(apps)
        shell?.read("getprop", 4 shl 20, 30)?.let { full ->
            device = device.copy(props = device.props + DeviceCollector.parseGetprop(full))
        }
        checks += DeviceAnalyzer.analyze(device, set, shellExpected = level == AccessLevel.SHELL)

        onProgress(Progress("SMS", "Links in Nachrichten …", 0.5f))
        checks += MessageAnalyzer.analyze(SmsCollector(context).collect(), set)
        coroutineContext.ensureActive()

        if (shell != null) {
            checks += shellChecks(shell, apps.map { it.packageName }.toSet(), onProgress)
        } else {
            checks += storageCheck(onProgress)
            checks += CheckResult("Tiefenprüfung").apply {
                gap(
                    "Laufende Prozesse, Systemprotokolle (logcat, dumpsys), gelöschte Apps und Dateien außerhalb des " +
                        "gemeinsamen Speichers wurden nicht geprüft. Dafür braucht es Shizuku (ohne Root) oder Root.",
                    GapFix.SHIZUKU,
                )
            }
        }
        val gapsForIocs = CheckResult("Indikatoren")
        iocs.status.missing.takeIf { it.isNotEmpty() }?.let { m ->
            gapsForIocs.gap("${m.size} Indikator-Quellen sind noch nicht geladen (${m.joinToString { it.name }}).", GapFix.UPDATE_INDICATORS)
        }
        iocs.status.corrupt.takeIf { it.isNotEmpty() }?.let { c ->
            gapsForIocs.gap("${c.size} Indikator-Quellen sind beschädigt und wurden nicht benutzt (${c.joinToString { it.name }}).", GapFix.UPDATE_INDICATORS)
        }
        if (gapsForIocs.gaps.isNotEmpty()) checks += gapsForIocs

        onProgress(Progress("Fertig", "", 1f))
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

    private fun storageCheck(onProgress: (Progress) -> Unit): List<CheckResult> {
        val storage = StorageCollector()
        if (!storage.canRead()) {
            return listOf(CheckResult(ShellAnalyzer.FILES).apply {
                gap("Dateien im Speicher (Downloads, Messenger-Anhänge) wurden nicht geprüft: Die Berechtigung „Zugriff auf alle Dateien“ fehlt.", GapFix.PERMISSION_FILES)
            })
        }
        onProgress(Progress("Dateien", "Gemeinsamer Speicher …", 0.55f))
        val res = storage.collect { onProgress(Progress("Dateien", it, 0.7f)) }
        val files = ShellAnalyzer.files(res.listing, iocs.set)
        val hashes = ShellAnalyzer.hashes(res.hashes, iocs.set)
        if (res.skippedLarge > 0) hashes.gap("${res.skippedLarge} Datei(en) über 120 MB wurden nicht mit den Fingerabdrücken verglichen.")
        if (res.unreadable > 0) hashes.gap("${res.unreadable} Datei(en) oder Ordner waren nicht lesbar (z. B. Android/data).", GapFix.SHIZUKU)
        return listOf(files, hashes)
    }

    private fun shellChecks(shell: Shell, installed: Set<String>, onProgress: (Progress) -> Unit): List<CheckResult> {
        val out = ArrayList<CheckResult>()
        val root = shell.level == AccessLevel.ROOT

        onProgress(Progress("Prozesse", "Laufende Prozesse …", 0.52f))
        out += ShellAnalyzer.processes(shell.read("ps -A -o NAME", 4 shl 20, 30).orEmpty(), iocs.set)

        onProgress(Progress("Pakete", "Alle Pakete, auch gelöschte …", 0.55f))
        out += ShellAnalyzer.packages(shell.read("pm list packages -u -i", 4 shl 20, 60).orEmpty(), installed, iocs.set)

        val logCommands = LOG_COMMANDS + if (root) ROOT_LOG_COMMANDS else emptyList()
        // Each log is streamed through the scanner while it is read; none is ever held whole.
        val logSources = logCommands.mapIndexed { i, (label, cmd) ->
            label to ShellAnalyzer.LineSource { onLine ->
                onProgress(Progress("Systemprotokolle", label, 0.58f + 0.17f * i / logCommands.size))
                shell.readLines(cmd, 32 shl 20, 180, onLine)
            }
        }
        out += ShellAnalyzer.logs(logSources, iocs.set, installed)

        onProgress(Progress("Dateien", "Dateiliste …", 0.76f))
        val listRoots = if (root) SHELL_FILE_ROOTS + ROOT_FILE_ROOTS else SHELL_FILE_ROOTS
        val listCmd = "find ${listRoots.joinToString(" ")} -type f 2>/dev/null"
        out += ShellAnalyzer.files(ShellAnalyzer.LineSource { shell.readLines(listCmd, 64 shl 20, 300, it) }, iocs.set)

        onProgress(Progress("Fingerabdrücke", "Programmdateien werden gehasht …", 0.82f))
        val hashRoots = if (root) SHELL_HASH_ROOTS + ROOT_HASH_ROOTS else SHELL_HASH_ROOTS
        val exec = if (root) " -o -perm -u+x" else ""
        val names = StorageCollector.CANDIDATES.joinToString(" -o ") { "-iname '*.$it'" }
        val hashCmd = "find ${hashRoots.joinToString(" ")} -type f -size -122880k \\( $names$exec \\) -exec sha256sum {} + 2>/dev/null"
        out += ShellAnalyzer.hashes(ShellAnalyzer.LineSource { shell.readLines(hashCmd, 64 shl 20, 900, it) }, iocs.set)

        if (!root) {
            out += CheckResult("Root-Prüfung").apply {
                gap("App-Datenordner (/data/data), Absturzberichte (/data/tombstones) und Systemdateien wurden nicht geprüft. Dafür braucht es Root.", GapFix.ROOT)
            }
        }
        return out
    }

    companion object {
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
