package de.belkis.spyhound

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocStore
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.ioc.Parsers
import de.belkis.spyhound.ioc.SourceContext
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IocStoreDebugTest {
    private val ctx = SourceContext("debug-test", Category.MERCENARY, "Testspy")

    @Test
    fun debugTestTsvMapsPackageToAppIdAndSha256() {
        val sha = "a".repeat(64)
        val tsv = """
            de.belkis.spyhound.testspy	sample-app	primary	debug-test	Testspy
            $sha	sample-cert	primary	debug-test	Testspy
        """.trimIndent()
        val set = IocSet(Parsers.debugTestTsv(tsv, ctx))
        assertEquals("Testspy", set.lookup(IocType.APP_ID, "de.belkis.spyhound.testspy")?.family)
        assertTrue(set.lookup(IocType.SHA256, sha) != null)
    }

    @Test
    fun releaseBuildSkipsDebugTestFile() {
        val all = ArrayList<Indicator>()
        val storeDir = File.createTempFile("iocstore", "").apply { delete(); mkdirs() }
        File(storeDir, IocStore.DEBUG_TEST_FILE).writeText(
            "de.belkis.spyhound.testspy\tx\tx\tdebug-test\tTestspy\n",
        )
        IocStore.appendDebugTestIndicators(all, storeDir, debugBuild = false)
        assertTrue(all.isEmpty())
        IocStore.appendDebugTestIndicators(all, storeDir, debugBuild = true)
        assertTrue(all.isNotEmpty())
    }
}
