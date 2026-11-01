package de.belkis.spyhound.shell

import android.content.Context
import android.os.Build
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import io.github.muntashirakon.adb.android.AdbMdns
import de.belkis.spyhound.scan.AccessLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.InputStream
import java.security.PrivateKey
import java.security.cert.Certificate
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** Only ever the phone itself; see [isLocalAdbHost]. */
internal const val ADB_LOOPBACK = "127.0.0.1"

/** libadb throws "Stream closed" instead of returning -1 when the command ends; that is a normal end of output. */
private class EofOnClose(private val inner: InputStream) : InputStream() {
    override fun read(): Int = try { inner.read() } catch (_: IOException) { -1 }
    override fun read(b: ByteArray, off: Int, len: Int): Int = try { inner.read(b, off, len) } catch (_: IOException) { -1 }
    override fun close() = inner.close()
}

/** libadb connection manager backed by the app's own [AdbKeys]. */
private class OwnAdbConnection(context: Context) : AbsAdbConnectionManager() {
    private val keys = AdbKeys.loadOrCreate(context)

    init {
        api = Build.VERSION.SDK_INT
        hostAddress = ADB_LOOPBACK
        setTimeout(10, TimeUnit.SECONDS)
    }

    override fun getPrivateKey(): PrivateKey = keys.privateKey
    override fun getCertificate(): Certificate = keys.certificate
    override fun getDeviceName(): String = "Spyhound"
}

/**
 * ADB-shell rights without a computer and without a second app: Spyhound pairs once with the phone's own
 * wireless-debugging service and then runs commands over a local ADB connection (`shell:` streams).
 * It only talks to 127.0.0.1; nothing here reaches the internet.
 */
class AdbShell private constructor(private val context: Context) : Shell {
    override val level = AccessLevel.SHELL
    override val isBuiltinAdb get() = true

    private fun <T> stream(command: String, timeoutSec: Long, use: (InputStream) -> T): T? = runCatching {
        // libadb never reports the end of a finished command, so the command prints a nonce-marked end line.
        val marker = "__SH_END_${java.util.UUID.randomUUID().toString().replace("-", "")}__"
        val adb = connection.openStream("shell:{ $command\n}; echo $marker\$?")
        // Closing the stream ends the command on the phone; the caller is never left blocked.
        guarded(timeoutSec, abort = { runCatching { adb.close() } }) {
            MarkerInputStream(EofOnClose(adb.openInputStream()), marker).use(use)
        }
    }.getOrNull()

    override fun read(command: String, maxBytes: Int, timeoutSec: Long): String? =
        stream(command, timeoutSec) { it.readCapped(maxBytes) }

    override fun readLines(command: String, maxBytes: Int, timeoutSec: Long, onLine: (String) -> Unit): Boolean =
        streamLines(onLine) { sink -> stream(command, timeoutSec) { it.forEachLineCapped(maxBytes, sink::accept) } }

    /** Drops a stale connection (the port changes whenever wireless debugging is toggled) and connects afresh. */
    override suspend fun reconnect(): Boolean = withContext(Dispatchers.IO) {
        runCatching { connection.disconnect() }
        connectNow(context) && alive()
    }

    override fun close() {
        runCatching { connection.disconnect() }
    }

    private fun alive(): Boolean = read("echo spyhound-ok", 64, 15)?.contains("spyhound-ok") == true

    companion object {
        private const val DISCOVERY_TIMEOUT_MS = 5_000L

        @Volatile private var manager: OwnAdbConnection? = null

        private val connection: AbsAdbConnectionManager
            get() = checkNotNull(manager) { "no ADB connection" }

        private fun manager(context: Context): OwnAdbConnection =
            manager ?: synchronized(this) { manager ?: OwnAdbConnection(context.applicationContext).also { manager = it } }

        /** Connects to the already paired wireless-debugging service of this phone. Null if that is not possible. */
        suspend fun connect(context: Context, discoveryTimeoutMs: Long = DISCOVERY_TIMEOUT_MS): AdbShell? =
            withContext(Dispatchers.IO) {
                val app = context.applicationContext
                if (!AdbKeys.exists(app)) return@withContext null
                if (!connectNow(app, discoveryTimeoutMs)) return@withContext null
                AdbShell(app).takeIf { it.alive() }
            }

        private suspend fun connectNow(context: Context, discoveryTimeoutMs: Long = DISCOVERY_TIMEOUT_MS): Boolean {
            val port = discoverPort(context, AdbMdns.SERVICE_TYPE_TLS_CONNECT, discoveryTimeoutMs) ?: return false
            return connectToPort(context, port)
        }

        private fun connectToPort(context: Context, port: Int): Boolean {
            require(isLocalAdbHost(ADB_LOOPBACK))
            val m = manager(context)
            return runCatching { m.isConnected || m.connect(ADB_LOOPBACK, port) }.getOrDefault(false)
        }

        /** Pairs with the open "Pair device with pairing code" dialog. The code is used once and not kept. */
        suspend fun pair(context: Context, port: Int, code: String): Boolean = withContext(Dispatchers.IO) {
            if (validPairingCode(code) == null) return@withContext false
            runCatching { manager(context).pair(ADB_LOOPBACK, port, code) }.getOrDefault(false)
        }

        /**
         * Finds the port of one of this phone's own wireless-debugging services (`_adb-tls-connect._tcp` or
         * `_adb-tls-pairing._tcp`) through mDNS. Services on other devices are ignored by the discovery.
         */
        suspend fun discoverPort(context: Context, type: String, timeoutMs: Long): Int? = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val mdns = AdbMdns(context.applicationContext, type) { _, port ->
                    if (port > 0 && cont.isActive) cont.resume(port)
                }
                cont.invokeOnCancellation { runCatching { mdns.stop() } }
                runCatching { mdns.start() }.onFailure { if (cont.isActive) cont.resume(null) }
            }
        }

        /** Forgets the key and the connection. The phone then needs a new pairing. */
        fun forget(context: Context) {
            runCatching { manager?.disconnect() }
            manager = null
            AdbKeys.delete(context.applicationContext)
        }
    }
}
