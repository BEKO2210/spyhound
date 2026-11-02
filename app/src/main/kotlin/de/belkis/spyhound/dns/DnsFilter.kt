package de.belkis.spyhound.dns

import android.content.Context
import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.DomainLists
import de.belkis.spyhound.ioc.IocSet

/**
 * Per-category switches of the DNS watcher. Spyware indicators (mercenary, stalkerware, malware) have no switch:
 * they always warn and are never blocked, because a blocked lookup would hide the very trace a person may need
 * as evidence, and an abuser could notice that the app interferes.
 */
data class DnsFilterSettings(
    val warnPhishing: Boolean = true,
    val warnTracker: Boolean = false,
    val blockPhishing: Boolean = false,
    val blockTracker: Boolean = false,
) {
    fun warns(category: Category): Boolean = when (category) {
        Category.PHISHING -> warnPhishing
        Category.TRACKER -> warnTracker
        else -> true
    }

    fun blocks(category: Category): Boolean = when (category) {
        Category.PHISHING -> blockPhishing
        Category.TRACKER -> blockTracker
        else -> false
    }
}

data class DnsMatch(val name: String, val category: Category, val family: String, val blocked: Boolean)

/** What to do with one DNS query: [matches] to report, and whether to answer it locally with NXDOMAIN. */
data class DnsVerdict(val matches: List<DnsMatch>, val block: Boolean) {
    companion object {
        val PASS = DnsVerdict(emptyList(), false)
    }
}

object DnsFilter {
    /** Decides for all names of one query; pure, so it runs the same on a phone and in a unit test. */
    fun evaluate(names: List<String>, spyware: IocSet, lists: DomainLists, settings: DnsFilterSettings): DnsVerdict {
        val evidence = ArrayList<DnsMatch>()
        val other = ArrayList<DnsMatch>()
        for (name in names) {
            spyware.lookupHost(name)?.let { evidence += DnsMatch(name, it.category, it.family, blocked = false) }
            for (category in LISTED) {
                if (!settings.warns(category) && !settings.blocks(category)) continue
                lists.lookup(category, name)?.let {
                    other += DnsMatch(name, category, it.family, blocked = settings.blocks(category))
                }
            }
        }
        // A query that touches a spyware indicator is always answered normally.
        if (evidence.isNotEmpty()) return DnsVerdict(evidence + other.map { it.copy(blocked = false) }, block = false)
        return DnsVerdict(other, block = other.any { it.blocked })
    }

    private val LISTED = listOf(Category.PHISHING, Category.TRACKER)
}

/** Stores [DnsFilterSettings] app-privately; defaults are phishing warnings on, everything else off. */
object DnsFilterPrefs {
    private const val PREFS = "spyhound_dns_filter"
    private const val WARN_PHISHING = "warn_phishing"
    private const val WARN_TRACKER = "warn_tracker"
    private const val BLOCK_PHISHING = "block_phishing"
    private const val BLOCK_TRACKER = "block_tracker"
    private val DEFAULTS = DnsFilterSettings()

    fun load(context: Context): DnsFilterSettings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return DnsFilterSettings(
            warnPhishing = p.getBoolean(WARN_PHISHING, DEFAULTS.warnPhishing),
            warnTracker = p.getBoolean(WARN_TRACKER, DEFAULTS.warnTracker),
            blockPhishing = p.getBoolean(BLOCK_PHISHING, DEFAULTS.blockPhishing),
            blockTracker = p.getBoolean(BLOCK_TRACKER, DEFAULTS.blockTracker),
        )
    }

    fun save(context: Context, settings: DnsFilterSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(WARN_PHISHING, settings.warnPhishing)
            .putBoolean(WARN_TRACKER, settings.warnTracker)
            .putBoolean(BLOCK_PHISHING, settings.blockPhishing)
            .putBoolean(BLOCK_TRACKER, settings.blockTracker)
            .apply()
    }
}
