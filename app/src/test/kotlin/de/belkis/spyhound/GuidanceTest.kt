package de.belkis.spyhound

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.scan.Finding
import de.belkis.spyhound.scan.Gap
import de.belkis.spyhound.scan.GapFix
import de.belkis.spyhound.scan.GapKind
import de.belkis.spyhound.scan.Guidance
import de.belkis.spyhound.scan.GuidanceAction
import de.belkis.spyhound.scan.Lead
import de.belkis.spyhound.scan.LeadKind
import de.belkis.spyhound.scan.guidanceFor
import java.io.File
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GuidanceTest {
    private fun finding(type: IocType, cat: Category) =
        Finding("Apps", "x", "y", Indicator("v", type, cat, "fam", "src"))

    private fun all(): List<Guidance> =
        IocType.entries.flatMap { t -> Category.entries.map { guidanceFor(finding(t, it)) } } +
            LeadKind.entries.map { guidanceFor(Lead("c", "w", "d", "why", it)) } +
            GapFix.entries.flatMap { f -> GapKind.entries.map { guidanceFor(Gap("c", "d", f, it)) } }

    private fun keys(path: String): Set<String> {
        val xml = File(path).readText()
        return Regex("""<string name="(guide_[a-z_]+)"[^>]*>([^<]+)</string>""").findAll(xml).onEach {
            assertTrue(it.groupValues[2].isNotBlank(), "${it.groupValues[1]} is empty in $path")
        }.map { it.groupValues[1] }.toSet()
    }

    @Test fun everyKeyExistsInBothLanguages() {
        val en = keys("src/main/res/values/strings.xml")
        val de = keys("src/main/res/values-de/strings.xml")
        val needed = all().flatMap { listOf(it.meaningKey, it.stepsKey) }.toSet()
        assertEquals(emptySet(), needed - en, "missing in EN")
        assertEquals(emptySet(), needed - de, "missing in DE")
        assertEquals(emptySet(), en - needed, "unused EN keys")
        assertEquals(en, de)
    }

    @Test fun everyFindingGetsExplanationAndHelpAction() {
        for (t in IocType.entries) for (c in Category.entries) {
            val g = guidanceFor(finding(t, c))
            assertEquals(GuidanceAction.GET_HELP, g.action)
            assertTrue(g.meaningKey.startsWith("guide_finding_"))
        }
    }

    @Test fun mercenaryStepsMentionEvidenceAndAccessNow() {
        val de = File("src/main/res/values-de/strings.xml").readText()
        val en = File("src/main/res/values/strings.xml").readText()
        for (t in IocType.entries) {
            val key = guidanceFor(finding(t, Category.MERCENARY)).stepsKey
            for (xml in listOf(de, en)) {
                val text = Regex("""name="$key">([^<]+)<""").find(xml)!!.groupValues[1]
                assertTrue("accessnow.org" in text, key)
            }
        }
    }

    @Test fun stalkerwareStepsWarnAndNameHelpline() {
        val de = File("src/main/res/values-de/strings.xml").readText()
        val seen = IocType.entries.map { guidanceFor(finding(it, Category.STALKERWARE)).stepsKey }.toSet()
        for (key in seen) {
            val text = Regex("""name="$key">([^<]+)<""").find(de)!!.groupValues[1]
            assertTrue("116 016" in text, key)
        }
    }

    @Test fun gapFixesMapToTheirAction() {
        val expected = mapOf(
            GapFix.PERMISSION_SMS to GuidanceAction.PERMISSION_SMS,
            GapFix.PERMISSION_FILES to GuidanceAction.PERMISSION_FILES,
            GapFix.SHIZUKU to GuidanceAction.SETUP_DEEP_CHECK,
            GapFix.ROOT to GuidanceAction.SETUP_DEEP_CHECK,
            GapFix.RESTART_BUILTIN_ADB to GuidanceAction.SETUP_DEEP_CHECK,
            GapFix.UPDATE_INDICATORS to GuidanceAction.UPDATE_INDICATORS,
        )
        for ((fix, action) in expected) assertEquals(action, guidanceFor(Gap("c", "d", fix)).action)
    }

    @Test fun leadActions() {
        assertEquals(GuidanceAction.APP_INFO, guidanceFor(Lead("c", "w", "d", "y", LeadKind.SIDELOADED_APP)).action)
        assertEquals(GuidanceAction.SETTINGS_DEVELOPER, guidanceFor(Lead("c", "w", "d", "y", LeadKind.DEBUGGING)).action)
        assertEquals(GuidanceAction.SETTINGS_UNKNOWN_SOURCES, guidanceFor(Lead("c", "w", "d", "y", LeadKind.UNKNOWN_SOURCES)).action)
    }

    @Test fun analyzersSetKinds() {
        val r = de.belkis.spyhound.scan.DeviceAnalyzer.analyze(
            de.belkis.spyhound.scan.DeviceState(
                props = mapOf("ro.debuggable" to "1"), securityPatch = "", sdk = 34, adbEnabled = true,
                adbWifiEnabled = null, developerOptions = true, installSources = emptyList(),
                rootTraces = emptyList(), selinux = null,
            ),
            de.belkis.spyhound.ioc.IocSet(emptyList()),
        )
        val kinds = r.leads.map { it.kind }.toSet()
        assertTrue(LeadKind.DEBUGGING in kinds && LeadKind.SYSTEM_BUILD in kinds, kinds.toString())
    }
}
