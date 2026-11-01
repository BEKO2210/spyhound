package de.belkis.spyhound.shell

import android.content.Context

/** Remembers that the user successfully used root at least once (silent re-probe on next start). */
object ShellPreferences {
    private const val PREFS = "spyhound_shell"
    private const val KEY_ROOT_GRANTED = "root_once_granted"

    fun wasRootGranted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ROOT_GRANTED, false)

    fun setRootGranted(context: Context, granted: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ROOT_GRANTED, granted).apply()
    }
}

/**
 * During an active scan the live [Shell] reference must not change; defer swaps until [scanFinished].
 */
class ScanGatedShellChanges {
    var scanInProgress: Boolean = false
        private set

    var pendingClearShell: Boolean = false
        private set

    var pendingBanner: String? = null
        private set

    var pendingNewShell: Boolean = false
        private set

    val hasDeferredWork: Boolean
        get() = pendingClearShell || pendingNewShell || pendingBanner != null

    fun scanStarted() {
        scanInProgress = true
    }

    /** @return work to apply after the scan, or null if nothing was deferred */
    fun scanFinished(): DeferredShellWork? {
        scanInProgress = false
        if (!hasDeferredWork) return null
        val work = DeferredShellWork(
            clearShell = pendingClearShell,
            applyPendingShell = pendingNewShell && !pendingClearShell,
            bannerMessage = pendingBanner,
        )
        pendingClearShell = false
        pendingNewShell = false
        pendingBanner = null
        return work
    }

    /** @return true when the change can run immediately */
    fun deferClearShell(banner: String? = null): Boolean {
        if (!scanInProgress) return true
        pendingClearShell = true
        if (banner != null) pendingBanner = banner
        return false
    }

    /** @return true when the new shell can be adopted immediately */
    fun deferAdoptNewShell(): Boolean {
        if (!scanInProgress) return true
        pendingNewShell = true
        return false
    }

    data class DeferredShellWork(
        val clearShell: Boolean,
        val applyPendingShell: Boolean,
        val bannerMessage: String?,
    )
}

const val SHIZUKU_STOPPED_BANNER =
    "Shizuku wurde beendet – nach Neustart des Telefons neu starten"
