package de.belkis.spyhound

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
}
