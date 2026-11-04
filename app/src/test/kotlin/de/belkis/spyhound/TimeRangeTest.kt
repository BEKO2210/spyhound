package de.belkis.spyhound

import de.belkis.spyhound.scan.AppRecord
import de.belkis.spyhound.scan.TimeRange
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.util.Locale

class TimeRangeTest {
    private val now = 1_735_689_600_000L

    @Test fun formatsSourcesAndWarnsForShortLogHistory() {
        val result = TimeRange.evaluate(mapOf(
            TimeRange.SMS to now - 86_400_000L,
            TimeRange.LOGS to now - 12L * 3_600_000,
            TimeRange.FILES to now - 7L * 86_400_000,
            TimeRange.APPS to now - 30L * 86_400_000,
        ), now)
        assertEquals(4, result.sources.size)
        assertTrue(result.warning)
        assertTrue(result.sources.first { it.source == TimeRange.LOGS }.text.contains("12 Stunden"))
    }

    @Test fun logsAtLeast48HoursHaveNoWarningAndMissingSourcesStayMissing() {
        val result = TimeRange.evaluate(mapOf(TimeRange.LOGS to now - 48L * 3_600_000), now, Locale.ENGLISH)
        assertFalse(result.warning)
        assertEquals("System logs: last 48 hours only", result.sources.single().text)
    }

    @Test fun parsesBriefLogcatTimestamp() {
        val timestamp = TimeRange.logcatTimestamp("11-02 09:15:10.123  123  456 I Tag: text", now)
        assertTrue(timestamp != null && timestamp < now)
    }

    @Test fun appRangeDropsInstallsOlderThanTheShippedAndroidVersion() {
        val samsungPlaceholder = 1_230_735_600_000L // 2008-12-31 15:00 UTC, preinstalled apps on a Galaxy S21
        val pixelFirstBoot = 1_577_836_807_000L // 2020-01-01 00:00:07 UTC, clock not yet set at setup
        val real = 1_729_000_000_000L // 2024-10-15
        val galaxy = listOf(AppRecord("a", isSystem = true, firstInstall = samsungPlaceholder),
            AppRecord("b", firstInstall = samsungPlaceholder), AppRecord("c", firstInstall = real), AppRecord("d"))
        assertEquals(real, TimeRange.oldestAppInstall(galaxy, initialSdk = 30))
        val pixel = listOf(AppRecord("e", firstInstall = pixelFirstBoot), AppRecord("f", firstInstall = now))
        assertEquals(now, TimeRange.oldestAppInstall(pixel, initialSdk = 36))
        // Unknown shipped version: only the pre-2010 firmware placeholder is dropped.
        assertEquals(pixelFirstBoot, TimeRange.oldestAppInstall(pixel + galaxy.take(1), initialSdk = 0))
    }
}
