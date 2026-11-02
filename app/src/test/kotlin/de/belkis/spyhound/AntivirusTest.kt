package de.belkis.spyhound

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.FeedCatalog
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
    private val esetIocs = IocSet(eset.flatMap { load(it) })

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
        assertEquals(setOf("aridspy", "vajraspy", "spyloan", "ngate", "promptspy"), eset.map { it.id.removePrefix("eset-") }.toSet())
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
        assertEquals(79, esetIocs.count(IocType.SHA256))
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
        val others = sources.filter { it.repo != "eset/malware-ioc" && it.bundled }.flatMap { load(it) }
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
        val report = Report(0, 1, AccessLevel.APP, 79, 5, 0, listOf(r), "dev")
        assertEquals(Verdict.MALWARE, report.verdict)
    }

    @Test fun installedAppWithoutHashMatchIsNoFinding() {
        val r = AppAnalyzer.analyze(listOf(app("f".repeat(64))), esetIocs, "de.belkis.spyhound")
        assertTrue(r.findings.isEmpty())
        assertEquals(Verdict.NOTHING_FOUND, Report(0, 1, AccessLevel.APP, 79, 5, 0, listOf(r), "dev").verdict)
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
        assertTrue("79" in gap.detail && "ausgewählten Kampagnen" in gap.detail && "Play Protect" in gap.detail, gap.detail)
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
}
