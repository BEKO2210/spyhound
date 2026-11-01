package de.belkis.spyhound.dns

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Public entry points for the local DNS watcher (no remote VPN server). */
object DnsGuard {
    const val ACTION_START = "de.belkis.spyhound.dns.START"
    const val ACTION_STOP = "de.belkis.spyhound.dns.STOP"

    private val _running = MutableStateFlow(false)

    /** Live state for the UI: the tunnel comes up and goes down asynchronously after start/stop. */
    val running: StateFlow<Boolean> = _running

    var isRunning: Boolean
        get() = _running.value
        internal set(value) { _running.value = value }

    /**
     * Starts the watcher after [VpnService.prepare], or opens the system VPN approval sheet if needed.
     * Call again from [Activity.onActivityResult] / the activity result launcher when the user approved VPN.
     */
    fun start(activity: Activity) {
        val prep = VpnService.prepare(activity)
        if (prep != null) {
            activity.startActivity(prep)
            return
        }
        ContextCompat.startForegroundService(
            activity,
            Intent(activity, DnsGuardService::class.java).setAction(ACTION_START),
        )
    }

    /**
     * The system keeps a VpnService with an open tunnel bound, so stopService() alone never ends it.
     * The service closes its tunnel itself on [ACTION_STOP].
     */
    fun stop(context: Context) {
        if (!isRunning) return
        context.startService(Intent(context, DnsGuardService::class.java).setAction(ACTION_STOP))
    }

    fun recentHits(context: Context): List<DnsHit> = DnsHitStore.recent(context)
}
