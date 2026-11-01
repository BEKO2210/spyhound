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

/**
 * Ends [inner] at the first [marker] and reads the exit code printed right after it. For transports (libadb
 * `shell:` streams) that never signal the end of a finished command; the marker carries a per-call nonce so
 * command output cannot fake it. Output before the marker is passed on unchanged.
 */
internal class MarkerInputStream(private val inner: InputStream, marker: String) : InputStream() {
    private val mark = marker.toByteArray(Charsets.US_ASCII)
    private val held = ByteArrayOutputStream()
    private var ready = ByteArray(0)
    private var pos = 0
    private var innerEof = false
    private var finished = false

    /** Exit code of the command once the marker was seen; null if the stream ended without it. */
    var exitCode: Int? = null
        private set

    override fun read(): Int {
        val one = ByteArray(1)
        return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xff
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        while (pos >= ready.size) {
            if (finished) return -1
            fill()
        }
        val n = minOf(len, ready.size - pos)
        System.arraycopy(ready, pos, b, off, n)
        pos += n
        return n
    }

    private fun fill() {
        val chunk = ByteArray(8192)
        val n = if (innerEof) -1 else inner.read(chunk)
        if (n < 0) {
            innerEof = true
            ready = held.toByteArray()
            held.reset()
            pos = 0
            finished = true
            return
        }
        held.write(chunk, 0, n)
        val all = held.toByteArray()
        val at = indexOf(all, mark)
        if (at >= 0) {
            ready = all.copyOfRange(0, at)
            pos = 0
            finished = true
            exitCode = readExit(all, at + mark.size)
            held.reset()
            return
        }
        val keep = minOf(all.size, mark.size - 1)
        ready = all.copyOfRange(0, all.size - keep)
        pos = 0
        held.reset()
        held.write(all, all.size - keep, keep)
    }

    private fun readExit(all: ByteArray, from: Int): Int? {
        val digits = StringBuilder()
        var i = from
        while (true) {
            val c = if (i < all.size) all[i++].toInt() else try { inner.read() } catch (_: IOException) { -1 }
            if (c !in '0'.code..'9'.code) break
            if (digits.length < 4) digits.append(c.toChar())
        }
        return digits.toString().toIntOrNull()
    }

    private fun indexOf(h: ByteArray, n: ByteArray): Int {
        outer@ for (i in 0..h.size - n.size) {
            for (j in n.indices) if (h[i + j] != n[j]) continue@outer
            return i
        }
        return -1
    }

    override fun close() = inner.close()
}
