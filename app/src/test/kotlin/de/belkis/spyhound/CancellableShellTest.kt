package de.belkis.spyhound

import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.shell.CancellableShell
import de.belkis.spyhound.shell.Shell
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.job
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CancellableShellTest {
    /** Blocks like a long `dumpsys` until its thread is interrupted, as [de.belkis.spyhound.shell.guarded] reacts. */
    private class SlowShell(val started: CountDownLatch) : Shell {
        override val level = AccessLevel.SHELL
        @Volatile var interrupted = false
        override fun read(command: String, maxBytes: Int, timeoutSec: Long): String? {
            started.countDown()
            return try { Thread.sleep(timeoutSec * 1000); "done" } catch (_: InterruptedException) { interrupted = true; null }
        }
    }

    @Test fun cancelStopsARunningCommandAtOnce() = runBlocking {
        val started = CountDownLatch(1)
        val slow = SlowShell(started)
        val t0 = System.nanoTime()
        val scan = async(Dispatchers.IO) {
            withContext(Dispatchers.IO) { CancellableShell(slow, coroutineContext.job).read("dumpsys usagestats", 1, 180) }
        }
        assertTrue(started.await(5, TimeUnit.SECONDS))
        scan.cancelAndJoin()
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertTrue(slow.interrupted, "command thread was not interrupted")
        assertTrue(ms < 3_000, "cancel took $ms ms")
    }

    @Test fun cancelledJobRunsNoFurtherCommands() {
        val job = Job().apply { cancel() }
        val started = CountDownLatch(1)
        assertNull(CancellableShell(SlowShell(started), job).read("ps", 1, 1))
        assertTrue(started.count == 1L)
    }

    @Test fun finishedCommandLeavesNoInterruptOnThePooledThread() {
        val shell = CancellableShell(object : Shell {
            override val level = AccessLevel.SHELL
            override fun read(command: String, maxBytes: Int, timeoutSec: Long) = "ok"
        }, Job())
        shell.read("echo", 1, 1)
        assertTrue(!Thread.currentThread().isInterrupted)
    }
}
