package de.belkis.spyhound

import de.belkis.spyhound.ioc.IocStore
import org.junit.Test
import java.io.IOException
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DownloadMirrorTest {
    @Test fun rawHostIsTriedFirstAndMirrorIsNotNeeded() {
        val asked = ArrayList<String>()
        val bytes = IocStore.withMirror("raw", "api") { asked += it; byteArrayOf(1) }
        assertContentEquals(byteArrayOf(1), bytes)
        assertEquals(listOf("raw"), asked)
    }

    @Test fun blockedRawHostFallsBackToMirror() {
        val asked = ArrayList<String>()
        val bytes = IocStore.withMirror("raw", "api") {
            asked += it
            if (it == "raw") throw IOException("Unable to resolve host") else byteArrayOf(2)
        }
        assertContentEquals(byteArrayOf(2), bytes)
        assertEquals(listOf("raw", "api"), asked)
    }

    @Test fun bothFailingKeepsBothReasons() {
        val e = assertFailsWith<IOException> {
            IocStore.withMirror("raw", "api") { throw IOException(if (it == "raw") "timeout" else "HTTP 403") }
        }
        assertTrue(e.message!!.contains("timeout") && e.message!!.contains("HTTP 403"), e.message)
    }

    @Test fun refusedSourceIsNotRetried() {
        // Allow-list violations are not network errors: the mirror must not be asked.
        val asked = ArrayList<String>()
        assertFailsWith<IllegalArgumentException> {
            IocStore.withMirror("raw", "api") { asked += it; throw IllegalArgumentException("nicht erlaubte Quelle") }
        }
        assertEquals(listOf("raw"), asked)
    }
}
