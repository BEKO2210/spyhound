package de.belkis.spyhound

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.belkis.spyhound.bugreport.readWithTimeout
import de.belkis.spyhound.dns.DnsGuardService
import de.belkis.spyhound.dns.DnsHit
import de.belkis.spyhound.dns.DnsHitStore
import de.belkis.spyhound.ioc.Category
import java.io.File
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class Robustness2Test {
    @Test
    fun lookupPoolIsBoundedAndDropsExcess() {
        val pool = DnsGuardService.newLookupPool() as ThreadPoolExecutor
        val gate = CountDownLatch(1)
        try {
            assertEquals(4, pool.maximumPoolSize)
            repeat(4 + 256) { pool.execute { gate.await() } }
            // Queue is full now: further packets are dropped silently, not queued and not thrown.
            repeat(1000) { pool.execute { gate.await() } }
            assertTrue(pool.queue.size <= 256)
        } catch (e: RejectedExecutionException) {
            throw AssertionError("excess packet must be dropped silently", e)
        } finally {
            gate.countDown()
            pool.shutdownNow()
        }
    }

    @Test
    fun concurrentHitsAreAllKeptAndFileParses() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.filesDir, "dns_guard_hits.json").delete()
        val n = 60
        val pool = Executors.newFixedThreadPool(12)
        repeat(n) { i ->
            pool.execute {
                runBlocking {
                    DnsHitStore.append(context, DnsHit(i.toLong(), "d$i.example", "f", Category.MERCENARY))
                }
            }
        }
        pool.shutdown()
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS))
        val hits = DnsHitStore.recent(context, 500)
        assertEquals(n, hits.map { it.domain }.toSet().size)
        assertTrue(context.filesDir.listFiles().orEmpty().none { it.name.endsWith(".tmp") })
    }

    @Test
    fun blockedReadTimesOutAndStreamIsClosed() {
        val released = CountDownLatch(1)
        val stuck = object : InputStream() {
            val gate = CountDownLatch(1)
            override fun read(): Int {
                gate.await()
                return -1
            }
            override fun close() {
                gate.countDown()
                released.countDown()
            }
        }
        val start = System.nanoTime()
        val r = runBlocking { readWithTimeout(300, { stuck }) { it.read() } }
        assertNull(r)
        assertTrue((System.nanoTime() - start) / 1_000_000 < 5_000)
        assertTrue(released.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun fastReadReturnsValue() {
        val r = runBlocking { readWithTimeout(5_000, { "abc".byteInputStream() }) { it.readBytes().size } }
        assertEquals(3, r)
    }
}
