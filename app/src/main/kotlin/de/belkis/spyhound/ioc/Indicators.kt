package de.belkis.spyhound.ioc

/** What an indicator describes. Only these kinds are compared on an Android device. */
enum class IocType {
    DOMAIN, IP, URL, SHA256, APP_ID, APP_CERT, PROCESS, FILE_PATH, FILE_NAME, ANDROID_PROP, EMAIL
}

/** Mercenary spyware (state-grade) or commercial stalkerware: they read very differently to a user. */
enum class Category { MERCENARY, STALKERWARE }

data class Indicator(
    val value: String,
    val type: IocType,
    val category: Category,
    val family: String,
    val sourceId: String,
    /** False for sources that only document iPhone spyware: their app ids and process names mean nothing on Android. */
    val androidArtifacts: Boolean = true,
)

/** The loaded indicators, indexed per kind for exact lookups. */
class IocSet(indicators: Iterable<Indicator>) {
    private val byType: Map<IocType, Map<String, Indicator>>
    val size: Int

    init {
        val maps = IocType.entries.associateWith { HashMap<String, Indicator>() }
        var n = 0
        for (ind in indicators) {
            val key = normalise(ind.type, ind.value) ?: continue
            if (ind.type == IocType.DOMAIN && SharedServices.isShared(key)) continue
            // First source wins, so the curated lists (loaded first) keep their attribution.
            if (maps.getValue(ind.type).putIfAbsent(key, ind.copy(value = key)) == null) n++
        }
        byType = maps
        size = n
    }

    fun lookup(type: IocType, value: String): Indicator? {
        val key = normalise(type, value) ?: return null
        val hit = byType[type]?.get(key) ?: return null
        if (!hit.androidArtifacts && type in IOS_SENSITIVE) return null
        return hit
    }

    /** The most specific indicator domain that [host] is, or is a subdomain of. */
    fun lookupHost(host: String): Indicator? {
        var h = normalise(IocType.DOMAIN, host) ?: return null
        val domains = byType.getValue(IocType.DOMAIN)
        while (true) {
            domains[h]?.let { return it }
            val dot = h.indexOf('.')
            if (dot < 0 || h.indexOf('.', dot + 1) < 0) return null // never match a bare TLD
            h = h.substring(dot + 1)
        }
    }

    fun all(type: IocType): Collection<Indicator> = byType.getValue(type).values

    fun count(type: IocType): Int = byType.getValue(type).size

    companion object {
        private val IOS_SENSITIVE = setOf(IocType.APP_ID, IocType.PROCESS, IocType.FILE_PATH, IocType.FILE_NAME)

        fun normalise(type: IocType, raw: String): String? {
            val v = raw.trim()
            if (v.isEmpty()) return null
            return when (type) {
                IocType.DOMAIN -> v.lowercase().trimEnd('.').removePrefix("*.").takeIf { '.' in it }
                IocType.SHA256 -> v.lowercase().takeIf { SHA256.matches(it) }
                IocType.APP_CERT -> v.replace(":", "").lowercase().takeIf { SHA1.matches(it) || SHA256.matches(it) }
                IocType.IP, IocType.EMAIL, IocType.URL -> v.lowercase()
                else -> v
            }
        }

        private val SHA256 = Regex("[0-9a-f]{64}")
        private val SHA1 = Regex("[0-9a-f]{40}")
    }
}

/**
 * Whole public services on which only single links or accounts were malicious.
 * A bare match on these would accuse every phone that ever opened a short link.
 */
object SharedServices {
    private val HOSTS = """
        bit.ly bitly.com j.mp tinyurl.com goo.gl t.co ow.ly is.gd v.gd buff.ly rebrand.ly
        cutt.ly shorturl.at tiny.cc lnkd.in t.ly rb.gy s.id
        google.com googleusercontent.com googleapis.com storage.googleapis.com docs.google.com
        drive.google.com sites.google.com blogspot.com dropbox.com dropboxusercontent.com
        dl.dropboxusercontent.com github.com gist.github.com api.github.com github.io
        githubusercontent.com raw.githubusercontent.com gist.githubusercontent.com
        objects.githubusercontent.com pastebin.com mediafire.com mega.nz wetransfer.com
        facebook.com fb.com instagram.com twitter.com x.com t.me telegram.me telegram.org
        wa.me whatsapp.com youtube.com youtu.be
    """.trim().split(Regex("\\s+")).toSet()

    fun isShared(host: String): Boolean {
        val h = host.lowercase().trimEnd('.')
        return h in HOSTS || (h.startsWith("www.") && h.substring(4) in HOSTS)
    }
}
