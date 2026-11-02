package de.belkis.spyhound.shell

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import de.belkis.spyhound.AppLanguage
import de.belkis.spyhound.R
import de.belkis.spyhound.setup.isDeveloperOptionsEnabled
import io.github.muntashirakon.adb.android.AdbMdns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Runs the one-time pairing with the phone's own wireless debugging and reports the connected [AdbShell].
 * The user stays in Settings and types the six-digit code into a notification; the flow itself is the pure [AdbSetup].
 */
object BuiltinAdb {
    private const val POLL_MS = 1_500L
    private const val MAX_WAIT_MS = 5 * 60_000L
    private const val CONNECT_TIMEOUT_MS = 8_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var setup = AdbSetup(Build.VERSION.SDK_INT)
    private val _state = MutableStateFlow(setup.state)
    val state: StateFlow<AdbSetupState> = _state

    private var job: Job? = null
    private var mdns: AdbMdns? = null
    private var lastPairingPort: Int? = null
    private var wrongCode = false
    @Volatile private var onConnected: ((AdbShell) -> Unit)? = null

    /** Starts (or restarts) the guided setup. [connected] gets the shell once pairing and connecting worked. */
    fun start(context: Context, connected: (AdbShell) -> Unit) {
        val app = context.applicationContext
        job?.cancel()
        onConnected = connected
        synchronized(lock) {
            wrongCode = false
            send(AdbSetupEvent.Restart)
        }
        job = scope.launch { run(app) }
    }

    /** Called with the digits the user typed into the notification. */
    fun submitCode(context: Context, raw: CharSequence?) {
        val app = context.applicationContext
        val code = validPairingCode(raw)
        val (port, digits) = synchronized(lock) {
            val port = setup.pairingPort
            wrongCode = code == null
            if (code == null || port == null || !setup.canSubmitCode) {
                send(AdbSetupEvent.CodeEntered(raw?.toString().orEmpty()))
                if (_state.value == AdbSetupState.WaitingForPairingCode) AdbPairingNotification.show(app, wrongCode = true)
                else if (_state.value !is AdbSetupState.Failed) AdbPairingNotification.show(app, lost = true)
                return
            }
            send(AdbSetupEvent.CodeEntered(code))
            port to code
        }
        AdbPairingNotification.show(app, pairing = true)
        scope.launch {
            val ok = try { AdbShell.pair(app, port, digits) } catch (e: CancellationException) { throw e } catch (t: Throwable) { false }
            synchronized(lock) {
                wrongCode = !ok
                if (ok) ShellPreferences.setBuiltinAdbPaired(app, true)
                send(AdbSetupEvent.PairingFinished(ok))
            }
            if (!ok && _state.value == AdbSetupState.WaitingForPairingCode) AdbPairingNotification.show(app, wrongCode = true)
        }
    }

    fun stop(context: Context) {
        job?.cancel()
        stopMdns()
        AdbPairingNotification.cancel(context.applicationContext)
    }

    private suspend fun run(app: Context) {
        val deadline = System.currentTimeMillis() + MAX_WAIT_MS
        try {
            startMdns(app)
            while (true) {
                val s = synchronized(lock) {
                    send(AdbSetupEvent.EnvironmentChecked(
                        devOptions = isDeveloperOptionsEnabled(app),
                        wirelessDebugging = wirelessDebuggingOn(app),
                        paired = AdbKeys.exists(app) && ShellPreferences.wasBuiltinAdbPaired(app),
                    ))
                }
                when (s) {
                    AdbSetupState.WaitingForPairingCode -> AdbPairingNotification.show(app, wrongCode = wrongCode)
                    AdbSetupState.Paired -> { connect(app); return }
                    AdbSetupState.Connected, is AdbSetupState.Failed -> return
                    AdbSetupState.Pairing -> Unit
                    else -> AdbPairingNotification.cancel(app)
                }
                if (System.currentTimeMillis() > deadline) {
                    synchronized(lock) { send(AdbSetupEvent.WaitTimedOut) }
                    return
                }
                delay(POLL_MS)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            // Never die silently: the screen would stay on its last status forever.
            synchronized(lock) { send(AdbSetupEvent.InternalError) }
        } finally {
            stopMdns()
            AdbPairingNotification.cancel(app)
        }
    }

    private suspend fun connect(app: Context) {
        val shell = AdbShell.connect(app, CONNECT_TIMEOUT_MS)
        synchronized(lock) {
            // A refusal means the phone no longer knows our key: the next run starts with a fresh pairing.
            if (shell == null) ShellPreferences.setBuiltinAdbPaired(app, false)
            send(AdbSetupEvent.ConnectFinished(shell != null))
        }
        if (shell != null) onConnected?.invoke(shell)
    }

    private fun send(event: AdbSetupEvent): AdbSetupState {
        val before = setup.state
        setup.on(event)
        // A pairing dialog that opened before we were waiting does not announce itself again.
        if (before != AdbSetupState.WaitingForPairingCode && setup.state == AdbSetupState.WaitingForPairingCode) {
            lastPairingPort?.let { setup.on(AdbSetupEvent.PairingPortFound(it)) }
        }
        _state.value = setup.state
        return setup.state
    }

    private fun startMdns(app: Context) {
        stopMdns()
        lastPairingPort = null
        mdns = AdbMdns(app, AdbMdns.SERVICE_TYPE_TLS_PAIRING) { _, port ->
            synchronized(lock) {
                lastPairingPort = port.takeIf { it > 0 }
                send(if (port > 0) AdbSetupEvent.PairingPortFound(port) else AdbSetupEvent.PairingPortLost)
            }
        }.also { runCatching { it.start() } }
    }

    private fun stopMdns() {
        mdns?.let { runCatching { it.stop() } }
        mdns = null
    }

    /** Wireless debugging is a setting, readable without any permission. */
    fun wirelessDebuggingOn(context: Context): Boolean =
        runCatching { Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1 }.getOrDefault(false)

    /** mDNS port of the open pairing dialog, if known. */
    val pairingPort: Int?
        get() = synchronized(lock) { setup.pairingPort }
}

/** The notification that carries the code field, so the user never has to leave the Settings screen. */
internal object AdbPairingNotification {
    const val KEY_CODE = "pairing_code"
    private const val CHANNEL = "adb_pairing"
    private const val ID = 4712

    fun show(context: Context, wrongCode: Boolean = false, pairing: Boolean = false, lost: Boolean = false) {
        val ctx = AppLanguage.localizedContext(context)
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, ctx.getString(R.string.adb_notif_channel), NotificationManager.IMPORTANCE_HIGH),
        )
        val body = when {
            pairing -> R.string.adb_notif_pairing
            lost -> R.string.adb_notif_lost
            wrongCode -> R.string.adb_notif_wrong
            else -> R.string.adb_notif_text
        }
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(ctx.getString(R.string.adb_notif_title))
            .setContentText(ctx.getString(body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(ctx.getString(body)))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
        if (!pairing && !lost) {
            val input = RemoteInput.Builder(KEY_CODE).setLabel(ctx.getString(R.string.adb_notif_hint)).build()
            // RemoteInput needs a mutable PendingIntent; the receiver is not exported.
            val pending = PendingIntent.getBroadcast(
                context, 0, Intent(context, AdbPairingReceiver::class.java).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            builder.addAction(
                NotificationCompat.Action.Builder(R.drawable.ic_launcher_monochrome, ctx.getString(R.string.adb_notif_action), pending)
                    .addRemoteInput(input).build(),
            )
        }
        runCatching { nm.notify(ID, builder.build()) } // without notification permission the app screen still guides
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(ID)
    }
}

class AdbPairingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val code = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(AdbPairingNotification.KEY_CODE)
        BuiltinAdb.submitCode(context, code)
    }
}
