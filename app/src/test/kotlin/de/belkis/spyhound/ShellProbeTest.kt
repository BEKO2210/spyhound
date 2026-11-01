package de.belkis.spyhound

import de.belkis.spyhound.shell.ProbeOutcome
import de.belkis.spyhound.shell.probeWithReconnect
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class ShellProbeTest {
    private class Fake(private val probeResults: List<Boolean>, private val reconnectOk: Boolean) {
        var probes = 0
        var reconnects = 0
        suspend fun probe(): Boolean = probeResults.getOrElse(probes++) { false }
        suspend fun reconnect(): Boolean { reconnects++; return reconnectOk }
    }

    private fun run(f: Fake) = runBlocking { probeWithReconnect(f::probe, f::reconnect) }

    @Test fun aliveFirstTimeDoesNotReconnect() {
        val f = Fake(listOf(true), true)
        assertEquals(ProbeOutcome.ALIVE, run(f))
        assertEquals(0, f.reconnects)
        assertEquals(1, f.probes)
    }

    @Test fun aliveAfterReconnect() {
        val f = Fake(listOf(false, true), true)
        assertEquals(ProbeOutcome.ALIVE_AFTER_RECONNECT, run(f))
        assertEquals(1, f.reconnects)
        assertEquals(2, f.probes)
    }

    @Test fun failedReconnectIsDeadWithoutSecondProbe() {
        val f = Fake(listOf(false, true), false)
        assertEquals(ProbeOutcome.DEAD, run(f))
        assertEquals(1, f.reconnects)
        assertEquals(1, f.probes)
    }

    @Test fun deadAfterReconnectStopsAfterOneRetry() {
        val f = Fake(emptyList(), true)
        assertEquals(ProbeOutcome.DEAD, run(f))
        assertEquals(1, f.reconnects)
        assertEquals(2, f.probes)
    }
}
