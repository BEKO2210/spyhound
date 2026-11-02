package de.belkis.spyhound.background

import android.content.Context

object BackgroundScanPreferences {
    private const val PREFS = "spyhound_background_scan"
    private const val KEY_WEEKLY_ENABLED = "weekly_enabled"
    private const val KEY_LAST_FINDING_KEYS = "last_finding_keys"

    fun isWeeklyEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WEEKLY_ENABLED, false)

    fun setWeeklyEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_WEEKLY_ENABLED, enabled).apply()
    }

    fun getLastFindingKeys(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_LAST_FINDING_KEYS, emptySet()) ?: emptySet()

    fun setLastFindingKeys(context: Context, keys: Set<String>) {
        prefs(context).edit().putStringSet(KEY_LAST_FINDING_KEYS, keys).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
