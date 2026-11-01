package de.belkis.spyhound

import de.belkis.spyhound.ui.info.SourceLicenseKind
import de.belkis.spyhound.ui.info.infoSourceGroups
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InfoSourceLinesTest {
    @Test
    fun groupsReposWithLicenses() {
        val json = javaClass.classLoader.getResourceAsStream("fixtures/sources-mini.json")!!.bufferedReader().readText()
        val sources = de.belkis.spyhound.ioc.FeedCatalog.parse(json)
        val groups = infoSourceGroups(sources)
        assertEquals(2, groups.size)
        assertEquals(SourceLicenseKind.MIT, groups.first { it.repo.contains("mvt") }.license)
        assertEquals(SourceLicenseKind.DEVICE_ONLY, groups.first { it.repo.contains("Amnesty") }.license)
        assertTrue(groups.first { it.repo.contains("mvt") }.bundled)
        assertTrue(!groups.first { it.repo.contains("Amnesty") }.bundled)
    }
}
