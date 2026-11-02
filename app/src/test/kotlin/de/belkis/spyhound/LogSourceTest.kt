package de.belkis.spyhound

import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.GapKind
import de.belkis.spyhound.scan.LogSource
import de.belkis.spyhound.scan.ShellAnalyzer
import de.belkis.spyhound.shell.Shell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSourceTest {
    /** Delivers [lines], then reports the time limit as reached when [finishes] is false. */
    private class FakeShell(private val lines: List<String>, private val finishes: Boolean) : Shell {
        override val level = AccessLevel.SHELL
        val timeouts = ArrayList<Long>()

        override fun read(command: String, maxBytes: Int, timeoutSec: Long): String? = null

        override fun readLines(command: String, maxBytes: Int, timeoutSec: Long, onLine: (String) -> Unit): Boolean {
            timeouts += timeoutSec
            lines.forEach(onLine)
            return finishes
        }
    }

    @Test fun logcatIsLimitedToFortyFiveSeconds() {
        val shell = FakeShell(listOf("a"), finishes = true)
        LogSource.read(shell, "logcat", "logcat -d", {}, {})
        LogSource.read(shell, "dumpsys package", "dumpsys package", {}, {})
        assertEquals(listOf(45L, 180L), shell.timeouts)
    }

    @Test fun timeLimitLeavesAnHonestGapInsteadOfHanging() {
        val shell = FakeShell(listOf("I/x: line one", "I/x: line two"), finishes = false)
        val source = ShellAnalyzer.LineSource { onLine -> LogSource.read(shell, "logcat", "logcat -d", onLine, {}) }
        val r = ShellAnalyzer.logs(listOf("logcat" to source), IocSet(emptyList()), emptySet())
        assertTrue(r.ran)
        assertEquals(1, r.gaps.size)
        assertEquals(GapKind.READ_FAILED, r.gaps.single().kind)
        assertTrue(r.gaps.single().detail.startsWith("logcat wurde nur teilweise gelesen"))
    }

    @Test fun progressIsThrottledAndCountsLines() {
        val shell = FakeShell(List(5) { "l$it" }, finishes = true)
        var clock = 0L
        val shown = ArrayList<String>()
        LogSource.read(shell, "logcat", "x", { clock += 400 }, { shown += it }, now = { clock })
        assertEquals("logcat · 0 Zeilen", shown.first())
        assertEquals(listOf("logcat · 0 Zeilen", "logcat · 4 Zeilen"), shown)
    }
}
