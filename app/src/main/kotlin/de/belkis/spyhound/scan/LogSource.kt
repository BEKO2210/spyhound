package de.belkis.spyhound.scan

import de.belkis.spyhound.shell.Shell

/** Reads one system log command line by line, reports throttled progress and ends it at a hard time limit. */
object LogSource {
    const val LOGCAT_TIMEOUT_SEC = 45L
    const val DEFAULT_TIMEOUT_SEC = 180L
    private const val PROGRESS_INTERVAL_MS = 1000L

    fun timeoutFor(label: String): Long = if (label == "logcat") LOGCAT_TIMEOUT_SEC else DEFAULT_TIMEOUT_SEC

    /** Detail text for the step: the log's name and how many lines were read so far. */
    fun progressDetail(label: String, lines: Long): String = "$label · $lines Zeilen"

    /**
     * False if the shell ended the command early (limit reached or aborted); the lines read until then were still
     * delivered, so the caller records an honest gap instead of waiting longer.
     */
    fun read(
        shell: Shell,
        label: String,
        command: String,
        onLine: (String) -> Unit,
        onProgress: (String) -> Unit,
        now: () -> Long = System::currentTimeMillis,
    ): Boolean {
        var lines = 0L
        var lastReport = now()
        onProgress(progressDetail(label, 0))
        return shell.readLines(command, 32 shl 20, timeoutFor(label)) { line ->
            lines++
            val t = now()
            if (t - lastReport >= PROGRESS_INTERVAL_MS) {
                lastReport = t
                onProgress(progressDetail(label, lines))
            }
            onLine(line)
        }
    }
}
