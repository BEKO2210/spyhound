package de.belkis.spyhound

import de.belkis.spyhound.shell.FORMAT_PLAIN
import de.belkis.spyhound.shell.FORMAT_WRAPPED
import de.belkis.spyhound.shell.StoredKey
import de.belkis.spyhound.shell.readStoredKey
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AdbKeyFormatTest {
    private val key = byteArrayOf(1, 2, 3)
    private val plain = byteArrayOf(FORMAT_PLAIN) + key

    @Test
    fun plainKeyIsRewrappedWhenKeystoreWorks() {
        val r = assertIs<StoredKey.Ready>(readStoredKey(plain, { byteArrayOf(FORMAT_WRAPPED, 9) + it }, { error("no") }))
        assertContentEquals(key, r.pkcs8)
        assertEquals(FORMAT_WRAPPED, r.rewrite!![0])
    }

    @Test
    fun plainKeyIsDiscardedWithoutKeystore() {
        assertIs<StoredKey.Discard>(readStoredKey(plain, { null }, { error("no") }))
    }

    @Test
    fun wrappedKeyIsReadAndNotRewritten() {
        val r = assertIs<StoredKey.Ready>(readStoredKey(byteArrayOf(FORMAT_WRAPPED, 0), { error("no") }, { key }))
        assertContentEquals(key, r.pkcs8)
        assertNull(r.rewrite)
    }
}
