package de.belkis.spyhound

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.FeedSource
import de.belkis.spyhound.ioc.IocStore
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class IocSourceStatusTest {
    private val localBytes = "old downloaded list".toByteArray()

    @Test
    fun nonBundledSourceWithWrongPinNeedsReloadAndIsNotLoaded() {
        val source = source(bundled = false)

        val loaded = IocStore.loadCatalogSources(listOf(source)) { localBytes }

        assertEquals(listOf(source), loaded.outdated)
        assertTrue(loaded.corrupt.isEmpty())
        assertTrue(loaded.indicators.isEmpty())
        assertTrue(loaded.active.isEmpty())
    }

    @Test
    fun bundledSourceWithWrongPinIsCorruptAndIsNotLoaded() {
        val source = source(bundled = true)

        val loaded = IocStore.loadCatalogSources(listOf(source)) { localBytes }

        assertEquals(listOf(source), loaded.corrupt)
        assertTrue(loaded.outdated.isEmpty())
        assertTrue(loaded.indicators.isEmpty())
        assertTrue(loaded.active.isEmpty())
    }

    private fun source(bundled: Boolean) = FeedSource(
        id = "test-source",
        name = "Test source",
        repo = "example/repo",
        commit = "a".repeat(40),
        path = "source.txt",
        format = "domains-txt",
        cite = "",
        category = Category.MERCENARY,
        family = "Test",
        sha256 = IocStore.sha256("new pinned list".toByteArray()),
        bundled = bundled,
        androidArtifacts = true,
    )
}
