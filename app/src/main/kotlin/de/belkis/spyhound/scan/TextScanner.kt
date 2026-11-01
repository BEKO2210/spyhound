package de.belkis.spyhound.scan

import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocType

/**
 * Finds indicator values inside free text (logs, dumpsys, SMS bodies).
 * Host names are extracted as whole tokens and walked up to their parents, so
 * "evil.example" matches "cdn.evil.example" but never "notevil.example".
 */
class TextScanner(private val iocs: IocSet) {
    data class Match(val indicator: Indicator, val token: String)

    fun hosts(text: CharSequence): List<Match> {
        val seen = HashSet<String>()
        val out = ArrayList<Match>()
        for (m in HOST.findAll(text)) {
            val token = m.value.lowercase().trimEnd('.')
            if (!seen.add(token)) continue
            val ind = iocs.lookupHost(token) ?: continue
            out += Match(ind, token)
        }
        return out
    }

    /** Package names (app ids) as whole dotted tokens. */
    fun appIds(text: CharSequence): List<Match> {
        val seen = HashSet<String>()
        val out = ArrayList<Match>()
        for (m in PACKAGE.findAll(text)) {
            val token = m.value
            if (!seen.add(token)) continue
            val ind = iocs.lookup(IocType.APP_ID, token) ?: continue
            out += Match(ind, token)
        }
        return out
    }

    /** Complete absolute paths, never a prefix ("/data/x" does not match "/data/xy"). */
    fun paths(text: CharSequence): List<Match> {
        val seen = HashSet<String>()
        val out = ArrayList<Match>()
        for (m in PATH.findAll(text)) {
            val token = m.value.trimEnd('.', ',', ';', ':')
            if (!seen.add(token)) continue
            val ind = iocs.lookup(IocType.FILE_PATH, token) ?: continue
            out += Match(ind, token)
        }
        return out
    }

    fun urls(text: CharSequence): List<Match> {
        if (iocs.count(IocType.URL) == 0) return emptyList()
        val lower = text.toString().lowercase()
        return iocs.all(IocType.URL).filter { lower.contains(it.value) }.map { Match(it, it.value) }
    }

    fun all(text: CharSequence): List<Match> = hosts(text) + appIds(text) + paths(text) + urls(text)

    companion object {
        private val HOST = Regex("""(?<![A-Za-z0-9_-])(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}(?![A-Za-z0-9_-])""")
        private val PACKAGE = Regex("""(?<![A-Za-z0-9_.])[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+(?![A-Za-z0-9_])""")
        private val PATH = Regex("""(?<![A-Za-z0-9_.-])/[A-Za-z0-9_.@+-]+(?:/[A-Za-z0-9_.@+-]+)*""")
    }
}
