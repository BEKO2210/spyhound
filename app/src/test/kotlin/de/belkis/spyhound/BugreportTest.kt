package de.belkis.spyhound

import de.belkis.spyhound.bugreport.BUGREPORT_PREFIX
import de.belkis.spyhound.bugreport.BugreportAnalyzer
import de.belkis.spyhound.bugreport.BugreportParser
import de.belkis.spyhound.bugreport.analyzeBugreport
import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.ioc.Parsers
import de.belkis.spyhound.ioc.SourceContext
import de.belkis.spyhound.scan.ShellAnalyzer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BugreportTest {
    private val ctx = SourceContext("test", Category.MERCENARY, "TestSpy")
    private val stix = """
        {"type":"bundle","objects":[
          {"type":"malware","id":"malware--1","name":"Predator"},
          {"type":"indicator","id":"indicator--1","pattern":"[domain-name:value='evil.example']"},
          {"type":"indicator","id":"indicator--2","pattern":"[app:id='com.spy.agent']"},
          {"type":"indicator","id":"indicator--5","pattern":"[android-property:name='sys.brand.note']"},
          {"type":"indicator","id":"indicator--6","pattern":"[process:name='fservernetd'] OR [file:path='/data/local/tmp/spy']"},
          {"type":"relationship","relationship_type":"indicates","source_ref":"indicator--1","target_ref":"malware--1"}
        ]}
    """.trimIndent()
    private val iocs = IocSet(Parsers.stix2(stix, ctx))

    @Test fun hitsFromRealisticZip() {
        val zip = zipOf(
            "version.txt" to "2.0\n",
            "main_entry.txt" to "bugreport-dm3q-UP1A-2026-10-20-09-15-00.txt\n",
            "bugreport-dm3q-UP1A-2026-10-20-09-15-00.txt" to dumpstate(
                date = "${LocalDate.now()} 09:15:00",
                includeProcesses = true,
                includePackages = true,
                includeLogs = true,
                includeProps = true,
            ),
            "dumpstate_board.txt" to "board: ok\n",
            "FS/data/local/tmp/spy" to "x",
            "FS/data/misc/note.txt" to "y",
        )
        val parsed = BugreportParser.parse(ByteArrayInputStream(zip))
        assertEquals("samsung SM-S911B", parsed.device)
        assertTrue(parsed.packages.containsAll(listOf("com.ok.app", "com.spy.agent", "com.hidden.sys")), parsed.packages.toString())
        assertTrue("package" in parsed.dumpsys)
        assertTrue("dbinfo" in parsed.dumpsys)
        assertTrue(parsed.logcat.contains("x.evil.example"))
        assertEquals("1", parsed.props["sys.brand.note"])
        assertEquals(listOf("/data/local/tmp/spy", "/data/misc/note.txt"), parsed.fsPaths)
        assertTrue(parsed.boardText.contains("board: ok"))

        val checks = analyzeBugreport(ByteArrayInputStream(zip), iocs, setOf("com.ok.app"))
        assertTrue(checks.all { it.name.startsWith(BUGREPORT_PREFIX) }, checks.map { it.name }.toString())
        val findings = checks.flatMap { it.findings }
        assertTrue(findings.any { it.indicator.type == IocType.PROCESS && it.where.contains("fservernetd") }, findings.toString())
        assertTrue(findings.any { it.indicator.type == IocType.APP_ID && it.where.contains("com.spy.agent") }, findings.toString())
        assertTrue(findings.any { it.indicator.type == IocType.DOMAIN }, findings.toString())
        assertTrue(findings.any { it.indicator.type == IocType.FILE_PATH }, findings.toString())
        assertTrue(findings.any { it.indicator.type == IocType.ANDROID_PROP && it.where.contains("sys.brand.note") }, findings.toString())
        assertTrue(checks.none { it.leads.any { lead -> lead.where == "Alter des Berichts" } })
        assertTrue(checks.first { it.name == BUGREPORT_PREFIX + ShellAnalyzer.PROCESSES }.ran)
        assertTrue(checks.first { it.name == BUGREPORT_PREFIX + ShellAnalyzer.PACKAGES }.ran)
        assertTrue(checks.first { it.name == BUGREPORT_PREFIX + ShellAnalyzer.LOGS }.ran)
        assertTrue(checks.first { it.name == BUGREPORT_PREFIX + BugreportAnalyzer.PROPS }.ran)
    }

    @Test fun missingSectionsBecomeGaps() {
        val zip = zipOf(
            "bugreport-empty-2026-01-01-00-00-00.txt" to """
                ========================================================
                == dumpstate: 2026-01-01 00:00:00
                ========================================================
                Build: test
                Build fingerprint: 'google/test/test:14/XXX/1:user/release-keys'
            """.trimIndent() + "\n",
        )
        val checks = analyzeBugreport(ByteArrayInputStream(zip), iocs, emptySet())
        val gaps = checks.flatMap { it.gaps }
        assertTrue(gaps.any { it.check.endsWith(ShellAnalyzer.PROCESSES) && it.detail.contains("PROCESSES AND THREADS") }, gaps.toString())
        assertTrue(gaps.any { it.check.endsWith(ShellAnalyzer.PACKAGES) && it.detail.contains("package") }, gaps.toString())
        assertTrue(gaps.any { it.check.endsWith(ShellAnalyzer.LOGS) }, gaps.toString())
        assertTrue(gaps.any { it.check.endsWith(BugreportAnalyzer.PROPS) && it.detail.contains("getprop") }, gaps.toString())
        assertTrue(checks.flatMap { it.findings }.isEmpty())
        assertTrue(checks.all { it.name.startsWith(BUGREPORT_PREFIX) })
    }

    @Test fun oldReportGetsLead() {
        val zip = zipOf(
            "bugreport-old.txt" to dumpstate(
                date = "2019-01-01 12:00:00",
                includeProcesses = true,
                includePackages = true,
                includeLogs = true,
                includeProps = true,
            ),
        )
        val checks = BugreportAnalyzer.analyze(
            BugreportParser.parse(ByteArrayInputStream(zip)),
            iocs,
            emptySet(),
            today = LocalDate.of(2026, 11, 1),
        )
        val lead = checks.flatMap { it.leads }.first { it.where == "Alter des Berichts" }
        assertTrue(lead.detail.contains("2019-01-01"), lead.detail)
        assertEquals(BUGREPORT_PREFIX + BugreportAnalyzer.REPORT, lead.check)
    }

    @Test fun zipBombTooManyEntries() {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zos ->
            repeat(BugreportParser.MAX_ENTRIES + 1) { i ->
                zos.putNextEntry(ZipEntry("pad/$i"))
                zos.closeEntry()
            }
        }
        val checks = analyzeBugreport(ByteArrayInputStream(bytes.toByteArray()), iocs, emptySet())
        val gaps = checks.flatMap { it.gaps }
        assertTrue(gaps.any { it.detail.contains("zu viele Einträge") }, gaps.toString())
        assertTrue(checks.flatMap { it.findings }.isEmpty())
    }

    @Test fun zipBombHugeSectionIsCapped() {
        val line = "I/Net: filler " + "a".repeat(200) + "\n"
        val n = BugreportParser.MAX_SECTION_BYTES / line.length + 50
        val huge = buildString(n * line.length) { repeat(n) { append(line) } }
        val body = """
            ========================================================
            == dumpstate: ${LocalDate.now()} 00:00:00
            ========================================================
            Build: test

            ------ SYSTEM LOG (logcat -v threadtime -d *:v) ------
        """.trimIndent() + "\n" + huge + "------ EVENT LOG (logcat -b events) ------\nI/am: done\n"
        val zip = zipOf("bugreport-huge.txt" to body)
        val parsed = BugreportParser.parse(ByteArrayInputStream(zip))
        assertTrue(parsed.truncated)
        assertTrue(parsed.logcat.length <= BugreportParser.MAX_SECTION_BYTES)
        val checks = analyzeBugreport(ByteArrayInputStream(zip), iocs, emptySet())
        assertTrue(checks.flatMap { it.gaps }.any { it.detail.contains("32 MB") })
    }

    @Test fun rejectedZipSlipEntryOverByteBudgetStopsParse() {
        val marker = "MARKER_PS_fservernetd"
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zos ->
            zos.putNextEntry(ZipEntry("../bomb"))
            var remaining = BugreportParser.MAX_TOTAL_BYTES + 1
            val chunk = ByteArray(8192)
            while (remaining > 0) {
                val n = minOf(remaining, chunk.size.toLong()).toInt()
                zos.write(chunk, 0, n)
                remaining -= n
            }
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("bugreport-after-bomb.txt"))
            zos.write(
                dumpstate(
                    date = "${LocalDate.now()} 00:00:00",
                    includeProcesses = true,
                    includePackages = false,
                    includeLogs = false,
                    includeProps = false,
                ).replace("fservernetd", marker).toByteArray(Charsets.UTF_8),
            )
            zos.closeEntry()
        }
        val parsed = BugreportParser.parse(ByteArrayInputStream(bytes.toByteArray()))
        assertTrue(
            parsed.warnings.any { it.contains("entpackte Größe") },
            parsed.warnings.toString(),
        )
        assertTrue(parsed.psText.isEmpty(), parsed.psText)
    }

    @Test fun manyLogSectionsRespectRetainedTextBudget() {
        val sectionBody = "I/log: " + "x".repeat(1024) + "\n"
        val linesPerSection = (1024 * 1024) / sectionBody.length + 1
        val section = buildString(linesPerSection * sectionBody.length) { repeat(linesPerSection) { append(sectionBody) } }
        val sections = 100
        val body = buildString {
            appendLine("========================================================")
            appendLine("== dumpstate: ${LocalDate.now()} 00:00:00")
            appendLine("========================================================")
            appendLine("Build: test")
            repeat(sections) { i ->
                appendLine("------ SYSTEM LOG (logcat part $i) ------")
                append(section)
                appendLine("------ 0.001s was the duration of 'SYSTEM LOG' ------")
            }
        }
        val zip = zipOf("bugreport-many-logs.txt" to body)
        val parsed = BugreportParser.parse(ByteArrayInputStream(zip))
        assertTrue(parsed.truncated, "expected retained-text cap to set truncated")
        assertTrue(
            parsed.logcat.toByteArray(Charsets.UTF_8).size <= BugreportParser.MAX_RETAINED_TEXT_BYTES + 64 * 1024,
            "logcat bytes=${parsed.logcat.toByteArray(Charsets.UTF_8).size}",
        )
    }

    @Test fun manyDumpsysSectionsRespectRetainedTextBudget() {
        val chunk = "x\n".repeat(512 * 1024)
        val body = buildString {
            appendLine("========================================================")
            appendLine("== dumpstate: ${LocalDate.now()} 00:00:00")
            appendLine("========================================================")
            appendLine("Build: test")
            repeat(100) { i ->
                appendLine("-------------------------------------------------------------------------------")
                appendLine("DUMP OF SERVICE svc$i:")
                append(chunk)
            }
        }
        val zip = zipOf("bugreport-many-dumpsys.txt" to body)
        val parsed = BugreportParser.parse(ByteArrayInputStream(zip))
        assertTrue(parsed.truncated)
        val kept = parsed.dumpsys.values.sumOf { it.toByteArray(Charsets.UTF_8).size.toLong() }
        assertTrue(
            kept <= BugreportParser.MAX_RETAINED_TEXT_BYTES + 64 * 1024,
            "kept dumpsys bytes=$kept",
        )
    }

    @Test fun overlongLineSetsTruncated() {
        val longLine = "I/tag: " + "z".repeat(BugreportParser.MAX_LINE_BYTES + 500)
        val body = """
            ========================================================
            == dumpstate: ${LocalDate.now()} 00:00:00
            ========================================================
            Build: test

            ------ SYSTEM LOG (logcat -v threadtime -d *:v) ------
            $longLine
            I/after: ok
        """.trimIndent() + "\n"
        val zip = zipOf("bugreport-long-line.txt" to body)
        val parsed = BugreportParser.parse(ByteArrayInputStream(zip))
        assertTrue(parsed.truncated, "expected overlong line to set truncated")
    }

    @Test fun zipSlipPathsAreIgnored() {
        val zip = zipOf(
            "../tmp/pwned" to "nope",
            "FS/../etc/passwd" to "nope",
            "bugreport-ok.txt" to dumpstate(
                date = "${LocalDate.now()} 00:00:00",
                includeProcesses = true,
                includePackages = false,
                includeLogs = false,
                includeProps = false,
            ),
        )
        val parsed = BugreportParser.parse(ByteArrayInputStream(zip))
        assertTrue(parsed.fsPaths.none { it.contains("..") }, parsed.fsPaths.toString())
        assertTrue(parsed.psText.contains("fservernetd"))
    }

    @Test fun durationLineDoesNotEndSection() {
        val zip = zipOf(
            "bugreport.txt" to """
                ========================================================
                == dumpstate: ${LocalDate.now()} 00:00:00
                ========================================================
                Build: test

                ------ SYSTEM LOG (logcat -v threadtime -d *:v) ------
                01-01 00:00:00.000  1  1 I start
                ------ 0.010s was the duration of 'SOMETHING ELSE' ------
                01-01 00:00:01.000  1  1 I Net: connect x.evil.example:443
                ------ EVENT LOG (logcat -b events -v threadtime -d *:v) ------
                01-01 00:00:02.000  1  1 I am_pss: 1
            """.trimIndent() + "\n",
        )
        val parsed = BugreportParser.parse(ByteArrayInputStream(zip))
        assertTrue(parsed.logcat.contains("x.evil.example"), parsed.logcat)
        val logs = analyzeBugreport(ByteArrayInputStream(zip), iocs, emptySet())
            .first { it.name == BUGREPORT_PREFIX + ShellAnalyzer.LOGS }
        assertEquals(1, logs.findings.size)
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            for ((name, content) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun dumpstate(
        date: String,
        includeProcesses: Boolean,
        includePackages: Boolean,
        includeLogs: Boolean,
        includeProps: Boolean,
    ): String = buildString {
        appendLine("========================================================")
        appendLine("== dumpstate: $date")
        appendLine("========================================================")
        appendLine()
        appendLine("Build: UP1A.231005.007")
        appendLine("Build fingerprint: 'samsung/dm3qxxx/dm3q:14/UP1A.231005.007/S911BXXS6CXG8:user/release-keys'")
        appendLine("Bootloader: S911BXXS6CXG8")
        appendLine("Radio: unknown")
        appendLine("Network: unknown")
        appendLine()
        if (includeProps) {
            appendLine("------ SYSTEM PROPERTIES (getprop) ------")
            appendLine("[ro.product.manufacturer]: [samsung]")
            appendLine("[ro.product.model]: [SM-S911B]")
            appendLine("[sys.brand.note]: [1]")
            appendLine("[ro.build.fingerprint]: [samsung/dm3qxxx/dm3q:14/UP1A.231005.007/S911BXXS6CXG8:user/release-keys]")
            appendLine("------ 0.050s was the duration of 'SYSTEM PROPERTIES' ------")
            appendLine()
        }
        if (includeProcesses) {
            appendLine("------ PROCESSES AND THREADS (ps -A -T -Z -O pri,nice,rtprio,sched,pcy,time) ------")
            appendLine("LABEL                          USER           PID   TID  PPID     VSZ    RSS WCHAN            ADDR         S  PRI  NI RTPRIO SCHED PCY          TIME NAME")
            appendLine("u:r:init:s0                    root             1     1     0    1234    432 0                   0         S    0   0     -1     0  fg      00:00:01 init")
            appendLine("u:r:untrusted_app:s0           u0_a123      12345 12345   890  123456   4321 0                   0         S   10   0     -1     0  fg      00:00:05 fservernetd")
            appendLine("u:r:untrusted_app:s0           u0_a123      12345 12346   890  123456   4321 0                   0         S   10   0     -1     0  fg      00:00:00 HeapTaskDaemon")
            appendLine("u:r:untrusted_app:s0           u0_a200      20001 20001   890  222222   2222 0                   0         S   10   0     -1     0  fg      00:00:01 com.spy.agent")
            appendLine("------ 0.200s was the duration of 'PROCESSES AND THREADS' ------")
            appendLine()
        }
        if (includePackages) {
            appendLine("-------------------------------------------------------------------------------")
            appendLine("DUMP OF SERVICE package:")
            appendLine("Activity Resolver Table:")
            appendLine("  Full MIME Types:")
            appendLine("Packages:")
            appendLine("  Package [com.ok.app] (aaa):")
            appendLine("    userId=10001")
            appendLine("    installerPackageName=com.android.vending")
            appendLine("    User 0:  installed=true hidden=false")
            appendLine("  Package [com.spy.agent] (bbb):")
            appendLine("    userId=10002")
            appendLine("    installerPackageName=null")
            appendLine("    User 0:  installed=false hidden=false stopped=true")
            appendLine("      dataDir=/data/user/0/com.spy.agent")
            appendLine("Hidden system packages:")
            appendLine("  Package [com.hidden.sys] (ccc):")
            appendLine("    userId=10003")
            appendLine("    User 0:  installed=true hidden=true")
            appendLine("Queries:")
            appendLine("  ...")
            appendLine("--------- 1.000s was the duration of dumpsys package, ending at: $date")
            appendLine()
            appendLine("-------------------------------------------------------------------------------")
            appendLine("DUMP OF SERVICE dbinfo:")
            appendLine("DbInfo")
            appendLine("--------- 0.010s was the duration of dumpsys dbinfo, ending at: $date")
            appendLine()
        }
        if (includeLogs) {
            appendLine("------ SYSTEM LOG (logcat -v threadtime -d *:v) ------")
            appendLine("10-20 09:14:00.000  1000  1000 I start")
            appendLine("------ 0.001s was the duration of 'UNRELATED' ------")
            appendLine("10-20 09:14:00.100  1000  1000 I Net: connect x.evil.example:443 from com.spy.agent /data/local/tmp/spy")
            appendLine("------ 0.300s was the duration of 'SYSTEM LOG' ------")
            appendLine()
            appendLine("------ EVENT LOG (logcat -b events -v threadtime -d *:v) ------")
            appendLine("10-20 09:14:01.000  1000  1000 I am_pss: 123")
            appendLine("------ 0.050s was the duration of 'EVENT LOG' ------")
            appendLine()
            appendLine("------ RADIO LOG (logcat -b radio -v threadtime -d *:v) ------")
            appendLine("10-20 09:14:02.000  1000  1000 D RIL: ok")
            appendLine("------ 0.050s was the duration of 'RADIO LOG' ------")
        }
    }
}
