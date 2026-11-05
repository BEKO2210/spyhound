package de.belkis.spyhound

import de.belkis.spyhound.ioc.IocStore
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class IocSourceSelectionTest {
    private val good = "good list".toByteArray()
    private val pin = IocStore.sha256(good)

    @Test
    fun corruptLocalFileFallsBackToBundledCopy() {
        assertContentEquals(good, IocStore.selectSource(pin, "garbage".toByteArray()) { good })
    }

    @Test
    fun validLocalFileWins() {
        assertContentEquals(good, IocStore.selectSource(pin, good) { error("not needed") })
    }

    @Test
    fun nothingMatchingStillReturnsACopySoItIsReportedAsAltered() {
        val bad = "garbage".toByteArray()
        assertContentEquals(bad, IocStore.selectSource(pin, bad) { "other".toByteArray() })
    }

    @Test
    fun noCopyAtAllIsMissing() {
        assertNull(IocStore.selectSource(pin, null, null))
    }
}
