package de.belkis.spyhound

import de.belkis.spyhound.scan.SmsRequestOutcome
import de.belkis.spyhound.scan.smsRequestOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class SmsPermissionTest {
    @Test
    fun grantedWins() {
        assertEquals(SmsRequestOutcome.GRANTED, smsRequestOutcome(granted = true, shouldShowRationale = false))
        assertEquals(SmsRequestOutcome.GRANTED, smsRequestOutcome(granted = true, shouldShowRationale = true))
    }

    @Test
    fun deniedWithRationaleIsPlainDenial() {
        assertEquals(SmsRequestOutcome.DENIED, smsRequestOutcome(granted = false, shouldShowRationale = true))
    }

    @Test
    fun deniedWithoutRationaleMeansBlockedByAndroid() {
        assertEquals(
            SmsRequestOutcome.BLOCKED_BY_ANDROID,
            smsRequestOutcome(granted = false, shouldShowRationale = false),
        )
    }
}
