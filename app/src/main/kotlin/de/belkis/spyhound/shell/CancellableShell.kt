package de.belkis.spyhound.shell

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job

/**
 * Lets "Cancel" stop a running shell command at once. Commands block a thread for up to minutes
 * (`dumpsys usagestats`, hashing all APKs); cancelling [job] interrupts that thread, and [guarded]
 * answers an interrupt by closing the stream or killing the process.
 */
class CancellableShell(private val inner: Shell, private val job: Job) : Shell by inner {
    override fun read(command: String, maxBytes: Int, timeoutSec: Long): String? =
        interruptible { inner.read(command, maxBytes, timeoutSec) }

    override fun readLines(command: String, maxBytes: Int, timeoutSec: Long, onLine: (String) -> Unit): Boolean =
        interruptible { inner.readLines(command, maxBytes, timeoutSec, onLine) } ?: false

    @OptIn(InternalCoroutinesApi::class)
    private fun <T> interruptible(block: () -> T): T? {
        if (job.isCancelled) return null
        val thread = Thread.currentThread()
        // onCancelling: fires when cancel() is called, not only after the blocked body has returned.
        val handle = job.invokeOnCompletion(onCancelling = true, invokeImmediately = true) { thread.interrupt() }
        try {
            return block()
        } finally {
            handle.dispose()
            Thread.interrupted() // a pooled IO thread must not carry the interrupt into its next task
        }
    }
}
