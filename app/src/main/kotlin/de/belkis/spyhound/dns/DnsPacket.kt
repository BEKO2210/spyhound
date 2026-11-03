package de.belkis.spyhound.dns

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Parses and builds IPv4/IPv6 UDP packets (typical DNS over VPN TUN). Pure JVM — no Android APIs.
 */
object DnsPacket {
    const val IP_PROTO_UDP = 17
    private const val DNS_PORT = 53

    data class Parsed(
        val isIpv6: Boolean,
        val srcIp: ByteArray,
        val dstIp: ByteArray,
        val srcPort: Int,
        val dstPort: Int,
        val payload: ByteArray,
    ) {
        val isDnsQuery: Boolean get() = dstPort == DNS_PORT && payload.isNotEmpty()
    }

    /** Parses one IP datagram; returns null if truncated, not UDP, or malformed. */
    fun parse(packet: ByteArray, length: Int = packet.size): Parsed? {
        if (length < 20) return null
        val version = (packet[0].toInt() ushr 4) and 0xf
        return when (version) {
            4 -> parseIpv4(packet, length)
            6 -> parseIpv6(packet, length)
            else -> null
        }
    }

    /** Builds a reply datagram by swapping L3/L4 endpoints and replacing the UDP payload. */
    fun buildReply(request: Parsed, responsePayload: ByteArray): ByteArray? {
        return if (request.isIpv6) buildIpv6Reply(request, responsePayload)
        else buildIpv4Reply(request, responsePayload)
    }

    // --- DNS names inside a UDP payload ---

    /**
     * An NXDOMAIN answer to [query]: same id and question, no records. Null if the question cannot be read.
     * Used to block a name locally; the query never leaves the device.
     */
    fun buildNxdomain(query: ByteArray): ByteArray? {
        if (query.size < 12) return null
        val qd = readU16(query, 4) ?: return null
        var end = 12
        repeat(qd) {
            val name = readName(query, 12, end) ?: return null
            end = name.second + 4
            if (end > query.size) return null
        }
        val reply = query.copyOf(end)
        val flags = readU16(query, 2) ?: return null
        // QR=1, keep opcode and RD, RA=1, RCODE=3 (name does not exist).
        writeU16(reply, 2, 0x8000 or (flags and 0x7900) or 0x0080 or 0x0003)
        writeU16(reply, 6, 0)
        writeU16(reply, 8, 0)
        writeU16(reply, 10, 0)
        return reply
    }

    /** Reads QNAMEs from the question section; null if the message is invalid. */
    fun questionNames(dnsPayload: ByteArray): List<String>? {
        if (dnsPayload.size < 12) return null
        val qd = readU16(dnsPayload, 4) ?: return null
        if (qd == 0) return emptyList()
        var off = 12
        val names = ArrayList<String>(qd)
        repeat(qd) {
            val parsed = readName(dnsPayload, 12, off) ?: return null
            names += parsed.first
            off = parsed.second + 4 // QTYPE + QCLASS
            if (off > dnsPayload.size) return null
        }
        return names
    }

    /**
     * Reads a DNS domain name at [offset]. [nameOrigin] is the start of the DNS message (for compression pointers).
     * Returns the name and the offset after the name (or after the compression pointer if only pointers were used).
     */
    fun readName(message: ByteArray, nameOrigin: Int, offset: Int): Pair<String, Int>? {
        if (offset < 0 || offset >= message.size || nameOrigin < 0 || nameOrigin > message.size) return null
        val labels = ArrayList<String>(4)
        var off = offset
        var endOffset = offset
        var jumped = false
        val visited = HashSet<Int>()
        var steps = 0
        var wireBytes = 0
        var terminated = false
        while (off < message.size) {
            if (++steps > message.size + 8) return null
            val len = message[off].toInt() and 0xff
            when {
                len == 0 -> {
                    off++
                    if (++wireBytes > 255) return null
                    if (!jumped) endOffset = off
                    terminated = true
                    break
                }
                len and 0xc0 == 0xc0 -> {
                    if (off + 1 >= message.size) return null
                    val ptr = ((len and 0x3f) shl 8) or (message[off + 1].toInt() and 0xff)
                    if (ptr < nameOrigin || ptr >= message.size) return null
                    if (ptr in visited) return null
                    visited += ptr
                    if (!jumped) {
                        endOffset = off + 2
                        jumped = true
                    }
                    off = ptr
                }
                len and 0xc0 != 0 -> return null
                else -> {
                    off++
                    if (off + len > message.size) return null
                    wireBytes += 1 + len
                    if (wireBytes > 255 || !isValidLabel(message, off, len)) return null
                    labels += String(message, off, len, Charsets.US_ASCII)
                    off += len
                }
            }
        }
        if (!terminated || labels.isEmpty()) return null // a name cut off before its end is no name
        return labels.joinToString(".") to endOffset
    }

    private fun isValidLabel(message: ByteArray, offset: Int, len: Int): Boolean {
        if (len !in 1..63) return false
        if (message[offset] == '-'.code.toByte() || message[offset + len - 1] == '-'.code.toByte()) return false
        for (i in offset until offset + len) {
            val b = message[i].toInt() and 0xff
            val ok = b in 'a'.code..'z'.code || b in 'A'.code..'Z'.code || b in '0'.code..'9'.code || b == '-'.code
            if (!ok) return false
        }
        return true
    }

    // --- builders for tests ---

    fun buildIpv4UdpQuery(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray,
    ): ByteArray {
        require(srcIp.size == 4 && dstIp.size == 4)
        val udpLen = 8 + payload.size
        val totalLen = 20 + udpLen
        val out = ByteArray(totalLen)
        out[0] = 0x45
        out[1] = 0
        writeU16(out, 2, totalLen)
        out[8] = 64
        out[9] = IP_PROTO_UDP.toByte()
        srcIp.copyInto(out, 12)
        dstIp.copyInto(out, 16)
        writeIpv4HeaderChecksum(out, 0, 20)
        writeUdp(out, 20, srcPort, dstPort, payload, srcIp, dstIp, false)
        return out
    }

    fun buildIpv6UdpQuery(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray,
    ): ByteArray {
        require(srcIp.size == 16 && dstIp.size == 16)
        val udpLen = 8 + payload.size
        val totalLen = 40 + udpLen
        val out = ByteArray(totalLen)
        out[0] = 0x60
        writeU16(out, 4, udpLen)
        out[6] = IP_PROTO_UDP.toByte()
        out[7] = 64
        srcIp.copyInto(out, 8)
        dstIp.copyInto(out, 24)
        writeUdp(out, 40, srcPort, dstPort, payload, srcIp, dstIp, true)
        return out
    }

    /** Minimal DNS query bytes for [qname] (one question, IN A). */
    fun buildDnsQuery(qname: String, id: Int = 0x1234): ByteArray {
        val nameBytes = encodeName(qname)
        val out = ByteArray(12 + nameBytes.size + 4)
        writeU16(out, 0, id)
        writeU16(out, 2, 0x0100) // RD=1
        writeU16(out, 4, 1) // QDCOUNT
        nameBytes.copyInto(out, 12)
        val tail = 12 + nameBytes.size
        writeU16(out, tail, 1) // A
        writeU16(out, tail + 2, 1) // IN
        return out
    }

    fun encodeName(qname: String): ByteArray {
        val labels = qname.trim().trimEnd('.').split('.').filter { it.isNotEmpty() }
        val parts = ArrayList<Byte>()
        for (label in labels) {
            require(label.length in 1..63) { "label length" }
            parts.add(label.length.toByte())
            label.toByteArray(Charsets.US_ASCII).forEach { parts.add(it) }
        }
        parts.add(0)
        return parts.toByteArray()
    }

    fun ipv4HeaderChecksum(header: ByteArray, offset: Int = 0, length: Int = 20): Int {
        return onesComplementSum(header, offset, length)
    }

    fun udpChecksum(
        ipSrc: ByteArray,
        ipDst: ByteArray,
        udp: ByteArray,
        udpOffset: Int,
        udpLength: Int,
        ipv6: Boolean,
    ): Int {
        var sum = 0
        if (ipv6) {
            sum = sum16(ipSrc, 0, 16, sum)
            sum = sum16(ipDst, 0, 16, sum)
            sum = sum + udpLength
            sum = sum + IP_PROTO_UDP
        } else {
            sum = sum16(ipSrc, 0, 4, sum)
            sum = sum16(ipDst, 0, 4, sum)
            sum += IP_PROTO_UDP // high byte zero, low byte protocol (one 16-bit word)
            sum += udpLength
        }
        val saved = ((udp[udpOffset + 6].toInt() and 0xff) shl 8) or (udp[udpOffset + 7].toInt() and 0xff)
        udp[udpOffset + 6] = 0
        udp[udpOffset + 7] = 0
        sum = sum16(udp, udpOffset, udpLength, sum)
        udp[udpOffset + 6] = (saved shr 8).toByte()
        udp[udpOffset + 7] = (saved and 0xff).toByte()
        var c = foldSum(sum)
        if (c == 0) c = 0xffff
        return c
    }

    // --- internals ---

    private fun parseIpv4(packet: ByteArray, length: Int): Parsed? {
        val ihl = (packet[0].toInt() and 0x0f) * 4
        if (ihl < 20 || length < ihl + 8) return null
        if ((packet[0].toInt() ushr 4) != 4) return null
        if (packet[9].toInt() != IP_PROTO_UDP) return null
        val total = readU16(packet, 2) ?: return null
        if (total > length || total < ihl + 8) return null
        val udpOff = ihl
        val srcPort = readU16(packet, udpOff) ?: return null
        val dstPort = readU16(packet, udpOff + 2) ?: return null
        val udpLen = readU16(packet, udpOff + 4) ?: return null
        if (udpLen < 8 || udpOff + udpLen > length) return null
        val payloadLen = udpLen - 8
        val payload = packet.copyOfRange(udpOff + 8, udpOff + 8 + payloadLen)
        return Parsed(
            isIpv6 = false,
            srcIp = packet.copyOfRange(12, 16),
            dstIp = packet.copyOfRange(16, 20),
            srcPort = srcPort,
            dstPort = dstPort,
            payload = payload,
        )
    }

    private fun parseIpv6(packet: ByteArray, length: Int): Parsed? {
        if (length < 40 + 8) return null
        if ((packet[0].toInt() ushr 4) != 6) return null
        if (packet[6].toInt() != IP_PROTO_UDP) return null
        val payloadLen = readU16(packet, 4) ?: return null
        if (40 + payloadLen > length) return null
        val udpOff = 40
        val srcPort = readU16(packet, udpOff) ?: return null
        val dstPort = readU16(packet, udpOff + 2) ?: return null
        val udpLen = readU16(packet, udpOff + 4) ?: return null
        if (udpLen < 8 || udpOff + udpLen > length) return null
        val payload = packet.copyOfRange(udpOff + 8, udpOff + udpLen)
        return Parsed(
            isIpv6 = true,
            srcIp = packet.copyOfRange(8, 24),
            dstIp = packet.copyOfRange(24, 40),
            srcPort = srcPort,
            dstPort = dstPort,
            payload = payload,
        )
    }

    private fun buildIpv4Reply(req: Parsed, payload: ByteArray): ByteArray? {
        if (req.srcIp.size != 4 || req.dstIp.size != 4) return null
        val udpLen = 8 + payload.size
        val totalLen = 20 + udpLen
        val out = ByteArray(totalLen)
        out[0] = 0x45
        writeU16(out, 2, totalLen)
        out[8] = 64
        out[9] = IP_PROTO_UDP.toByte()
        req.dstIp.copyInto(out, 12)
        req.srcIp.copyInto(out, 16)
        writeIpv4HeaderChecksum(out, 0, 20)
        writeUdp(out, 20, req.dstPort, req.srcPort, payload, req.dstIp, req.srcIp, false)
        return out
    }

    private fun buildIpv6Reply(req: Parsed, payload: ByteArray): ByteArray? {
        if (req.srcIp.size != 16 || req.dstIp.size != 16) return null
        val udpLen = 8 + payload.size
        val out = ByteArray(40 + udpLen)
        out[0] = 0x60
        writeU16(out, 4, udpLen)
        out[6] = IP_PROTO_UDP.toByte()
        out[7] = 64
        req.dstIp.copyInto(out, 8)
        req.srcIp.copyInto(out, 24)
        writeUdp(out, 40, req.dstPort, req.srcPort, payload, req.dstIp, req.srcIp, true)
        return out
    }

    private fun writeUdp(
        out: ByteArray,
        udpOff: Int,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray,
        ipSrc: ByteArray,
        ipDst: ByteArray,
        ipv6: Boolean,
    ) {
        val udpLen = 8 + payload.size
        writeU16(out, udpOff, srcPort)
        writeU16(out, udpOff + 2, dstPort)
        writeU16(out, udpOff + 4, udpLen)
        out[udpOff + 6] = 0
        out[udpOff + 7] = 0
        payload.copyInto(out, udpOff + 8)
        val csum = udpChecksum(ipSrc, ipDst, out, udpOff, udpLen, ipv6)
        writeU16(out, udpOff + 6, csum)
    }

    private fun writeIpv4HeaderChecksum(header: ByteArray, offset: Int, length: Int) {
        header[offset + 10] = 0
        header[offset + 11] = 0
        val c = onesComplementSum(header, offset, length)
        writeU16(header, offset + 10, c)
    }

    private fun onesComplementSum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        val end = offset + length
        while (i + 1 < end) {
            sum += ((data[i].toInt() and 0xff) shl 8) or (data[i + 1].toInt() and 0xff)
            i += 2
        }
        if (i < end) sum += (data[i].toInt() and 0xff) shl 8
        return foldSum(sum)
    }

    private fun sum16(data: ByteArray, offset: Int, length: Int, initial: Int): Int {
        var sum = initial
        var i = offset
        val end = offset + length
        while (i + 1 < end) {
            sum += ((data[i].toInt() and 0xff) shl 8) or (data[i + 1].toInt() and 0xff)
            i += 2
        }
        if (i < end) sum += (data[i].toInt() and 0xff) shl 8
        return sum
    }

    private fun foldSum(sum: Int): Int {
        var s = sum
        while (s ushr 16 != 0) s = (s and 0xffff) + (s ushr 16)
        return s.inv() and 0xffff
    }

    private fun readU16(data: ByteArray, offset: Int): Int? {
        if (offset + 1 >= data.size) return null
        return ((data[offset].toInt() and 0xff) shl 8) or (data[offset + 1].toInt() and 0xff)
    }

    private fun writeU16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value shr 8).toByte()
        data[offset + 1] = (value and 0xff).toByte()
    }

    fun ipBytes(host: String): ByteArray = InetAddress.getByName(host).address

    fun isIpv4(bytes: ByteArray): Boolean = bytes.size == 4
    fun isIpv6(bytes: ByteArray): Boolean = bytes.size == 16

    fun addressString(bytes: ByteArray): String = when (bytes.size) {
        4 -> Inet4Address.getByAddress(bytes).hostAddress
        16 -> Inet6Address.getByAddress(bytes).hostAddress
        else -> ""
    } ?: ""
}
