package de.belkis.spyhound

import de.belkis.spyhound.shell.SHIZUKU_STOPPED_BANNER
import de.belkis.spyhound.shell.ScanGatedShellChanges
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShellSessionTest {
    @Test fun defersShellClearUntilScanEnds() {
        val gate = ScanGatedShellChanges()
        gate.scanStarted()
        assertFalse(gate.deferClearShell(SHIZUKU_STOPPED_BANNER))
        assertTrue(gate.hasDeferredWork)
        val work = gate.scanFinished()
        assertEquals(SHIZUKU_STOPPED_BANNER, work?.bannerMessage)
        assertTrue(work!!.clearShell)
        assertFalse(work.applyPendingShell)
        assertNull(gate.scanFinished())
    }

    @Test fun defersNewShellUntilScanEnds() {
        val gate = ScanGatedShellChanges()
        gate.scanStarted()
        assertFalse(gate.deferAdoptNewShell())
        val work = gate.scanFinished()
        assertTrue(work!!.applyPendingShell)
        assertFalse(work.clearShell)
    }

    @Test fun appliesImmediatelyWhenNoScan() {
        val gate = ScanGatedShellChanges()
        assertTrue(gate.deferClearShell())
        assertTrue(gate.deferAdoptNewShell())
        assertNull(gate.scanFinished())
    }
}
