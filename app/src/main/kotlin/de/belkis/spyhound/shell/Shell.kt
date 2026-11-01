package de.belkis.spyhound.shell

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.Process
import de.belkis.spyhound.BuildConfig
import de.belkis.spyhound.scan.AccessLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.system.exitProcess

/** A shell with more rights than the app itself. */
interface Shell {
    val level: AccessLevel

    /** Runs [command] and returns its standard output (capped at [maxBytes]), or null if it could not run or timed out. */
    fun read(command: String, maxBytes: Int = 48 shl 20, timeoutSec: Long = 180): String?

    /**
     * Like [read], but hands the output over line by line, so a large output never sits in memory as a whole.
     * Returns false if the command could not run or timed out (lines delivered before that stay valid).
     */
    fun readLines(command: String, maxBytes: Int = 48 shl 20, timeoutSec: Long = 180, onLine: (String) -> Unit): Boolean =
        read(command, maxBytes, timeoutSec)?.let { text -> text.lineSequence().forEach(onLine); true } ?: false

    /** Drops a possibly stale connection and opens a fresh one. Returns false if that did not work. */
    suspend fun reconnect(): Boolean = false

    fun close() {}
}

/** Root through `su` (Magisk, KernelSU, APatch all provide it). */
class RootShell : Shell {
    override val level = AccessLevel.ROOT

    override fun read(command: String, maxBytes: Int, timeoutSec: Long): String? =
        streamProcess(listOf("su", "-c", command), timeoutSec) { it.readCapped(maxBytes) }

    override fun readLines(command: String, maxBytes: Int, timeoutSec: Long, onLine: (String) -> Unit): Boolean =
        streamLines(onLine) { sink -> streamProcess(listOf("su", "-c", command), timeoutSec) { it.forEachLineCapped(maxBytes, sink::accept) } }

    // Every command already starts its own su process; a fresh uid check is the new session.
    override suspend fun reconnect(): Boolean = withContext(Dispatchers.IO) { probe() != null }

    companion object {
        /** Asks su for uid 0. A root manager may show its consent prompt here. */
        fun probe(): RootShell? {
            val sh = RootShell()
            val id = sh.read("id", 4096, 30) ?: return null
            return if ("uid=0" in id) sh else null
        }
    }
}

/** ADB-shell rights (or root, if Shizuku itself runs as root) without a computer, through Shizuku. */
class ShizukuShell private constructor(service: IShellService, uid: Int) : Shell {
    override val level = if (uid == 0) AccessLevel.ROOT else AccessLevel.SHELL

    @Volatile private var service: IShellService = service

    private fun <T> stream(command: String, timeoutSec: Long, use: (InputStream) -> T): T? = runCatching {
        val pfd = service.exec(command, timeoutSec)
        // Closing our end ends the producer too: its next write fails, and the service has a watchdog of its own.
        guarded(timeoutSec, abort = { pfd.closeQuietly() }) {
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use(use)
        }
    }.getOrNull()

    override fun read(command: String, maxBytes: Int, timeoutSec: Long): String? =
        stream(command, timeoutSec) { it.readCapped(maxBytes) }

    override fun readLines(command: String, maxBytes: Int, timeoutSec: Long, onLine: (String) -> Unit): Boolean =
        streamLines(onLine) { sink -> stream(command, timeoutSec) { it.forEachLineCapped(maxBytes, sink::accept) } }

    /** Unbinds the stale service (removing it) and binds a new one; this shell keeps working through it. */
    override suspend fun reconnect(): Boolean = withContext(Dispatchers.Main) {
        runCatching { Shizuku.unbindUserService(args, null, true) }
        val fresh = bind(RECONNECT_TIMEOUT_MS)?.service ?: return@withContext false
        service = fresh
        true
    }

    override fun close() {
        runCatching { Shizuku.unbindUserService(args, null, true) }
    }

    enum class Status { NOT_RUNNING, NEEDS_PERMISSION, READY, TOO_OLD }

    companion object {
        private const val REQUEST_CODE = 4711
        private const val CONNECT_TIMEOUT_MS = 15_000L
        private const val RECONNECT_TIMEOUT_MS = 5_000L

        private val args by lazy {
            Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, ShellUserService::class.java.name))
                .daemon(false)
                .processNameSuffix("shell")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.SHELL_SERVICE_VERSION)
        }

        fun status(): Status {
            if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) return Status.NOT_RUNNING
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) return Status.TOO_OLD
            return if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) Status.READY else Status.NEEDS_PERMISSION
        }

        /** Shows Shizuku's own consent dialog; the answer arrives in [onResult]. */
        fun requestPermission(onResult: (Boolean) -> Unit) {
            val listener = object : Shizuku.OnRequestPermissionResultListener {
                override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                    if (requestCode != REQUEST_CODE) return
                    Shizuku.removeRequestPermissionResultListener(this)
                    onResult(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
            Shizuku.addRequestPermissionResultListener(listener)
            Shizuku.requestPermission(REQUEST_CODE)
        }

        suspend fun connect(): ShizukuShell? = withContext(Dispatchers.Main) {
            if (status() != Status.READY) return@withContext null
            bind(CONNECT_TIMEOUT_MS)
        }

        private suspend fun bind(timeoutMs: Long): ShizukuShell? =
            withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine { cont ->
                    val conn = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                            val svc = binder?.takeIf { it.pingBinder() }?.let { IShellService.Stub.asInterface(it) }
                            val shell = svc?.let { s -> runCatching { ShizukuShell(s, s.uid()) }.getOrNull() }
                            if (cont.isActive) cont.resume(shell)
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {}
                    }
                    cont.invokeOnCancellation { runCatching { Shizuku.unbindUserService(args, conn, false) } }
                    runCatching { Shizuku.bindUserService(args, conn) }.onFailure { if (cont.isActive) cont.resume(null) }
                }
            }
    }
}

/** Lives in Shizuku's process. Must keep a public no-argument constructor. */
class ShellUserService : IShellService.Stub() {
    override fun exec(command: String, timeoutSec: Long): ParcelFileDescriptor {
        val (read, write) = ParcelFileDescriptor.createPipe()
        val p = ProcessBuilder("sh", "-c", command).start()
        p.outputStream.close()
        Thread { try { p.errorStream.drain() } catch (_: IOException) {} }.apply { isDaemon = true; start() }
        // The client enforces the timeout too; this covers a vanished client while the command stays silent.
        val watchdog = Thread {
            try { Thread.sleep((timeoutSec + 5) * 1000); p.destroyForcibly() } catch (_: InterruptedException) {}
        }.apply { isDaemon = true; start() }
        Thread {
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(write).use { out -> p.inputStream.copyTo(out) }
            } catch (_: IOException) {
            } finally {
                watchdog.interrupt()
                p.destroyForcibly()
            }
        }.start()
        return read
    }

    override fun uid(): Int = Process.myUid()

    override fun destroy() {
        exitProcess(0)
    }
}
