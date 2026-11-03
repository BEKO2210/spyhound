package de.belkis.spyhound

import de.belkis.spyhound.dns.DnsPacket
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DnsPacketTest {
    private val client4 = DnsPacket.ipBytes("192.0.2.10")
    private val resolver4 = DnsPacket.ipBytes("10.111.222.1")

    @Test
    fun parseIpv4UdpDnsQuery() {
        val dns = DnsPacket.buildDnsQuery("evil.example")
        val raw = DnsPacket.buildIpv4UdpQuery(client4, resolver4, 44000, 53, dns)
        val parsed = DnsPacket.parse(raw)
        assertNotNull(parsed)
        assertEquals(false, parsed.isIpv6)
        assertEquals(53, parsed.dstPort)
        assertEquals("evil.example", DnsPacket.questionNames(parsed.payload)?.single())
    }

    @Test
    fun parseIpv6UdpDnsQuery() {
        val client6 = DnsPacket.ipBytes("2001:db8::5")
        val resolver6 = DnsPacket.ipBytes("fd00:5ec::1")
        val dns = DnsPacket.buildDnsQuery("track.evil.example")
        val raw = DnsPacket.buildIpv6UdpQuery(client6, resolver6, 44001, 53, dns)
        val parsed = DnsPacket.parse(raw)
        assertNotNull(parsed)
        assertTrue(parsed.isIpv6)
        assertEquals(listOf("track.evil.example"), DnsPacket.questionNames(parsed.payload))
    }

    @Test
    fun compressionPointerInName() {
        val name = DnsPacket.encodeName("www.evil.example")
        val evilPtr = (12 + 1 + 3).toByte() // label length byte for "evil"
        val msg = ByteArray(12 + name.size + 4 + 6)
        writeU16(msg, 0, 0xabcd)
        writeU16(msg, 2, 0)
        writeU16(msg, 4, 2) // two questions
        writeU16(msg, 6, 0)
        writeU16(msg, 8, 0)
        writeU16(msg, 10, 0)
        var off = 12
        name.copyInto(msg, off)
        off += name.size
        writeU16(msg, off, 1); off += 2
        writeU16(msg, off, 1); off += 2
        msg[off++] = 0xc0.toByte()
        msg[off++] = evilPtr
        writeU16(msg, off, 1); off += 2
        writeU16(msg, off, 1)
        val names = DnsPacket.questionNames(msg.copyOf(off + 2))
        assertNotNull(names)
        assertEquals(listOf("www.evil.example", "evil.example"), names)
    }

    @Test
    fun brokenPacketsRejected() {
        assertNull(DnsPacket.parse(byteArrayOf(0x45)))
        val dns = DnsPacket.buildDnsQuery("x.example")
        val raw = DnsPacket.buildIpv4UdpQuery(client4, resolver4, 1, 53, dns)
        raw[9] = 6 // TCP
        assertNull(DnsPacket.parse(raw))
        val short = raw.copyOf(raw.size - 5)
        assertNull(DnsPacket.parse(short))
        val loop = ByteArray(14)
        loop[12] = 0xc0.toByte()
        loop[13] = 0x0c
        assertNull(DnsPacket.readName(loop, 12, 12))
    }

    @Test
    fun ipv4AndUdpChecksumsOnBuiltPackets() {
        val dns = DnsPacket.buildDnsQuery("checksum.example")
        val raw = DnsPacket.buildIpv4UdpQuery(client4, resolver4, 45000, 53, dns)
        assertEquals(0, DnsPacket.ipv4HeaderChecksum(raw, 0, 20))
        val udpOff = 20
        val udpLen = ((raw[udpOff + 4].toInt() and 0xff) shl 8) or (raw[udpOff + 5].toInt() and 0xff)
        val udpCsum = DnsPacket.udpChecksum(
            raw.copyOfRange(12, 16),
            raw.copyOfRange(16, 20),
            raw,
            udpOff,
            udpLen,
            ipv6 = false,
        )
        assertTrue(udpCsum != 0)
        assertEquals(
            udpCsum,
            ((raw[udpOff + 6].toInt() and 0xff) shl 8) or (raw[udpOff + 7].toInt() and 0xff),
        )
    }

    @Test
    fun buildReplySwapsEndpointsAndPreservesDns() {
        val dns = DnsPacket.buildDnsQuery("reply.example", id = 0x7777)
        val queryPkt = DnsPacket.buildIpv4UdpQuery(client4, resolver4, 40000, 53, dns)
        val req = DnsPacket.parse(queryPkt)!!
        val fakeAnswer = dns.copyOf().also {
            it[2] = 0x81.toByte() // QR=1, RA=1
            it[3] = 0x80.toByte()
        }
        val replyPkt = DnsPacket.buildReply(req, fakeAnswer)
        assertNotNull(replyPkt)
        val reply = DnsPacket.parse(replyPkt)!!
        assertEquals(40000, reply.dstPort)
        assertEquals(53, reply.srcPort)
        assertTrue(client4.contentEquals(reply.dstIp))
        assertTrue(resolver4.contentEquals(reply.srcIp))
        assertTrue(fakeAnswer.contentEquals(reply.payload))
    }

    @Test
    fun rejectsDotInLabelInvalidHostnameBytesAndOverlongNames() {
        fun rawQueryWithWireName(wireName: ByteArray): ByteArray {
            val out = ByteArray(12 + wireName.size + 4)
            writeU16(out, 0, 0x1234)
            writeU16(out, 2, 0x0100)
            writeU16(out, 4, 1)
            wireName.copyInto(out, 12)
            writeU16(out, 12 + wireName.size, 1)
            writeU16(out, 12 + wireName.size + 2, 1)
            return out
        }

        // Single label containing a dot ("evil.example" as one 12-byte label) must not become "evil.example".
        val singleLabelWithDot = byteArrayOf(12) + "evil.example".toByteArray(Charsets.US_ASCII) + byteArrayOf(0)
        val dotQuery = rawQueryWithWireName(singleLabelWithDot)
        assertNull(DnsPacket.questionNames(dotQuery))
        val spyware = de.belkis.spyhound.ioc.IocSet(
            listOf(de.belkis.spyhound.ioc.Indicator("evil.example", de.belkis.spyhound.ioc.IocType.DOMAIN, de.belkis.spyhound.ioc.Category.MERCENARY, "Spy", "t")),
        )
        val verdict = de.belkis.spyhound.dns.DnsFilter.evaluate(
            DnsPacket.questionNames(dotQuery).orEmpty(),
            spyware,
            de.belkis.spyhound.ioc.DomainLists.EMPTY,
            de.belkis.spyhound.dns.DnsFilterSettings(),
        )
        assertTrue(verdict.matches.isEmpty() && !verdict.block)

        // Labels with bytes outside valid hostname characters must be rejected.
        for (badLabel in listOf("evil/example", "evil example", "evil\u0000x", "-evil", "evil-", "evil\u0080")) {
            val bytes = badLabel.toByteArray(Charsets.ISO_8859_1)
            val wire = byteArrayOf(bytes.size.toByte()) + bytes + byteArrayOf(2, 'i'.code.toByte(), 'o'.code.toByte(), 0)
            assertNull(DnsPacket.questionNames(rawQueryWithWireName(wire)), "expected null for label $badLabel")
        }

        // 255-byte total wire name length (253 dotted chars) is the maximum valid DNS name; 256+ bytes must be rejected.
        val label63 = "a".repeat(63)
        val maxValid = "$label63.$label63.$label63.${"b".repeat(61)}" // 64*3 + 62 + 1 = 255 wire bytes
        assertEquals(listOf(maxValid), DnsPacket.questionNames(DnsPacket.buildDnsQuery(maxValid)))

        val tooLong256 = "$label63.$label63.$label63.${"b".repeat(62)}" // 64*3 + 63 + 1 = 256 wire bytes
        assertNull(DnsPacket.questionNames(DnsPacket.buildDnsQuery(tooLong256)))
    }

    private fun writeU16(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value shr 8).toByte()
        buf[offset + 1] = (value and 0xff).toByte()
    }
}
