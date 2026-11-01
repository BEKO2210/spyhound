package de.belkis.spyhound.ui

import android.content.Context

object IntroPreferences {
    private const val PREFS = "spyhound_intro"
    private const val KEY_COMPLETED = "completed"

    fun hasCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_COMPLETED, false)

    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_COMPLETED, true).apply()
    }
}
