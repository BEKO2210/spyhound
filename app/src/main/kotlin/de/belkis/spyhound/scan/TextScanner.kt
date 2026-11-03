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

    /** IPv4 addresses as whole tokens (IPv6 is not extracted: its text form is too ambiguous in logs). */
    fun ips(text: CharSequence): List<Match> {
        val seen = HashSet<String>()
        val out = ArrayList<Match>()
        for (m in IPV4.findAll(text)) {
            val token = m.value
            if (!seen.add(token)) continue
            val ind = iocs.lookup(IocType.IP, token) ?: continue
            out += Match(ind, token)
        }
        return out
    }

    /**
     * Links are cut out of the text and looked up directly, never searched for in the whole text.
     * A link matches an indicator URL when it equals it or lies below it ("/a" matches "/a/b?x=1", not "/ab").
     */
    fun urls(text: CharSequence): List<Match> {
        if (iocs.count(IocType.URL) == 0) return emptyList()
        val seen = HashSet<String>()
        val out = ArrayList<Match>()
        for (m in URL.findAll(text)) {
            val token = IocSet.normaliseUrl(m.value.trimEnd('.', ',', ';', ':', '!', '?'))
            if (!seen.add(token)) continue
            val ind = lookupUrl(token) ?: continue
            if (seen.add("ioc:" + ind.value)) out += Match(ind, token)
        }
        return out
    }

    private fun lookupUrl(token: String): Indicator? {
        val schemeEnd = token.indexOf("://") + 3
        val withoutFrag = token.substringBefore('#')
        if ('?' in withoutFrag) {
            iocs.lookup(IocType.URL, withoutFrag)?.let { return it }
            iocs.lookup(IocType.URL, withoutFrag.substring(schemeEnd))?.let { return it }
        }
        var cur = withoutFrag.substringBefore('?')
        while (true) {
            val bare = cur.trimEnd('/')
            if (bare.length <= schemeEnd) return null
            iocs.lookup(IocType.URL, bare)?.let { return it }
            iocs.lookup(IocType.URL, "$bare/")?.let { return it }
            iocs.lookup(IocType.URL, bare.substring(schemeEnd))?.let { return it } // feeds that omit the scheme
            val cut = bare.lastIndexOf('/')
            if (cut <= schemeEnd) return null
            cur = bare.substring(0, cut)
        }
    }

    fun all(text: CharSequence): List<Match> = hosts(text) + appIds(text) + paths(text) + ips(text) + urls(text)

    companion object {
        private val HOST = Regex("""(?<![A-Za-z0-9_-])(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}(?![A-Za-z0-9_-])""")
        private val PACKAGE = Regex("""(?<![A-Za-z0-9_.])[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+(?![A-Za-z0-9_])""")
        private val PATH = Regex("""(?<![A-Za-z0-9_.-])/[A-Za-z0-9_.@+-]+(?:/[A-Za-z0-9_.@+-]+)*""")
        private val IPV4 = Regex("""(?<![0-9.])(?:[0-9]{1,3}\.){3}[0-9]{1,3}(?![0-9]|\.[0-9])""")
        private val URL = Regex("""https?://[^\s"'<>(){}\[\]\\]+""", RegexOption.IGNORE_CASE)
    }
}
