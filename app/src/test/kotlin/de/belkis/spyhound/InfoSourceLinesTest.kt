package de.belkis.spyhound

import de.belkis.spyhound.ioc.FeedCatalog
import de.belkis.spyhound.ui.info.MALWAREBAZAAR_REPO
import de.belkis.spyhound.ui.info.SourceLicenseKind
import de.belkis.spyhound.ui.info.infoSourceGroups
import de.belkis.spyhound.ui.info.sourceGroupTitleRes
import de.belkis.spyhound.R
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InfoSourceLinesTest {
    @Test
    fun groupsReposWithLicenses() {
        val json = javaClass.classLoader.getResourceAsStream("fixtures/sources-mini.json")!!.bufferedReader().readText()
        val sources = FeedCatalog.parse(json)
        val groups = infoSourceGroups(sources)
        assertEquals(2, groups.size)
        assertEquals(SourceLicenseKind.MIT, groups.first { it.repo.contains("mvt") }.license)
        assertEquals(SourceLicenseKind.DEVICE_ONLY, groups.first { it.repo.contains("Amnesty") }.license)
        assertTrue(groups.first { it.repo.contains("mvt") }.bundled)
        assertTrue(!groups.first { it.repo.contains("Amnesty") }.bundled)
    }

    @Test
    fun noBundledSourceIsShownAsDeviceOnly() {
        // "Loaded on the phone only" means there is no licence to ship the file; a bundled list must never say that.
        val json = java.io.File("src/main/assets/iocs/sources.json").readText()
        val wrong = infoSourceGroups(FeedCatalog.parse(json))
            .filter { it.bundled && it.license == SourceLicenseKind.DEVICE_ONLY }
            .map { it.repo }
        assertEquals(emptyList(), wrong)
    }

    @Test
    fun malwareBazaarGroupWhenSourceEnabled() {
        val commit = "a".repeat(40)
        val sha = "b".repeat(64)
        val json = """
            [{"id":"malwarebazaar-android","name":"abuse.ch MalwareBazaar Android","repo":"$MALWAREBAZAAR_REPO",
              "commit":"$commit","path":"data/malwarebazaar/android.tsv","format":"malwarebazaar-tsv",
              "category":"malware","sha256":"$sha","cite":"abuse.ch MalwareBazaar, in collaboration with Spamhaus Technology"}]
        """
        val groups = infoSourceGroups(FeedCatalog.parse(json))
        assertEquals(1, groups.size)
        val group = groups.single()
        assertEquals(MALWAREBAZAAR_REPO, group.repo)
        assertEquals(SourceLicenseKind.ABUSECH_TERMS, group.license)
        assertEquals(R.string.info_repo_malwarebazaar, sourceGroupTitleRes(group.repo))
        assertTrue(!group.bundled)
    }

    @Test
    fun malwareBazaarGroupAbsentWhenSourceDisabled() {
        val json = """
            [{"id":"malwarebazaar-android","name":"abuse.ch MalwareBazaar Android","repo":"$MALWAREBAZAAR_REPO",
              "commit":"","path":"data/malwarebazaar/android.tsv","format":"malwarebazaar-tsv",
              "category":"malware","sha256":"","enabled":false}]
        """
        val groups = infoSourceGroups(FeedCatalog.parse(json))
        assertTrue(groups.none { it.repo == MALWAREBAZAAR_REPO })
    }
}
