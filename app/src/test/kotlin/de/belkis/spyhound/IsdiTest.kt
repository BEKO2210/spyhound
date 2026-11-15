package de.belkis.spyhound

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.ioc.Parsers
import de.belkis.spyhound.ioc.SourceContext
import de.belkis.spyhound.scan.AppAnalyzer
import de.belkis.spyhound.scan.AppRecord
import de.belkis.spyhound.scan.LeadKind
import de.belkis.spyhound.scan.TextsEn
import java.io.File
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Cornell ISDi app flags: stalkerware and spyware are findings, dual-use apps only ever a hint. */
class IsdiTest {
    private val ctx = SourceContext("isdi-app-flags", Category.STALKERWARE)
    private val csv = """
        appId,store,flag,title
        com.stalk.one,playstore,stalkerware,Stalk One
        com.spy.two,offstore,spyware,"Spy, Two"
        com.find.phone,playstore,dual-use,Find My Phone
        com.both.listed,playstore,dual-use,Both
        com.both.listed,offstore,stalkerware,Both
        com.fine.app,playstore,safe,Fine
        broken,row
    """.trimIndent()

    @Test fun parserKeepsOnlyStalkerwareAndSpyware() {
        val list = Parsers.parse("isdi-csv", csv, ctx)
        assertEquals(listOf("com.stalk.one", "com.spy.two", "com.both.listed"), list.map { it.value })
        assertTrue(list.all { it.type == IocType.APP_ID && it.category == Category.STALKERWARE })
        assertEquals("Spy, Two", list[1].family)
    }

    @Test fun dualUseListLeavesOutAppsThatAreAlsoStalkerware() {
        assertEquals(setOf("com.find.phone"), Parsers.isdiDualUse(csv))
    }

    @Test fun dualUseAppIsOneLeadAndNoFinding() {
        val apps = listOf(
            AppRecord("com.find.phone", installer = null, certSha1 = listOf("x"),
                grantedPermissions = setOf("android.permission.CAMERA"), accessibility = true, hiddenFromLauncher = true),
        )
        val r = AppAnalyzer.analyze(apps, IocSet(emptyList(), setOf("com.find.phone")), "self")
        assertTrue(r.findings.isEmpty())
        assertEquals(1, r.leads.size)
        assertEquals(LeadKind.DUAL_USE_APP, r.leads.single().kind)
        assertTrue(r.leads.single().detail.startsWith(AppAnalyzer.DUAL_USE_TEXT))
        assertTrue(r.leads.single().detail.contains("Kamera"))
    }

    @Test fun dualUseLeadIsTranslated() {
        val r = AppAnalyzer.analyze(listOf(AppRecord("com.find.phone", installer = "com.android.vending", certSha1 = listOf("x"))),
            IocSet(emptyList(), setOf("com.find.phone")), "self", TextsEn)
        assertEquals("Can be used to watch someone else. Check that you installed it yourself.", r.leads.single().detail)
    }

    @Test fun listedStalkerwareStaysAFindingEvenIfDualUse() {
        val ind = Indicator("com.stalk.one", IocType.APP_ID, Category.STALKERWARE, "Stalk One", "isdi-app-flags")
        val r = AppAnalyzer.analyze(listOf(AppRecord("com.stalk.one", installer = "com.android.vending")),
            IocSet(listOf(ind), setOf("com.stalk.one")), "self")
        assertEquals(1, r.findings.size)
        assertTrue(r.leads.none { it.kind == LeadKind.DUAL_USE_APP })
    }

    @Test fun bundledFileSplitsAsPublished() {
        val text = File("src/main/assets/iocs/isdi-app-flags.csv").readText()
        val indicators = Parsers.parse("isdi-csv", text, ctx).map { it.value }.toSet()
        val dual = Parsers.isdiDualUse(text)
        assertTrue(indicators.size in 500..700, "stalkerware+spyware rows: ${indicators.size}")
        assertTrue(dual.size > 4000, "dual-use rows: ${dual.size}")
        assertTrue(indicators.intersect(dual).isEmpty())
    }
}
