package de.belkis.spyhound

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.FeedCatalog
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.KnownGood
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocStore
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.ioc.Parsers
import de.belkis.spyhound.ioc.SourceContext
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.ApkFile
import de.belkis.spyhound.scan.AppAnalyzer
import de.belkis.spyhound.scan.AppRecord
import de.belkis.spyhound.scan.GapKind
import de.belkis.spyhound.scan.Report
import de.belkis.spyhound.scan.ShellAnalyzer
import de.belkis.spyhound.scan.TextsEn
import de.belkis.spyhound.scan.Verdict
import de.belkis.spyhound.scan.guidanceFor
import java.io.File
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Offline antivirus step 1: exact SHA-256 matches against the ESET malware-ioc Android lists. */
class AntivirusTest {
    private val dir = File("src/main/assets/iocs")
    private val sources = FeedCatalog.parse(File(dir, "sources.json").readText())
    private val eset = sources.filter { it.repo == "eset/malware-ioc" }
    private val meta = sources.filter { it.repo == "meta/threat-research" }
    private val esetIocs = IocSet((eset + meta).flatMap { load(it) })

    private fun load(s: de.belkis.spyhound.ioc.FeedSource) =
        Parsers.parse(s.format, File(dir, s.fileName).readText(), SourceContext(s.id, s.category, s.family, s.androidArtifacts))

    // First lines of the real lists at commit 59745502656e (aridspy, promptspy; promptspy is upper case upstream).
    private val aridspySample = "0cb41557841ff6f314c398250a165706e0b18f93674a7c12f4489018a1661673"
    private val promptspySample = "DB7A1C352C7D1AC75E0BA31B71CF4B0E3304A22B8D0D636FA80B5D5095BE1E00"

    private fun ctx() = SourceContext("t", Category.MALWARE, "Fam")

    // A2: hashlist parser
    @Test fun hashlistAcceptsValidLines() {
        val out = Parsers.hashlist("$aridspySample\n", ctx())
        assertEquals(1, out.size)
        assertEquals(IocType.SHA256, out[0].type)
        assertEquals(Category.MALWARE, out[0].category)
        assertEquals("Fam", out[0].family)
    }

    @Test fun hashlistRejectsShortLongAndNonHex() {
        val text = listOf(aridspySample.dropLast(1), aridspySample + "0", "g".repeat(64), "text").joinToString("\n")
        assertTrue(Parsers.hashlist(text, ctx()).isEmpty())
    }

    @Test fun hashlistLowercasesUpperCase() {
        assertEquals(promptspySample.lowercase(), Parsers.hashlist(promptspySample, ctx()).single().value)
    }

    @Test fun hashlistHandlesCommentsCrlfAndBlankLines() {
        val text = "# ESET list\r\n\r\n$aridspySample  # trailing comment\r\n   \r\n$promptspySample\r\n"
        assertEquals(2, Parsers.hashlist(text, ctx()).size)
    }

    @Test fun hashlistEmptyFile() {
        assertTrue(Parsers.hashlist("", ctx()).isEmpty())
        assertTrue(Parsers.parse("hashlist", "\n\n", ctx()).isEmpty())
    }

    // A1: catalog, pins, licence record
    @Test fun esetSourcesArePinnedAndVerified() {
        assertEquals(
            setOf(
                "aridspy", "vajraspy", "spyloan", "ngate", "promptspy", "callphantom", "evilvideo", "ghostchat",
                "gravityrat", "gref", "kamran", "prospytospy", "pwa-phishing", "virtual-invaders",
            ),
            eset.map { it.id.removePrefix("eset-") }.toSet(),
        )
        for (s in eset) {
            assertTrue(s.bundled, s.id)
            assertEquals("hashlist", s.format)
            assertEquals(Category.MALWARE, s.category)
            assertEquals("59745502656e813f9f79fc8005243edbc04f9397", s.commit)
            assertTrue(s.family.isNotBlank())
            val bytes = File(dir, s.fileName).readBytes()
            assertEquals(s.sha256, IocStore.sha256(bytes), s.id)
            val tampered = bytes.copyOf().also { it[0] = if (it[0] == '0'.code.toByte()) '1'.code.toByte() else '0'.code.toByte() }
            assertTrue(IocStore.sha256(tampered) != s.sha256, "a changed byte must break the pin")
        }
        val notice = File("../NOTICE").readText()
        assertTrue("BSD 2-Clause" in notice && "ESET spol. s r.o." in notice && "eset/malware-ioc" in notice)
    }

    @Test fun sourceOutsideAllowListIsRejected() {
        val json = """[{"id":"x-evil","name":"x","repo":"evil/malware-ioc","commit":"0","path":"a/samples.sha256","format":"hashlist","cite":"","category":"malware","family":"F","sha256":"0"}]"""
        assertFailsWith<IllegalArgumentException> { FeedCatalog.parse(json) }
        assertTrue("eset/malware-ioc" in FeedCatalog.ALLOWED_REPOS)
    }

    // A3 (JVM part): real hashes land in the lookup table
    @Test fun realEsetHashesAreLoaded() {
        assertEquals(166, esetIocs.count(IocType.SHA256))
        assertEquals("AridSpy", esetIocs.lookup(IocType.SHA256, aridspySample)?.family)
        assertEquals("PromptSpy", esetIocs.lookup(IocType.SHA256, promptspySample)?.family)
        assertTrue(esetIocs.all(IocType.SHA256).all { it.category == Category.MALWARE })
    }

    // Only exact matches count
    @Test fun nearMissesAreNoFindings() {
        assertNull(esetIocs.lookup(IocType.SHA256, aridspySample.dropLast(1)))
        assertNull(esetIocs.lookup(IocType.SHA256, aridspySample.dropLast(1) + "0"))
        assertNull(esetIocs.lookup(IocType.SHA256, aridspySample.take(32)))
    }

    // A4 (JVM part): the hash of nothing and of a trivial file is never a malware sample
    @Test fun commonHashesDoNotCollide() {
        val empty = IocStore.sha256(ByteArray(0))
        val hello = IocStore.sha256("hello".toByteArray())
        assertNull(esetIocs.lookup(IocType.SHA256, empty))
        assertNull(esetIocs.lookup(IocType.SHA256, hello))
        val others = sources.filter { it.repo != "eset/malware-ioc" && it.repo != "meta/threat-research" && it.bundled }.flatMap { load(it) }
        val mine = esetIocs.all(IocType.SHA256).map { it.value }.toSet()
        assertTrue(others.none { it.type == IocType.SHA256 && it.value.lowercase() in mine })
    }

    private fun app(vararg sha: String?) = AppRecord(
        packageName = "com.example.app", label = "Example",
        apks = sha.mapIndexed { i, s -> ApkFile("/data/app/x/split$i.apk", s) },
    )

    @Test fun installedApkWithExactHashIsMalwareFinding() {
        val r = AppAnalyzer.analyze(listOf(app("f".repeat(64), aridspySample)), esetIocs, "de.belkis.spyhound")
        val f = r.findings.single()
        assertEquals(Category.MALWARE, f.indicator.category)
        assertTrue("AridSpy" in f.detail && "ESET" in f.detail, f.detail)
        assertTrue("Spyware" !in f.detail)
        assertEquals("guide_finding_malware_meaning", guidanceFor(f).meaningKey)
        val report = Report(0, 1, AccessLevel.APP, 166, 5, 0, listOf(r), "dev")
        assertEquals(Verdict.MALWARE, report.verdict)
    }

    @Test fun installedAppWithoutHashMatchIsNoFinding() {
        val r = AppAnalyzer.analyze(listOf(app("f".repeat(64))), esetIocs, "de.belkis.spyhound")
        assertTrue(r.findings.isEmpty())
        assertEquals(Verdict.NOTHING_FOUND, Report(0, 1, AccessLevel.APP, 166, 5, 0, listOf(r), "dev").verdict)
    }

    @Test fun apkInDownloadsIsMatchedByFileHash() {
        val out = "$promptspySample  /storage/emulated/0/Download/update.apk\n${"e".repeat(64)}  /storage/emulated/0/Download/other.apk\n"
        val r = ShellAnalyzer.hashes(out, esetIocs)
        assertEquals(2, r.examined)
        val f = r.findings.single()
        assertTrue(f.where.endsWith("update.apk"))
        assertTrue("PromptSpy" in f.detail && "Schadsoftware" in f.detail, f.detail)
    }

    // Honest gap
    @Test fun coverageGapNamesTheLimit() {
        val r = AppAnalyzer.analyze(emptyList(), esetIocs, "de.belkis.spyhound")
        val gap = r.gaps.single { it.kind == GapKind.MALWARE_COVERAGE }
        assertTrue("166" in gap.detail && "ausgewählten Kampagnen" in gap.detail && "Play Protect" in gap.detail, gap.detail)
        assertEquals("guide_gap_malware_coverage_meaning", guidanceFor(gap).meaningKey)
        assertTrue(AppAnalyzer.analyze(emptyList(), IocSet(emptyList()), "x").gaps.none { it.kind == GapKind.MALWARE_COVERAGE })
    }

    @Test fun englishTextsExist() {
        val r = AppAnalyzer.analyze(listOf(app(aridspySample)), esetIocs, "de.belkis.spyhound")
        val f = r.findings.single()
        val en = TextsEn.text(f.detail)
        assertTrue("known malware sample" in en && "AridSpy" in en, en)
        val gap = TextsEn.text(r.gaps.single { it.kind == GapKind.MALWARE_COVERAGE }.detail)
        assertTrue("selected campaigns" in gap && "Play Protect" in gap, gap)
        assertFalse(en.contains("Schadsoftware"))
        assertNotNull(f.indicator.sourceId)
    }

    // Step 2: further ESET Android folders (real first lines at commit 59745502656e)
    @Test fun furtherEsetFoldersAreLoadedWithFamily() {
        val callphantom = "30b3126ef94c934fe09c6be6e2e5634ba6973e41bac18b32001a21875aad4d5f"
        val ghostchatUpper = "E4AA98C1BEEE901871FD8A78B37B21EF886E507E65FC6499E3DF3769081CD1CB"
        val bad = "52ff338f0f85c32678e7f614f0ce9ceeeea9e7d9cf4ddc3abb4446a2935949f2"
        assertEquals("CallPhantom", esetIocs.lookup(IocType.SHA256, callphantom)?.family)
        assertEquals("GhostChat", esetIocs.lookup(IocType.SHA256, ghostchatUpper)?.family)
        assertEquals("BadBazaar", esetIocs.lookup(IocType.SHA256, bad)?.family)
        val counts = eset.associate { it.id.removePrefix("eset-") to load(it).size }
        assertEquals(153, counts.values.sum())
        assertEquals(28, counts["callphantom"])
        assertEquals(20, counts["virtual-invaders"])
    }

    // Mixed files are used only where Android rows can be told apart: Meta labels every row
    @Test fun metaCsvKeepsOnlyAndroidSha256Rows() {
        val s = meta.single()
        assertTrue(s.bundled)
        assertEquals("2d664e985e55fb754de7b1175f713e11f73cf8c8", s.commit)
        assertEquals(s.sha256, IocStore.sha256(File(dir, s.fileName).readBytes()))
        val out = load(s)
        assertEquals(13, out.size)
        assertTrue(out.all { it.type == IocType.SHA256 && it.category == Category.MALWARE })
        assertTrue(esetIocs.lookup(IocType.SHA256, "aeb0c38219e714ab881d0065b9fc1915ba84ad5b86916a82814d056f1dfaf66d") != null)
        // the Windows sample, the md5 rows and the domains of the same file are not imported
        assertNull(esetIocs.lookup(IocType.SHA256, "05320c7348c156f0a98907d2b1527ff080eae36437d58735f2822d9f42f5d273"))
        assertEquals("Meta", AppAnalyzer.sourceName(out.first()))
        val notice = File("../NOTICE").readText()
        assertTrue("meta/threat-research" in notice && "Meta Platforms, Inc." in notice && "MIT License" in notice)
    }

    @Test fun metaCsvParserEdgeCases() {
        val h = aridspySample
        val text = "indicator_type,indicator_value,comment,ds\r\n" +
            "sha256,$h,PSS Android Malware Hash,2021-04-21\r\n" +
            "sha256,${h.uppercase()},android sample,2021-04-21\r\n" +
            "sha256,$h,PSS Windows Malware Hash,2021-04-21\r\n" +
            "sha256,${h.dropLast(1)},PSS Android Malware Hash,2021-04-21\r\n" +
            "md5,e567efd5c800c5b0c6eb5aa0bccc10e9,Arid Viper: Android Hash,2021-04-21\r\n" +
            "domain_name,example.org,PSS Android C2 Domain,2021-04-21\r\n"
        val out = Parsers.parse("typed-csv-android-sha256", text, ctx())
        assertEquals(2, out.size)
        assertTrue(out.all { it.value == h })
        assertTrue(Parsers.parse("typed-csv-android-sha256", "", ctx()).isEmpty())
    }

    // Known-harmless fingerprints (concept 2.4)
    @Test fun knownGoodHashesAreNeverFindings() {
        val empty = IocStore.sha256(ByteArray(0))
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", empty)
        assertTrue(KnownGood.contains(empty.uppercase()))
        val poisoned = IocSet(listOf(Indicator(empty, IocType.SHA256, Category.MALWARE, "X", "evil")))
        assertNull(poisoned.lookup(IocType.SHA256, empty))
        assertEquals(0, poisoned.count(IocType.SHA256))
        val r = AppAnalyzer.analyze(listOf(app(empty)), poisoned, "de.belkis.spyhound")
        assertTrue(r.findings.isEmpty())
        assertEquals(1, KnownGood.conflicts(listOf(Indicator(empty, IocType.SHA256, Category.MALWARE, "X", "evil"))).size)
    }

    @Test fun noShippedSourceHitsTheKnownGoodList() {
        for (s in sources.filter { it.format == "hashlist" || it.format == "typed-csv-android-sha256" }) {
            assertTrue(KnownGood.conflicts(load(s)).isEmpty(), s.id)
        }
        assertTrue(esetIocs.all(IocType.SHA256).none { KnownGood.contains(it.value) })
    }
}
