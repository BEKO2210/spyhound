package de.belkis.spyhound

import de.belkis.spyhound.setup.BuiltinAdbWizardStep
import de.belkis.spyhound.setup.deriveBuiltinAdbWizardSteps
import de.belkis.spyhound.shell.AdbFailure
import de.belkis.spyhound.shell.AdbSetupState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinAdbWizardStepsTest {

    @Test
    fun step1RequiresWirelessNotDevOptionsFlag() {
        val steps = deriveBuiltinAdbWizardSteps(
            wirelessOn = false,
            state = AdbSetupState.NeedsDevOptions,
            pairingPort = null,
        )
        assertFalse(steps.step1Done)
        assertEquals(BuiltinAdbWizardStep.WLAN_DEBUG, steps.activeStep)
    }

    @Test
    fun android17WirelessOnMarksStep1DoneDespiteHiddenDevFlag() {
        val steps = deriveBuiltinAdbWizardSteps(
            wirelessOn = true,
            state = AdbSetupState.WaitingForPairingCode,
            pairingPort = null,
        )
        assertTrue(steps.step1Done)
        assertEquals(BuiltinAdbWizardStep.PAIRING_DIALOG, steps.activeStep)
    }

    @Test
    fun step2DoneWhenPairingPortKnown() {
        val steps = deriveBuiltinAdbWizardSteps(
            wirelessOn = true,
            state = AdbSetupState.WaitingForPairingCode,
            pairingPort = 37123,
        )
        assertTrue(steps.step2Done)
        assertEquals(BuiltinAdbWizardStep.ENTER_CODE, steps.activeStep)
    }

    @Test
    fun step3DoneWhenPairedOrConnected() {
        val paired = deriveBuiltinAdbWizardSteps(true, AdbSetupState.Paired, 42)
        assertTrue(paired.step3Done)
        assertNull(paired.activeStep)
        val connected = deriveBuiltinAdbWizardSteps(true, AdbSetupState.Connected, null)
        assertTrue(connected.step3Done)
    }

    @Test
    fun failureMapsToCurrentStep() {
        val timeout = deriveBuiltinAdbWizardSteps(
            wirelessOn = true,
            state = AdbSetupState.Failed(AdbFailure.TIMEOUT),
            pairingPort = null,
        )
        assertEquals(BuiltinAdbWizardStep.PAIRING_DIALOG, timeout.failedStep)
        assertEquals(AdbFailure.TIMEOUT, timeout.failure)

        val codes = deriveBuiltinAdbWizardSteps(
            wirelessOn = true,
            state = AdbSetupState.Failed(AdbFailure.TOO_MANY_INVALID_CODES),
            pairingPort = 40000,
        )
        assertEquals(BuiltinAdbWizardStep.ENTER_CODE, codes.failedStep)
    }

    @Test
    fun unsupportedFailsStep1() {
        val steps = deriveBuiltinAdbWizardSteps(
            wirelessOn = false,
            state = AdbSetupState.Failed(AdbFailure.UNSUPPORTED),
            pairingPort = null,
        )
        assertEquals(BuiltinAdbWizardStep.WLAN_DEBUG, steps.failedStep)
    }
}
