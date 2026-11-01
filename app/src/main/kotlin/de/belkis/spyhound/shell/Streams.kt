package de.belkis.spyhound.shell

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference

private const val MAX_LINE_BYTES = 64 shl 10

/** Reads a stream up to [maxBytes] and stops there; the caller ends the producer. */
internal fun InputStream.readCapped(maxBytes: Int): String {
    val out = ByteArrayOutputStream()
    val buf = ByteArray(64 shl 10)
    while (out.size() < maxBytes) {
        val n = read(buf)
        if (n < 0) break
        out.write(buf, 0, minOf(n, maxBytes - out.size()))
    }
    return out.toString(Charsets.UTF_8.name())
}

/** Reads and discards a stream so the producer is never blocked on it. */
internal fun InputStream.drain() {
    val buf = ByteArray(8 shl 10)
    while (read(buf) >= 0) { /* discard */ }
}

/** Splits a stream into UTF-8 lines without holding more than one line; stops after [maxBytes] bytes. */
internal fun InputStream.forEachLineCapped(maxBytes: Int, onLine: (String) -> Unit) {
    val buf = ByteArray(64 shl 10)
    val line = ByteArrayOutputStream()
    var total = 0
    fun flush() {
        onLine(line.toString(Charsets.UTF_8.name()))
        line.reset()
    }
    while (total < maxBytes) {
        val n = read(buf)
        if (n < 0) break
        val take = minOf(n, maxBytes - total)
        for (i in 0 until take) {
            if (buf[i] == '\n'.code.toByte()) {
                flush()
            } else {
                if (line.size() >= MAX_LINE_BYTES) flush() // a pathological line is cut, not buffered
                line.write(buf[i].toInt())
            }
        }
        total += take
    }
    if (line.size() > 0) flush()
}

internal fun Closeable.closeQuietly() {
    try { close() } catch (_: IOException) {}
}

/**
 * Runs [body] on a helper thread and waits at most [timeoutSec] for it. On timeout, failure, or when it is done,
 * [abort] is called. The caller is never left blocked, even if the producer ignores [abort] (a read on a pipe
 * that a surviving child holds open cannot be woken on every platform); such a worker is simply abandoned.
 * Returns null on timeout or failure.
 */
internal fun <T> guarded(timeoutSec: Long, abort: () -> Unit, body: () -> T): T? {
    val result = AtomicReference<Result<T>?>(null)
    val worker = Thread { result.set(runCatching(body)) }.apply { isDaemon = true; start() }
    var timedOut = false
    try {
        worker.join(timeoutSec * 1000)
        if (worker.isAlive) {
            timedOut = true
            abort()
            worker.join(2000) // lets a woken worker finish; a stuck one is abandoned
        }
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        abort()
        return null
    } finally {
        abort()
    }
    return if (timedOut) null else result.get()?.getOrNull()
}

/** Hands lines to [onLine] under a lock and stops once closed, so an abandoned worker cannot touch results later. */
internal class LineSink(private val onLine: (String) -> Unit) {
    private var open = true

    fun accept(line: String) = synchronized(this) { if (open) onLine(line) }

    fun close() = synchronized(this) { open = false }
}

/** Runs [read] over a [LineSink]; false if it failed or timed out. Lines delivered before that stay valid. */
internal fun streamLines(onLine: (String) -> Unit, read: (LineSink) -> Unit?): Boolean {
    val sink = LineSink(onLine)
    try {
        return read(sink) != null
    } finally {
        sink.close()
    }
}

/**
 * Starts [argv] and gives its stdout to [use]. On timeout, failure, or when [use] returns, the process is killed
 * and both pipes are closed.
 */
internal fun <T> streamProcess(argv: List<String>, timeoutSec: Long, use: (InputStream) -> T): T? {
    val p = try { ProcessBuilder(argv).start() } catch (_: IOException) { return null }
    p.outputStream.closeQuietly()
    Thread { try { p.errorStream.drain() } catch (_: IOException) {} }.apply { isDaemon = true; start() }
    return guarded(timeoutSec, abort = {
        p.destroyForcibly()
        p.inputStream.closeQuietly()
        p.errorStream.closeQuietly()
    }) { use(p.inputStream) }
}
