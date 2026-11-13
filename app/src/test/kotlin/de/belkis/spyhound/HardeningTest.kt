package de.belkis.spyhound

import de.belkis.spyhound.collect.DebugSwitches
import de.belkis.spyhound.scan.HardeningAction
import de.belkis.spyhound.scan.HardeningId
import de.belkis.spyhound.scan.HardeningInput
import de.belkis.spyhound.scan.HardeningItem
import de.belkis.spyhound.scan.HardeningState
import de.belkis.spyhound.scan.evaluateHardening
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HardeningTest {
    private val today = LocalDate.of(2026, 11, 2)
    private val safe = HardeningInput(
        screenLockSet = true, usbDebuggingOn = false, wifiDebuggingOn = false, developerOptionsOn = false,
        securityPatch = today.minusDays(30), unknownInstallerApps = emptyList(),
    )

    private fun List<HardeningItem>.of(id: HardeningId) = first { it.id == id }

    @Test
    fun safePhoneIsAllOk() {
        assertTrue(evaluateHardening(safe, today).all { it.state == HardeningState.OK })
    }

    @Test
    fun missingScreenLockIsImportant() {
        val item = evaluateHardening(safe.copy(screenLockSet = false), today).of(HardeningId.SCREEN_LOCK)
        assertEquals(HardeningState.IMPORTANT, item.state)
        assertEquals(HardeningAction.SECURITY_SETTINGS, item.action)
    }

    @Test
    fun debuggingAndDeveloperOptionsAreRecommended() {
        val items = evaluateHardening(
            safe.copy(usbDebuggingOn = true, wifiDebuggingOn = true, developerOptionsOn = true), today,
        )
        listOf(HardeningId.USB_DEBUGGING, HardeningId.WIFI_DEBUGGING, HardeningId.DEVELOPER_OPTIONS).forEach {
            assertEquals(HardeningState.RECOMMENDED, items.of(it).state)
            assertEquals(HardeningAction.DEVELOPER_SETTINGS, items.of(it).action)
        }
    }

    @Test
    fun wifiDebuggingIsFlaggedAsExpectedForTheDeepCheck() {
        assertTrue(evaluateHardening(safe.copy(wifiDebuggingOn = true), today).of(HardeningId.WIFI_DEBUGGING).note)
        assertFalse(evaluateHardening(safe.copy(usbDebuggingOn = true), today).of(HardeningId.USB_DEBUGGING).note)
    }

    @Test
    fun patchAgeBoundaries() {
        fun state(days: Long) =
            evaluateHardening(safe.copy(securityPatch = today.minusDays(days)), today).of(HardeningId.SECURITY_PATCH).state
        assertEquals(HardeningState.OK, state(90))
        assertEquals(HardeningState.RECOMMENDED, state(91))
        assertEquals(HardeningState.RECOMMENDED, state(365))
        assertEquals(HardeningState.IMPORTANT, state(366))
    }

    @Test
    fun patchOlderThanNinetyDaysCarriesItsAge() {
        val item = evaluateHardening(safe.copy(securityPatch = today.minusDays(120)), today).of(HardeningId.SECURITY_PATCH)
        assertEquals(HardeningState.RECOMMENDED, item.state)
        assertEquals(120, item.detail)
        assertEquals(HardeningAction.SYSTEM_UPDATE, item.action)
    }

    @Test
    fun futurePatchDateCountsAsFresh() {
        val item = evaluateHardening(safe.copy(securityPatch = today.plusDays(10)), today).of(HardeningId.SECURITY_PATCH)
        assertEquals(HardeningState.OK, item.state)
        assertEquals(0, item.detail)
    }

    @Test
    fun unknownPatchDateOmitsTheRow() {
        assertTrue(evaluateHardening(safe.copy(securityPatch = null), today).none { it.id == HardeningId.SECURITY_PATCH })
    }

    @Test
    fun installerAppsAreRecommendedWithCount() {
        val item = evaluateHardening(safe.copy(unknownInstallerApps = listOf("A", "B")), today)
            .of(HardeningId.UNKNOWN_APP_INSTALLERS)
        assertEquals(HardeningState.RECOMMENDED, item.state)
        assertEquals(2, item.detail)
        assertEquals(HardeningAction.UNKNOWN_APPS, item.action)
    }

    @Test
    fun orderIsStable() {
        val ids = evaluateHardening(safe, today).map { it.id }
        assertEquals(HardeningId.entries.toList(), ids)
    }

    @Test
    fun hiddenDebugSwitchesAreNeverShownAsOff() {
        // Android 17 reports all three as 0 to apps while they are on: no check mark then.
        val items = evaluateHardening(safe.copy(usbDebuggingOn = null, wifiDebuggingOn = null, developerOptionsOn = null), today)
        for (id in listOf(HardeningId.USB_DEBUGGING, HardeningId.WIFI_DEBUGGING, HardeningId.DEVELOPER_OPTIONS)) {
            assertEquals(HardeningState.UNKNOWN, items.of(id).state)
            assertEquals(HardeningAction.DEVELOPER_SETTINGS, items.of(id).action)
        }
        assertFalse(items.of(HardeningId.WIFI_DEBUGGING).note)
    }

    @Test
    fun zeroMeansUnknownFromAndroid17AndProofWins() {
        assertEquals(false, DebugSwitches.interpret(0, 36))
        assertEquals(null, DebugSwitches.interpret(0, 37))
        assertEquals(true, DebugSwitches.interpret(1, 37))
        assertEquals(null, DebugSwitches.interpret(null, 30))
        assertEquals(true, DebugSwitches.resolve(read = null, proven = true))
        assertEquals(false, DebugSwitches.resolve(read = null, proven = false))
        assertEquals(null, DebugSwitches.resolve(read = null, proven = null))
        assertEquals(false, DebugSwitches.resolve(read = false, proven = null))
    }
}
