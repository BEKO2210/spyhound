package de.belkis.spyhound.ioc

/**
 * "Is there a newer Spyhound?" Asked only together with a source update the user started, on the same host.
 * The answer is a version number and nothing else: the app never downloads or installs an update itself.
 */
object AppRelease {
    const val LATEST_URL = "https://api.github.com/repos/BEKO2210/spyhound/releases/latest"
    const val DOWNLOAD_PAGE = "https://github.com/BEKO2210/spyhound/releases/latest"

    private val TAG = Regex(""""tag_name"\s*:\s*"v?(\d{1,4}(?:\.\d{1,4}){1,3})"""")
    private val NOT_FINAL = Regex(""""(?:draft|prerelease)"\s*:\s*true""")

    /** The version of the newest published release, or null if the answer is not a plain final release. */
    fun latestVersion(json: String): String? {
        if (NOT_FINAL.containsMatchIn(json)) return null
        val tags = TAG.findAll(json).map { it.groupValues[1] }.toList()
        // One release has one tag. More than one means this is not the answer we asked for.
        return tags.singleOrNull()
    }

    /** True only if [latest] is a higher version than [installed]; anything unreadable counts as "not newer". */
    fun isNewer(latest: String, installed: String): Boolean {
        val a = numbers(latest) ?: return false
        val b = numbers(installed) ?: return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun numbers(version: String): List<Int>? =
        Regex("""^v?(\d{1,4}(?:\.\d{1,4}){1,3})""").find(version.trim())?.groupValues?.get(1)?.split('.')?.map { it.toInt() }
}
