package de.belkis.spyhound

import de.belkis.spyhound.shell.MarkerInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MarkerInputStreamTest {
    private val m = "__SH_END_abc__"

    /** Hands out at most [chunk] bytes per read and then blocks-like EOF never comes: it returns -1 only at the end. */
    private fun source(text: String, chunk: Int): InputStream {
        val data = text.toByteArray()
        var i = 0
        return object : InputStream() {
            override fun read(): Int = if (i < data.size) data[i++].toInt() and 0xff else -1
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (i >= data.size) return -1
                val n = minOf(len, chunk, data.size - i)
                System.arraycopy(data, i, b, off, n)
                i += n
                return n
            }
        }
    }

    @Test fun cutsAtMarkerAndParsesExitCode() {
        val s = MarkerInputStream(source("a\nb\n${m}0\n", 1000), m)
        assertEquals("a\nb\n", s.readBytes().toString(Charsets.UTF_8))
        assertEquals(0, s.exitCode)
    }

    @Test fun markerSplitAcrossReads() {
        for (chunk in 1..9) {
            val s = MarkerInputStream(source("out${m}127\n", chunk), m)
            assertEquals("out", s.readBytes().toString(Charsets.UTF_8))
            assertEquals(127, s.exitCode)
        }
    }

    @Test fun noMarkerPassesEverythingAndNoExitCode() {
        val s = MarkerInputStream(ByteArrayInputStream("partial".toByteArray()), m)
        assertEquals("partial", s.readBytes().toString(Charsets.UTF_8))
        assertNull(s.exitCode)
    }

    @Test fun foreignMarkerLookalikeIsOutput() {
        val s = MarkerInputStream(source("__SH_END_zzz__0\n${m}1", 3), m)
        assertEquals("__SH_END_zzz__0\n", s.readBytes().toString(Charsets.UTF_8))
        assertEquals(1, s.exitCode)
    }
}
