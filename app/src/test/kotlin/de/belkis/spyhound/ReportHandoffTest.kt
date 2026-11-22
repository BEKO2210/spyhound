package de.belkis.spyhound

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.CheckResult
import de.belkis.spyhound.scan.GapFix
import de.belkis.spyhound.scan.GapKind
import de.belkis.spyhound.scan.Report
import de.belkis.spyhound.scan.ReportHandoff
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportHandoffTest {
    private val pkg = "org.telegram.FlyGram"
    private val path = "/sdcard/Download/secret-folder/"

    private fun report(): Report {
        val apps = CheckResult("Apps").apply {
            ran = true; examined = 212
            val ind = Indicator(pkg, IocType.APP_ID, Category.MALWARE, "BadBazaar", "eset-gref")
            hit("FlyGram ($pkg)", "Paketname passt", ind)
            hit("FlyGram ($pkg)", "Zertifikat passt", ind.copy(type = IocType.APP_CERT, value = "ab:cd"))
            hit("Monitor", "App-ID passt", Indicator("com.example.spy", IocType.APP_ID, Category.STALKERWARE, " ", "echap"))
            lead("Notizen (com.example.notes)", "Bedienungshilfe aktiv", "Kann Bildschirm lesen")
            gap("Ordner $path nicht lesbar", GapFix.PERMISSION_FILES)
        }
        val deep = CheckResult("Tiefenprüfung").apply { gap("Prozesse nicht geprüft", GapFix.SHIZUKU) }
        val files = CheckResult("Dateien").apply { gap("$path zu groß", kind = GapKind.LARGE_FILES) }
        return Report(
            startedAt = 1_767_268_800_123, finishedAt = 1_767_268_830_000, access = AccessLevel.APP,
            indicatorCount = 100, indicatorSources = 3, indicatorSourcesMissing = 0,
            checks = listOf(apps, deep, files), device = "Google Pixel 11 Pro XL",
        )
    }

    @Test
    fun `fields follow the proposed format`() {
        val o = Json.parseToJsonElement(ReportHandoff.render(report(), "0.3.17")).jsonObject
        assertEquals("spyhound-local-handoff", o["format"]!!.jsonPrimitive.content)
        assertEquals(1, o["format_version"]!!.jsonPrimitive.int)
        assertEquals("0.3.17", o["spyhound_version"]!!.jsonPrimitive.content)
        assertEquals("2026-01-01T12:00:00Z", o["checked_at_utc"]!!.jsonPrimitive.content)
        assertEquals("basic", o["check_depth"]!!.jsonPrimitive.content)
        val counts = o["counts"]!!.jsonObject
        assertEquals(3, counts["findings"]!!.jsonPrimitive.int)
        assertEquals(1, counts["leads"]!!.jsonPrimitive.int)
        assertEquals(3, counts["gaps"]!!.jsonPrimitive.int)
        assertEquals(ReportHandoff.NOTICE, o["notice"]!!.jsonPrimitive.content)
    }

    @Test
    fun `findings carry only the published name and source, once per label`() {
        val f = Json.parseToJsonElement(ReportHandoff.render(report(), "x")).jsonObject["findings"]!!.jsonArray
        assertEquals(2, f.size)
        assertEquals("BadBazaar", f[0].jsonObject["indicator_name"]!!.jsonPrimitive.content)
        assertEquals("eset-gref", f[0].jsonObject["indicator_source"]!!.jsonPrimitive.content)
        assertEquals("Monitoring app (unnamed in source)", f[1].jsonObject["indicator_name"]!!.jsonPrimitive.content)
        assertEquals(setOf("indicator_name", "indicator_source"), f[0].jsonObject.keys)
    }

    @Test
    fun `gaps are stable codes, every check keeps its own`() {
        val g = Json.parseToJsonElement(ReportHandoff.render(report(), "x")).jsonObject["gaps"]!!.jsonArray
            .map { it.jsonPrimitive.content }
        assertEquals(listOf("apps.needs_permission_files", "deep_check.needs_shizuku", "files.large_files"), g)
    }

    @Test
    fun `outdated sources use the stable update code`() {
        val indicators = CheckResult("Indikatoren").apply {
            gap("1 Indikator-Quelle ist veraltet und wurde nicht benutzt (Quelle).", GapFix.UPDATE_INDICATORS)
        }
        val report = Report(0, 0, AccessLevel.APP, 0, 0, 1, listOf(indicators), "d")

        val gaps = Json.parseToJsonElement(ReportHandoff.render(report, "x"))
            .jsonObject["gaps"]!!.jsonArray.map { it.jsonPrimitive.content }

        assertEquals(listOf("indicators.needs_update_indicators"), gaps)
    }

    @Test
    fun `no matched value, app, path, lead text or device leaves the phone`() {
        val text = ReportHandoff.render(report(), "x")
        for (secret in listOf(pkg, "ab:cd", "com.example.spy", "com.example.notes", "secret-folder",
            "Pixel", "FlyGram", "Bedienungshilfe", "Paketname")) {
            assertFalse("leaked: $secret", text.contains(secret))
        }
    }

    @Test
    fun `an empty report still names its limits`() {
        val empty = Report(0, 0, AccessLevel.ROOT, 0, 0, 0, emptyList(), "d")
        val o = Json.parseToJsonElement(ReportHandoff.render(empty, "x")).jsonObject
        assertEquals("root", o["check_depth"]!!.jsonPrimitive.content)
        assertTrue(o["findings"]!!.jsonArray.isEmpty())
        assertTrue(o["notice"]!!.jsonPrimitive.content.contains("No findings never means a clean device"))
    }
}
