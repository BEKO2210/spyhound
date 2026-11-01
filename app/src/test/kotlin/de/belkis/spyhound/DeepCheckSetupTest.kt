package de.belkis.spyhound

import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.setup.DeepCheckSetupSnapshot
import de.belkis.spyhound.setup.DeepCheckSetupStep
import de.belkis.spyhound.setup.activeDeepCheckStep
import de.belkis.spyhound.setup.stepDone
import de.belkis.spyhound.shell.ShizukuShell
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeepCheckSetupTest {
    @Test
    fun `first missing step is install when Shizuku absent`() {
        val snap = DeepCheckSetupSnapshot(false, false, ShizukuShell.Status.NOT_RUNNING, AccessLevel.APP)
        assertEquals(DeepCheckSetupStep.INSTALL_SHIZUKU, activeDeepCheckStep(snap))
        assertFalse(stepDone(DeepCheckSetupStep.INSTALL_SHIZUKU, snap))
    }

    @Test
    fun `dev options step after install`() {
        val snap = DeepCheckSetupSnapshot(true, false, ShizukuShell.Status.NOT_RUNNING, AccessLevel.APP)
        assertEquals(DeepCheckSetupStep.DEV_OPTIONS, activeDeepCheckStep(snap))
        assertTrue(stepDone(DeepCheckSetupStep.INSTALL_SHIZUKU, snap))
    }

    @Test
    fun `start step when dev options on but Shizuku not running`() {
        val snap = DeepCheckSetupSnapshot(true, true, ShizukuShell.Status.NOT_RUNNING, AccessLevel.APP)
        assertEquals(DeepCheckSetupStep.START_SHIZUKU, activeDeepCheckStep(snap))
    }

    @Test
    fun `allow step when Shizuku running without shell access`() {
        val snap = DeepCheckSetupSnapshot(true, true, ShizukuShell.Status.NEEDS_PERMISSION, AccessLevel.APP)
        assertEquals(DeepCheckSetupStep.ALLOW_SPYHOUND, activeDeepCheckStep(snap))
        assertTrue(stepDone(DeepCheckSetupStep.START_SHIZUKU, snap))
    }

    @Test
    fun `finished when shell connected`() {
        val snap = DeepCheckSetupSnapshot(true, true, ShizukuShell.Status.READY, AccessLevel.SHELL)
        assertNull(activeDeepCheckStep(snap))
        assertTrue(snap.allDone)
    }
}
