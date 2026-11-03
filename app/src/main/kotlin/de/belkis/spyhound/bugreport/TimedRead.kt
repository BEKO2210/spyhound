package de.belkis.spyhound.bugreport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import java.io.FilterInputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicLong

/**
 * A single read of a user-chosen bug report may block at most this long. Only a stalled read counts: parsing and
 * analysing a large report can take minutes on a phone and must not be cut off.
 */
const val BUGREPORT_STALL_TIMEOUT_MS = 60_000L

/**
 * Opens a stream and runs [block] on it. If one read() call blocks for longer than [stallMs], the stream is closed
 * (which releases the blocked read) and the result is null.
 */
suspend fun <T> readWithStallTimeout(
    stallMs: Long,
    open: () -> InputStream?,
    block: (InputStream) -> T?,
): T? {
    // 0 = no read in progress, otherwise the time the current read started.
    val readSince = AtomicLong(0)
    var watched: InputStream? = null
    // Own scope: a read that ignores cancellation must not keep the caller waiting.
    val job = CoroutineScope(SupervisorJob() + Dispatchers.IO).async {
        runCatching {
            val raw = open() ?: return@runCatching null
            val stream = object : FilterInputStream(raw) {
                private inline fun <R> timed(read: () -> R): R {
                    readSince.set(System.nanoTime())
                    try { return read() } finally { readSince.set(0) }
                }
                override fun read(): Int = timed { super.read() }
                override fun read(b: ByteArray, off: Int, len: Int): Int = timed { super.read(b, off, len) }
            }
            watched = stream
            stream.use(block)
        }.getOrNull()
    }
    val tick = (stallMs / 4).coerceIn(10, 1_000)
    while (job.isActive) {
        val since = readSince.get()
        if (since != 0L && (System.nanoTime() - since) / 1_000_000 > stallMs) {
            runCatching { watched?.close() }
            job.cancel()
            return null
        }
        delay(tick)
    }
    return runCatching { job.await() }.getOrNull()
}
