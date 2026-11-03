package de.belkis.spyhound

import de.belkis.spyhound.ioc.*
import java.io.File
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IocHardeningTest {
    private val ctx = SourceContext("test", Category.MERCENARY)
    private val dir = File("src/main/assets/iocs")
    private fun stix(pattern: String, revoked: Boolean = false): String =
        """{"objects":[{"type":"indicator","revoked":$revoked,"pattern":${JsonPrimitive(pattern)}}]}"""

    @Test fun stixRejectsCompoundConditionsAndQualifiers() {
        val a = "domain-name:value = 'benign.example'"
        val b = "file:hashes.'SHA-256' = '${"a".repeat(64)}'"
        for (pattern in listOf("[$a AND $b]", "[$a] FOLLOWEDBY [$b]", "[$a] AND [$b]",
            "[$a] WITHIN 10 SECONDS", "[$a] REPEATS 2 TIMES", "[$a OR $b] junk", "[C OR $a]",
            "[$a OR $b AND $a]", "[$a OR domain-name:value LIKE 'other.example']")) {
            assertTrue(Parsers.stix2(stix(pattern), ctx).isEmpty(), pattern)
        }
    }

    @Test fun stixSkipsRevokedIndicators() {
        assertTrue(Parsers.stix2(stix("[domain-name:value = 'benign.example']", true), ctx).isEmpty())
    }

    @Test fun stixKeepsSingleComparisonsAndDisjunctions() {
        val a = "domain-name:value = 'one.example'"
        val b = "domain-name:value = 'two.example'"
        for (pattern in listOf("[$a OR $b]", "[$a] OR [$b]")) {
            assertEquals(listOf("one.example", "two.example"), Parsers.stix2(stix(pattern), ctx).map { it.value })
        }
        assertEquals("/tmp/AND OR FOLLOWEDBY", Parsers.stix2(stix("[file:path = '/tmp/AND OR FOLLOWEDBY']"), ctx).single().value)
    }

    @Test fun mispQuotedNewlinesNeverBecomeIndicators() {
        val csv = "type,value,comment\r\ndomain,real.example,\"first\r\ndomain,benign.example,fake\r\nlast\"\r\ndomain,next.example,ok"
        assertEquals(listOf("real.example", "next.example"), Parsers.mispCsv(csv, ctx).map { it.value })
        assertEquals("first\r\ndomain,benign.example,fake\r\nlast", Parsers.csvRows(csv)[1][2])
    }

    @Test fun csvMalformedRecordsAreDroppedWhole() {
        assertEquals(listOf(listOf("ok", "value")), Parsers.csvRows("domain,\"bad\"suffix\nok,value"))
        assertEquals(listOf(listOf("type", "value")), Parsers.csvRows("type,value\ndomain,\"open\ndomain,benign.example"))
        assertEquals(listOf(listOf("a,b", "say \"hi\"", "")), Parsers.csvRows("\"a,b\",\"say \"\"hi\"\"\","))
    }

    @Test fun catalogRequiresFullLowercaseCommit() {
        for (commit in listOf("main", "a".repeat(39), "a".repeat(41), "A".repeat(40), "g".repeat(40), "")) {
            assertFailsWith<IllegalArgumentException>(commit) {
                FeedCatalog.parse("""[{"id":"x","repo":"mvt-project/mvt-indicators","path":"x.txt","commit":"$commit"}]""")
            }
        }
        assertEquals(1, FeedCatalog.parse("""[{"id":"x","repo":"mvt-project/mvt-indicators","path":"x.txt","commit":"${"a".repeat(40)}"}]""").size)
    }

    @Test fun oversizedRecordsAreSkippedWithoutLeakingMiddleLines() {
        assertTrue(Parsers.csvRows("x,".repeat(64) + "x").isEmpty())
        assertEquals(64, Parsers.csvRows("x,".repeat(63) + "x").single().size)
        assertEquals(8192, Parsers.csvRows("x".repeat(8192)).single().single().length)
        assertTrue(Parsers.csvRows("x".repeat(8193)).isEmpty())
        val csv = "type,value,comment\ndomain,oversize.example,\"${"x".repeat(8193)}\ndomain,benign.example,fake\nend\"\ndomain,valid.example,ok"
        assertEquals(listOf("valid.example"), Parsers.mispCsv(csv, ctx).map { it.value })
        assertTrue(Parsers.curatedTsv("bad.example\t" + "x\t".repeat(64), ctx).isEmpty())
        assertTrue(Parsers.domainsTxt("x".repeat(8193), ctx).isEmpty())
        assertTrue(Parsers.stix2(stix("[file:path='${"x".repeat(8193)}']"), ctx).isEmpty())
    }

    @Test fun indicatorLimitRejectsWholeSource() {
        val row = "domain,evil.example\n"
        assertEquals(200_000, Parsers.mispCsv("type,value\n" + row.repeat(200_000), ctx).size)
        val inputs = mapOf(
            "misp-csv" to ("type,value\n" + row.repeat(200_001)),
            "domains-txt" to "evil.example\n".repeat(200_001),
            "curated-tsv" to "evil.example\tx\tx\n".repeat(200_001),
            "hashlist" to ("a".repeat(64) + "\n").repeat(200_001),
            "csv-domain-ip" to ("domain,ip\n" + "evil.example,1.2.3.4\n".repeat(100_001)),
            "typed-csv-android-sha256" to ("sha256," + "a".repeat(64) + ",Android\n").repeat(200_001),
            "stix2" to ("{\"objects\":[" + List(200_001) {
                """{"type":"indicator","pattern":"[domain-name:value='evil.example']"}"""
            }.joinToString(",") + "]}"),
        )
        for ((format, text) in inputs) assertFailsWith<IocFormatException>(format) { Parsers.parse(format, text, ctx); Unit }
    }

    @Test fun curatedHashMismatchRejectsWholeSourceAndReportsCorruption() {
        val all = ArrayList<Indicator>()
        val corrupt = ArrayList<FeedSource>()
        IocStore.appendCuratedIndicators(all, corrupt) { name ->
            File(dir, name).readBytes() + "\nbenign.example\tx\tx\n".toByteArray()
        }
        assertTrue(all.isEmpty())
        assertEquals(IocStore.CURATED.toSet(), corrupt.map { it.fileName }.toSet())
    }

    @Test fun curatedAssetsPassPinsAndKnownGoodCheck() {
        KnownGood.useSystemList(File(dir, IocStore.KNOWN_GOOD).readText())
        val all = ArrayList<Indicator>()
        val corrupt = ArrayList<FeedSource>()
        IocStore.appendCuratedIndicators(all, corrupt) { File(dir, it).readBytes() }
        assertTrue(corrupt.isEmpty())
        assertEquals(428, all.size)
        assertTrue(KnownGood.conflicts(all).isEmpty())
    }

    @Test fun unreadableCuratedAssetsAreReportedAsCorrupt() {
        val all = ArrayList<Indicator>()
        val corrupt = ArrayList<FeedSource>()
        IocStore.appendCuratedIndicators(all, corrupt) { throw java.io.IOException("unreadable") }
        assertTrue(all.isEmpty())
        assertEquals(IocStore.CURATED.toSet(), corrupt.map { it.fileName }.toSet())
    }

    @Test fun curatedKnownGoodConflictRejectsWholeSource() {
        val bytes = File(dir, IocStore.CURATED.first()).readBytes()
        val hash = Parsers.curatedTsv(bytes.toString(Charsets.UTF_8), ctx).first { it.type == IocType.SHA256 }.value
        try {
            KnownGood.useSystemList(hash)
            val all = ArrayList<Indicator>()
            val corrupt = ArrayList<FeedSource>()
            IocStore.appendCuratedIndicators(all, corrupt) { File(dir, it).readBytes() }
            assertEquals(listOf(IocStore.CURATED.first()), corrupt.map { it.fileName })
            assertEquals(23, all.size)
        } finally {
            KnownGood.useSystemList(File(dir, IocStore.KNOWN_GOOD).readText())
        }
    }

    @Test fun bundledIndicatorCountsStayPinned() {
        val expected = mapOf(
            "mvt-predator" to 582, "mvt-rcs-lab" to 40, "echap-stalkerware" to 6086,
            "mvt-quadream-kingspawn" to 167, "mvt-operation-triangulation" to 112,
            "mvt-wyrmspy-dragonegg" to 53, "mvt-eaglemsgspy" to 51, "mvt-candiru" to 125,
            "mvt-residentbat" to 48, "mvt-cellebrite" to 1, "mvt-darksword" to 43,
            "mvt-coruna" to 215, "mvt-morpheus" to 8, "mvt-btmob" to 162, "mvt-spyrtacus" to 9,
            "eset-aridspy" to 15, "eset-vajraspy" to 13, "eset-spyloan" to 17, "eset-ngate" to 25,
            "eset-promptspy" to 9, "eset-callphantom" to 28, "eset-evilvideo" to 1, "eset-ghostchat" to 2,
            "eset-gravityrat" to 3, "eset-gref" to 5, "eset-kamran" to 1, "eset-prospytospy" to 12,
            "eset-pwa-phishing" to 2, "eset-virtual-invaders" to 20, "meta-pss-android-2021" to 13,
            "adfection_iocs.txt" to 405, "curated_mercenary.txt" to 23,
        )
        val actual = FeedCatalog.parse(File(dir, "sources.json").readText()).filter { it.bundled }.associate { s ->
            s.id to Parsers.parse(s.format, File(dir, s.fileName).readText(), SourceContext(s.id, s.category, s.family, s.androidArtifacts)).size
        }.toMutableMap()
        for (name in IocStore.CURATED) actual[name] = Parsers.curatedTsv(File(dir, name).readText(), ctx).size
        assertEquals(expected, actual)
    }
}
