package de.belkis.spyhound

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.FeedCatalog
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocStore
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.ShellAnalyzer
import de.belkis.spyhound.scan.TextScanner
import de.belkis.spyhound.shell.Shell
import de.belkis.spyhound.shell.forEachLineCapped
import de.belkis.spyhound.shell.readCapped
import de.belkis.spyhound.shell.streamProcess
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReviewFixesTest {
    private fun ind(value: String, type: IocType, android: Boolean = true, source: String = "s") =
        Indicator(value, type, Category.MERCENARY, "Fam", source, android)

    private val sha = "a".repeat(64)

    // 1: streaming instead of whole strings

    @Test fun linesAreSplitAndCapped() {
        val lines = ArrayList<String>()
        ByteArrayInputStream("a\nbb\r\nlast".toByteArray()).forEachLineCapped(1 shl 20) { lines += it }
        assertEquals(listOf("a", "bb\r", "last"), lines)
        lines.clear()
        ByteArrayInputStream("abc\ndef\nghi\n".toByteArray()).forEachLineCapped(6) { lines += it }
        assertEquals(listOf("abc", "de"), lines)
        assertEquals("abcd", ByteArrayInputStream("abcdefgh".toByteArray()).readCapped(4))
    }

    @Test fun endlessLineIsCutInsteadOfBuffered() {
        val big = ByteArray(300_000) { 'x'.code.toByte() }
        var longest = 0
        ByteArrayInputStream(big).forEachLineCapped(Int.MAX_VALUE) { longest = maxOf(longest, it.length) }
        assertTrue(longest <= 64 shl 10)
    }

    @Test fun hugeLogIsScannedLineByLineWithoutBeingCollected() {
        val set = IocSet(listOf(ind("evil.example", IocType.DOMAIN)))
        var produced = 0
        val r = ShellAnalyzer.logs(
            listOf("big" to ShellAnalyzer.LineSource { feed ->
                repeat(2_000_000) { i -> produced++; feed(if (i == 1_999_999) "c x.evil.example" else "filler line $i") }
                true
            }),
            set,
        )
        assertEquals(2_000_000, produced)
        assertEquals(1, r.findings.size)
        assertTrue(r.ran)
    }

    @Test fun defaultReadLinesFollowsRead() {
        val sh = object : Shell {
            override val level = AccessLevel.SHELL
            override fun read(command: String, maxBytes: Int, timeoutSec: Long) = if (command == "ok") "x\ny" else null
        }
        val got = ArrayList<String>()
        assertTrue(sh.readLines("ok", onLine = { got += it }))
        assertEquals(listOf("x", "y"), got)
        assertFalse(sh.readLines("bad", onLine = { got += it }))
    }

    // 2: processes and pipes on timeout

    @Test fun streamProcessReturnsOutput() {
        assertEquals("a\nb\n", streamProcess(listOf("sh", "-c", "printf 'a\\nb\\n'"), 10) { it.readCapped(1000) })
    }

    @Test fun timeoutEndsReadEvenIfAChildKeepsThePipeOpen() {
        val timeoutSec = 1L
        val t0 = System.nanoTime()
        // stdout stays open with no bytes until the process is killed on timeout.
        val r = streamProcess(
            listOf("sh", "-c", "tail -f /dev/null"),
            timeoutSec,
        ) { it.readCapped(1000) }
        val elapsedSec = (System.nanoTime() - t0) / 1_000_000_000.0
        assertNull(r)
        assertTrue(elapsedSec >= timeoutSec * 0.75, "expected timeout wait, was ${elapsedSec}s")
        assertTrue(elapsedSec < 30.0, "expected abort within 30s, was ${elapsedSec}s")
    }

    @Test fun timeoutKillsSilentProcess() {
        val t0 = System.nanoTime()
        assertNull(streamProcess(listOf("sleep", "30"), 1) { it.readCapped(1000) })
        assertTrue((System.nanoTime() - t0) / 1_000_000_000 < 15)
    }

    @Test fun missingProgramIsNull() {
        assertNull(streamProcess(listOf("/nonexistent/su"), 5) { it.readCapped(10) })
    }

    // 3: ios-only duplicates

    @Test fun androidDuplicateReplacesIosOnlyEntry() {
        val set = IocSet(listOf(ind("com.spy", IocType.APP_ID, android = false, source = "ios"), ind("com.spy", IocType.APP_ID, source = "and")))
        assertEquals("and", set.lookup(IocType.APP_ID, "com.spy")?.sourceId)
        assertEquals(1, set.size)
        val iosOnly = IocSet(listOf(ind("com.spy", IocType.APP_ID, android = false)))
        assertNull(iosOnly.lookup(IocType.APP_ID, "com.spy"))
    }

    // 4: URLs by lookup

    @Test fun urlMatchesAreSegmentBased() {
        val set = IocSet(listOf(ind("https://evil.example/a", IocType.URL), ind("http://6.6.6.6/login", IocType.URL)))
        val sc = TextScanner(set)
        assertEquals(1, sc.urls("see https://EVIL.example/a.").size)
        assertEquals(1, sc.urls("see https://evil.example/a/b?x=1").size)
        assertEquals(1, sc.urls("(http://6.6.6.6/login)").size)
        assertEquals(0, sc.urls("https://evil.example/ab").size)
        assertEquals(0, sc.urls("https://archive.example/web/https:/x/evil.example/a").size)
        assertEquals(0, sc.urls("https:///").size)
    }

    // 5: IPs and directory paths

    @Test fun ipsAreScanned() {
        val set = IocSet(listOf(ind("52.8.153.44", IocType.IP)))
        val sc = TextScanner(set)
        assertEquals(listOf("52.8.153.44"), sc.all("I/Net: connect 52.8.153.44:443").map { it.token })
        assertTrue(sc.ips("v152.8.153.44 and 52.8.153.44.7 and 52.8.153.445").isEmpty())
        val r = ShellAnalyzer.logs(mapOf("logcat" to "connect 52.8.153.44"), set)
        assertEquals(1, r.findings.size)
    }

    @Test fun directoryIndicatorMatchesPathMention() {
        val set = IocSet(listOf(ind("/data/local/tmp/wd/", IocType.FILE_PATH)))
        assertEquals(1, TextScanner(set).paths("dir /data/local/tmp/wd/ here").size)
        assertEquals(1, TextScanner(set).paths("dir /data/local/tmp/wd here").size)
        assertNull(set.lookup(IocType.FILE_PATH, "/data/local/tmp/wdx"))
    }

    // 6: package tokens behave as the scanner documents (no change needed)

    @Test fun packageTokensAreWholeAndCaseSensitive() {
        val set = IocSet(listOf(ind("com.evil.spy", IocType.APP_ID)))
        val sc = TextScanner(set)
        assertEquals(1, sc.appIds("ActivityRecord com.evil.spy/com.evil.spy.MainActivity").size)
        assertEquals(0, sc.appIds("com.evil.spy.123 com.evil.spy2").size)
        assertEquals(0, sc.appIds("Com.Evil.Spy").size)
    }

    // 8: file names and atomic replacement

    private fun feed(id: String, path: String) =
        """[{"id":"$id","name":"n","repo":"AmnestyTech/investigations","commit":"${"a".repeat(40)}","path":"$path","format":"stix2","cite":"","category":"mercenary","family":"f","sha256":"$sha"}]"""

    @Test fun sourceFileNamesCannotLeaveTheFolder() {
        assertEquals("ok-1.stix2", FeedCatalog.parse(feed("ok-1", "a/b.stix2")).single().fileName)
        assertFailsWith<IllegalArgumentException> { FeedCatalog.parse(feed("../evil", "a/b.stix2")) }
        assertFailsWith<IllegalArgumentException> { FeedCatalog.parse(feed("x", "a/b.c/d")) }
        assertFailsWith<IllegalArgumentException> { FeedCatalog.parse(feed("x\\y", "a/b.csv")) }
        assertFailsWith<IllegalArgumentException> { FeedCatalog.parse(feed("", "a/b.csv")) }
    }

    @Test fun replaceKeepsOldFileOnFailureAndSwapsOnSuccess() {
        val dir = File.createTempFile("ioc", "dir").apply { delete(); mkdirs() }
        try {
            val target = File(dir, "t.txt").apply { writeText("old") }
            IocStore.replaceAtomically(File(dir, "t.txt.part"), target, "new".toByteArray())
            assertEquals("new", target.readText())
            assertFalse(File(dir, "t.txt.part").exists())
            val missingDir = File(dir, "nope/t.part")
            assertFailsWith<java.io.IOException> { IocStore.replaceAtomically(missingDir, target, "x".toByteArray()) }
            assertEquals("new", target.readText())
        } finally {
            dir.deleteRecursively()
        }
    }

    // 9: failed or empty commands are gaps, never "checked"

    @Test fun failedOrEmptyFileAndHashCommandsAreGaps() {
        val set = IocSet(listOf(ind(sha, IocType.SHA256)))
        for (r in listOf(
            ShellAnalyzer.files(ShellAnalyzer.LineSource { false }, set),
            ShellAnalyzer.files("", set),
            ShellAnalyzer.hashes(ShellAnalyzer.LineSource { false }, set),
            ShellAnalyzer.hashes("\n", set),
        )) {
            assertFalse(r.ran)
            assertEquals(1, r.gaps.size)
        }
        val partial = ShellAnalyzer.hashes(ShellAnalyzer.LineSource { feed -> feed("$sha  /x"); false }, set)
        assertTrue(partial.ran)
        assertEquals(1, partial.findings.size)
        assertEquals(1, partial.gaps.size)
        val good = ShellAnalyzer.files("/sdcard/a\n", set)
        assertTrue(good.ran)
        assertTrue(good.gaps.isEmpty())
    }

    @Test fun failedLogCommandIsAGap() {
        val set = IocSet(emptyList())
        val r = ShellAnalyzer.logs(listOf("a" to ShellAnalyzer.LineSource { false }, "b" to ShellAnalyzer.LineSource { it("x"); true }), set)
        assertEquals(1, r.gaps.size)
        assertEquals(1, r.examined)
        assertNotNull(r)
    }
}
