package de.belkis.spyhound.scan

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

/** Interprets oldest observed evidence timestamps. Missing sources stay explicitly unknown. */
object TimeRange {
    const val SMS = "sms"
    const val LOGS = "logs"
    const val FILES = "files"
    const val APPS = "apps"
    const val LOG_WARNING_HOURS = 48L

    /** Parses Android logcat brief timestamps (MM-DD HH:mm:ss.mmm), using current year. */
    fun logcatTimestamp(line: String, now: Long): Long? {
        val match = Regex("^\\s*(\\d{2})-(\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})").find(line) ?: return null
        val (month, day, time) = match.destructured
        return runCatching {
            val c = Calendar.getInstance().apply { timeInMillis = now }
            val year = c.get(Calendar.YEAR)
            val parsed = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).parse("$year-$month-$day $time")!!.time
            // Logs spanning New Year can begin in prior year.
            if (parsed > now) Calendar.getInstance().apply { timeInMillis = parsed; add(Calendar.YEAR, -1) }.timeInMillis else parsed
        }.getOrNull()
    }

    data class SourceRange(val source: String, val text: String, val warning: Boolean = false)
    data class Summary(val sources: List<SourceRange>, val warning: Boolean) {
        val hasEvidence get() = sources.isNotEmpty()
    }

    fun evaluate(oldestEvidence: Map<String, Long>, now: Long, locale: Locale = Locale.GERMAN): Summary {
        val date = SimpleDateFormat("dd.MM.yyyy", locale)
        val sources = listOf(SMS, LOGS, FILES, APPS).mapNotNull { key ->
            val timestamp = oldestEvidence[key]?.takeIf { it > 0 && it <= now } ?: return@mapNotNull null
            val hours = ((now - timestamp) / 3_600_000L).coerceAtLeast(0)
            val logs = key == LOGS
            val name = when (key) {
                SMS -> if (locale.language == "en") "SMS" else "SMS"
                LOGS -> if (locale.language == "en") "System logs" else "Systemprotokolle"
                FILES -> if (locale.language == "en") "Files" else "Dateien"
                else -> if (locale.language == "en") "App installations" else "App-Installationen"
            }
            val text = if (logs) {
                if (locale.language == "en") "$name: last $hours hours only" else "$name: nur letzte $hours Stunden"
            } else {
                val prefix = if (locale.language == "en") "$name: reaches back to " else "$name: reicht bis "
                prefix + date.format(Date(timestamp))
            }
            SourceRange(key, text, logs && hours < LOG_WARNING_HOURS)
        }
        return Summary(sources, sources.any { it.warning })
    }
}
