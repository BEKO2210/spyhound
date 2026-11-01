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
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.system.exitProcess

/** A shell with more rights than the app itself. */
interface Shell {
    val level: AccessLevel

    /** Runs [command] and returns its standard output (capped at [maxBytes]), or null if it could not run. */
    fun read(command: String, maxBytes: Int = 48 shl 20, timeoutSec: Long = 180): String?

    fun close() {}
}

/** Reads a stream up to [maxBytes]; the rest is drained so the producer is not blocked. */
internal fun InputStream.readCapped(maxBytes: Int): String {
    val out = ByteArrayOutputStream()
    val buf = ByteArray(64 shl 10)
    while (true) {
        val n = read(buf)
        if (n < 0) break
        val room = maxBytes - out.size()
        if (room > 0) out.write(buf, 0, minOf(n, room))
    }
    return out.toString(Charsets.UTF_8.name())
}

/** Root through `su` (Magisk, KernelSU, APatch all provide it). */
class RootShell : Shell {
    override val level = AccessLevel.ROOT

    override fun read(command: String, maxBytes: Int, timeoutSec: Long): String? = runCatching {
        val p = ProcessBuilder("su", "-c", command).redirectErrorStream(false).start()
        p.outputStream.close()
        Thread { runCatching { p.errorStream.readCapped(0) } }.start()
        val reader = Thread.currentThread()
        val killer = Thread {
            if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) { p.destroyForcibly(); reader.interrupt() }
        }.apply { isDaemon = true; start() }
        val text = p.inputStream.readCapped(maxBytes)
        killer.interrupt()
        text
    }.getOrNull()

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
class ShizukuShell private constructor(private val service: IShellService, uid: Int) : Shell {
    override val level = if (uid == 0) AccessLevel.ROOT else AccessLevel.SHELL

    override fun read(command: String, maxBytes: Int, timeoutSec: Long): String? = runCatching {
        val pfd = service.exec(command)
        val timer = Thread {
            try { Thread.sleep(timeoutSec * 1000); pfd.close() } catch (_: InterruptedException) {}
        }.apply { isDaemon = true; start() }
        val text = ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readCapped(maxBytes) }
        timer.interrupt()
        text
    }.getOrNull()

    override fun close() {
        runCatching { Shizuku.unbindUserService(args, null, true) }
    }

    enum class Status { NOT_RUNNING, NEEDS_PERMISSION, READY, TOO_OLD }

    companion object {
        private const val REQUEST_CODE = 4711

        private val args by lazy {
            Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, ShellUserService::class.java.name))
                .daemon(false)
                .processNameSuffix("shell")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE)
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
            withTimeoutOrNull(15_000) {
                suspendCancellableCoroutine { cont ->
                    val conn = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                            val svc = binder?.takeIf { it.pingBinder() }?.let { IShellService.Stub.asInterface(it) }
                            val shell = svc?.let { s -> runCatching { ShizukuShell(s, s.uid()) }.getOrNull() }
                            if (cont.isActive) cont.resume(shell)
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {}
                    }
                    runCatching { Shizuku.bindUserService(args, conn) }.onFailure { if (cont.isActive) cont.resume(null) }
                }
            }
        }
    }
}

/** Lives in Shizuku's process. Must keep a public no-argument constructor. */
class ShellUserService : IShellService.Stub() {
    override fun exec(command: String): ParcelFileDescriptor {
        val (read, write) = ParcelFileDescriptor.createPipe()
        val p = ProcessBuilder("sh", "-c", command).start()
        p.outputStream.close()
        Thread { runCatching { p.errorStream.readCapped(0) } }.start()
        Thread {
            runCatching {
                ParcelFileDescriptor.AutoCloseOutputStream(write).use { out -> p.inputStream.copyTo(out) }
            }
            p.destroy()
        }.start()
        return read
    }

    override fun uid(): Int = Process.myUid()

    override fun destroy() {
        exitProcess(0)
    }
}
