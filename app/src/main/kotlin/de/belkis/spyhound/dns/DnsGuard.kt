package de.belkis.spyhound.dns

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat

/** Public entry points for the local DNS watcher (no remote VPN server). */
object DnsGuard {
    const val ACTION_START = "de.belkis.spyhound.dns.START"

    @Volatile
    var isRunning: Boolean = false
        internal set

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

    fun stop(context: Context) {
        context.stopService(Intent(context, DnsGuardService::class.java))
    }

    fun recentHits(context: Context): List<DnsHit> = DnsHitStore.recent(context)
}
