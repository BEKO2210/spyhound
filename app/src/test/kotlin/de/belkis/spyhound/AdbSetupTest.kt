package de.belkis.spyhound

import de.belkis.spyhound.shell.AdbFailure
import de.belkis.spyhound.shell.AdbSetup
import de.belkis.spyhound.shell.AdbSetupEvent
import de.belkis.spyhound.shell.AdbSetupEvent.CodeEntered
import de.belkis.spyhound.shell.AdbSetupEvent.EnvironmentChecked
import de.belkis.spyhound.shell.AdbSetupState
import de.belkis.spyhound.shell.isLocalAdbHost
import de.belkis.spyhound.shell.validPairingCode
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdbSetupTest {
    private fun waiting(): AdbSetup = AdbSetup(34).apply {
        on(EnvironmentChecked(devOptions = true, wirelessDebugging = true))
        on(AdbSetupEvent.PairingPortFound(40123))
    }

    @Test fun codeMustBeSixDigits() {
        assertEquals("123456", validPairingCode("123456"))
        assertEquals("012345", validPairingCode(" 012345 "))
        for (bad in listOf("", "12345", "1234567", "12345a", "12 456", "１２３４５６", "-12345")) assertNull(validPairingCode(bad), bad)
        assertNull(validPairingCode(null))
    }

    @Test fun environmentLeadsThroughTheSteps() {
        val s = AdbSetup(34)
        assertEquals(AdbSetupState.NeedsDevOptions, s.state)
        assertEquals(AdbSetupState.NeedsDevOptions, s.on(EnvironmentChecked(false, false)))
        assertEquals(AdbSetupState.NeedsWirelessDebugging, s.on(EnvironmentChecked(true, false)))
        assertEquals(AdbSetupState.WaitingForPairingCode, s.on(EnvironmentChecked(true, true)))
    }

    @Test fun wirelessDebuggingImpliesDeveloperOptions() {
        // Android 17 reports development_settings_enabled as 0 to apps while wireless debugging is on.
        val s = AdbSetup(37)
        assertEquals(AdbSetupState.WaitingForPairingCode, s.on(EnvironmentChecked(false, true)))
    }

    @Test fun internalErrorIsAVisibleFailureAndRestartable() {
        val s = waiting()
        assertEquals(AdbSetupState.Failed(AdbFailure.INTERNAL), s.on(AdbSetupEvent.InternalError))
        assertEquals(AdbSetupState.Failed(AdbFailure.INTERNAL), s.on(EnvironmentChecked(true, true)))
        assertEquals(AdbSetupState.NeedsDevOptions, s.on(AdbSetupEvent.Restart))
    }

    @Test fun androidTenCannotPair() {
        val s = AdbSetup(29)
        assertEquals(AdbSetupState.Failed(AdbFailure.UNSUPPORTED), s.state)
        assertEquals(AdbSetupState.Failed(AdbFailure.UNSUPPORTED), s.on(AdbSetupEvent.Restart))
        assertEquals(AdbSetupState.Failed(AdbFailure.UNSUPPORTED), s.on(EnvironmentChecked(true, true)))
    }

    @Test fun happyPathReachesConnected() {
        val s = waiting()
        assertTrue(s.canSubmitCode)
        assertEquals(AdbSetupState.Pairing, s.on(CodeEntered("123456")))
        assertEquals(AdbSetupState.Paired, s.on(AdbSetupEvent.PairingFinished(true)))
        assertEquals(AdbSetupState.Connected, s.on(AdbSetupEvent.ConnectFinished(true)))
    }

    @Test fun codeWithoutPairingPortIsNotSent() {
        val s = AdbSetup(34)
        s.on(EnvironmentChecked(true, true))
        assertFalse(s.canSubmitCode)
        assertEquals(AdbSetupState.WaitingForPairingCode, s.on(CodeEntered("123456")))
        s.on(AdbSetupEvent.PairingPortFound(1))
        s.on(AdbSetupEvent.PairingPortLost)
        assertFalse(s.canSubmitCode)
    }

    @Test fun invalidCodeStaysWaitingButIsBounded() {
        val s = waiting()
        repeat(AdbSetup.MAX_INVALID_CODES - 1) { assertEquals(AdbSetupState.WaitingForPairingCode, s.on(CodeEntered("12"))) }
        assertEquals(0, s.pairingAttempts)
        assertEquals(AdbSetupState.Failed(AdbFailure.TOO_MANY_INVALID_CODES), s.on(CodeEntered("12")))
    }

    @Test fun rejectedPairingRetriesThreeTimesThenFails() {
        val s = waiting()
        repeat(AdbSetup.MAX_PAIRING_ATTEMPTS - 1) {
            assertEquals(AdbSetupState.Pairing, s.on(CodeEntered("111111")))
            assertEquals(AdbSetupState.WaitingForPairingCode, s.on(AdbSetupEvent.PairingFinished(false)))
        }
        s.on(CodeEntered("111111"))
        assertEquals(AdbSetupState.Failed(AdbFailure.PAIRING_REJECTED), s.on(AdbSetupEvent.PairingFinished(false)))
    }

    @Test fun failedConnectIsFinalUntilRestart() {
        val s = waiting()
        s.on(CodeEntered("123456")); s.on(AdbSetupEvent.PairingFinished(true))
        assertEquals(AdbSetupState.Failed(AdbFailure.CONNECT_FAILED), s.on(AdbSetupEvent.ConnectFinished(false)))
        // No automatic retry: events other than Restart change nothing.
        assertEquals(AdbSetupState.Failed(AdbFailure.CONNECT_FAILED), s.on(EnvironmentChecked(true, true, paired = true)))
        assertEquals(AdbSetupState.Failed(AdbFailure.CONNECT_FAILED), s.on(AdbSetupEvent.ConnectFinished(true)))
        assertEquals(AdbSetupState.NeedsDevOptions, s.on(AdbSetupEvent.Restart))
        assertEquals(0, s.pairingAttempts)
        assertNull(s.pairingPort)
    }

    @Test fun timeoutEndsWaitingAndPairing() {
        assertEquals(AdbSetupState.Failed(AdbFailure.TIMEOUT), waiting().on(AdbSetupEvent.WaitTimedOut))
        val s = waiting().apply { on(CodeEntered("123456")) }
        assertEquals(AdbSetupState.Failed(AdbFailure.TIMEOUT), s.on(AdbSetupEvent.WaitTimedOut))
    }

    @Test fun turningWirelessDebuggingOffWhileWaitingGoesBack() {
        val s = waiting()
        assertEquals(AdbSetupState.NeedsWirelessDebugging, s.on(EnvironmentChecked(true, false)))
        assertNull(s.pairingPort)
    }

    @Test fun environmentDoesNotInterruptRunningSteps() {
        val s = waiting()
        s.on(CodeEntered("123456"))
        assertEquals(AdbSetupState.Pairing, s.on(EnvironmentChecked(true, true)))
        s.on(AdbSetupEvent.PairingFinished(true))
        assertEquals(AdbSetupState.Paired, s.on(EnvironmentChecked(true, true)))
    }

    @Test fun alreadyPairedSkipsThePairingCode() {
        // After a reboot: the key is known to the phone, only wireless debugging has to be switched on again.
        val s = AdbSetup(34)
        assertEquals(AdbSetupState.NeedsWirelessDebugging, s.on(EnvironmentChecked(true, false, paired = true)))
        assertEquals(AdbSetupState.Paired, s.on(EnvironmentChecked(true, true, paired = true)))
    }

    @Test fun connectionDroppingAfterRebootIsNoticed() {
        val s = waiting()
        s.on(CodeEntered("123456")); s.on(AdbSetupEvent.PairingFinished(true)); s.on(AdbSetupEvent.ConnectFinished(true))
        assertEquals(AdbSetupState.NeedsWirelessDebugging, s.on(EnvironmentChecked(true, false, paired = true)))
    }

    @Test fun staleEventsAreIgnored() {
        val s = AdbSetup(34)
        assertEquals(AdbSetupState.NeedsDevOptions, s.on(AdbSetupEvent.PairingFinished(true)))
        assertEquals(AdbSetupState.NeedsDevOptions, s.on(AdbSetupEvent.ConnectFinished(true)))
        assertEquals(AdbSetupState.NeedsDevOptions, s.on(CodeEntered("123456")))
    }

    @Test fun onlyThisPhoneIsAllowed() {
        for (ok in listOf("127.0.0.1", "127.1.2.3", "localhost", "::1")) assertTrue(isLocalAdbHost(ok), ok)
        assertTrue(isLocalAdbHost("192.168.1.20", setOf("192.168.1.20")))
        for (bad in listOf("192.168.1.20", "8.8.8.8", "example.com", "127.0.0.1.evil.com", "")) assertFalse(isLocalAdbHost(bad), bad)
    }
}
