package de.belkis.spyhound.bugreport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Upper bound for reading a user-chosen bug report; a provider that stalls must not hang the import. */
const val BUGREPORT_READ_TIMEOUT_MS = 120_000L

/**
 * Opens a resource and runs [block] on it, giving up after [timeoutMs] (null result). On timeout the resource is
 * closed so that a blocked read is released instead of leaking a stuck thread.
 */
suspend fun <C : Closeable, T> readWithTimeout(
    timeoutMs: Long,
    open: () -> C?,
    block: (C) -> T?,
): T? {
    val current = AtomicReference<Closeable?>()
    val timedOut = AtomicBoolean(false)
    // Own scope: a read that ignores cancellation must not keep the caller waiting.
    val job = CoroutineScope(SupervisorJob() + Dispatchers.IO).async {
        runCatching {
            val resource = open() ?: return@runCatching null
            current.set(resource)
            if (timedOut.get()) runCatching { resource.close() }
            resource.use(block)
        }.getOrNull()
    }
    val result = withTimeoutOrNull(timeoutMs) { job.await() }
    if (!job.isCompleted) {
        timedOut.set(true)
        runCatching { current.get()?.close() }
        job.cancel()
    }
    return result
}
