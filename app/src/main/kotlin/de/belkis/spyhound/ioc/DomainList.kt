package de.belkis.spyhound.ioc

/**
 * A large, read-only set of domains for tracker and phishing lists.
 *
 * Every entry is stored reversed by label ("www.example.com" becomes "com.example.www") in one sorted byte array,
 * so a host and all of its parent domains are prefixes of the reversed host and a lookup is a handful of binary
 * searches without any allocation per entry. 100 000 entries cost about 2 MB instead of tens of MB as a hash map.
 */
class DomainList private constructor(
    private val data: ByteArray,
    private val starts: IntArray, // starts[i] until starts[i + 1] is entry i; one extra sentinel at the end
) {
    val size: Int get() = starts.size - 1

    /** The listed domain that [host] is, or is a subdomain of; null if none. Never matches a bare TLD. */
    fun lookup(host: String): String? {
        val key = reversedKey(host) ?: return null
        val firstDot = key.indexOfFirst { it == DOT }
        var end = key.size
        while (true) {
            val hit = find(key, end)
            if (hit >= 0) return reverseLabels(hit)
            // Shorten to the parent: cut at the previous label boundary, but keep at least "tld.domain".
            var cut = end - 1
            while (cut >= 0 && key[cut] != DOT) cut--
            if (cut <= firstDot) return null
            end = cut
        }
    }

    fun contains(host: String): Boolean = lookup(host) != null

    private fun find(key: ByteArray, keyLen: Int): Int {
        var lo = 0
        var hi = size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val c = compare(mid, key, keyLen)
            when {
                c < 0 -> lo = mid + 1
                c > 0 -> hi = mid - 1
                else -> return mid
            }
        }
        return -1
    }

    // Entry [i] against key[0 until keyLen].
    private fun compare(i: Int, key: ByteArray, keyLen: Int): Int {
        val s = starts[i]
        val len = starts[i + 1] - s
        val n = minOf(len, keyLen)
        for (k in 0 until n) {
            val d = (data[s + k].toInt() and 0xff) - (key[k].toInt() and 0xff)
            if (d != 0) return d
        }
        return len - keyLen
    }

    private fun reverseLabels(i: Int): String {
        val entry = String(data, starts[i], starts[i + 1] - starts[i], Charsets.US_ASCII)
        return entry.split('.').asReversed().joinToString(".")
    }

    companion object {
        private const val DOT = '.'.code.toByte()
        val EMPTY = DomainList(ByteArray(0), intArrayOf(0))

        /** Reversed lower-case ASCII key of a host, or null if it is not a plain host name with two or more labels. */
        private fun reversedKey(raw: String): ByteArray? {
            val h = raw.trim().lowercase().trimEnd('.').removePrefix("*.")
            if (h.length !in 3..253 || '.' !in h) return null
            val out = ByteArray(h.length)
            var o = 0
            var labelEnd = h.length
            var i = h.length - 1
            while (i >= -1) {
                if (i < 0 || h[i] == '.') {
                    val len = labelEnd - (i + 1)
                    if (len == 0 || len > 63) return null
                    for (k in i + 1 until labelEnd) {
                        val c = h[k]
                        if (!(c in 'a'..'z' || c in '0'..'9' || c == '-' || c == '_')) return null
                        out[o++] = c.code.toByte()
                    }
                    if (i >= 0) out[o++] = DOT
                    labelEnd = i
                }
                i--
            }
            return out
        }

        /**
         * Builds a list from the lines of a hosts file ("0.0.0.0 example.com") or a plain domain list; `#` comments,
         * blank lines, IP-only lines, invalid host names and [SharedServices] entries are dropped. Works on the raw
         * bytes so a 10 MB list is never turned into a String.
         */
        fun parse(bytes: ByteArray): DomainList {
            val keys = ArrayList<ByteArray>()
            var pos = 0
            while (pos < bytes.size) {
                var eol = pos
                while (eol < bytes.size && bytes[eol] != '\n'.code.toByte()) eol++
                lineHost(bytes, pos, eol)?.let { host ->
                    if (!SharedServices.isShared(host)) reversedKey(host)?.let { keys += it }
                }
                pos = eol + 1
            }
            return fromKeys(keys)
        }

        fun parse(text: String): DomainList = parse(text.toByteArray(Charsets.UTF_8))

        fun of(hosts: Iterable<String>): DomainList =
            fromKeys(hosts.filter { !SharedServices.isShared(it) }.mapNotNull { reversedKey(it) })

        private fun fromKeys(keys: List<ByteArray>): DomainList {
            val sorted = keys.sortedWith { a, b -> compareBytes(a, b) }
            val data = java.io.ByteArrayOutputStream(sorted.sumOf { it.size })
            val starts = ArrayList<Int>(sorted.size + 1)
            var prev: ByteArray? = null
            var offset = 0
            for (k in sorted) {
                if (prev != null && compareBytes(prev, k) == 0) continue
                starts += offset
                data.write(k)
                offset += k.size
                prev = k
            }
            starts += offset
            return DomainList(data.toByteArray(), starts.toIntArray())
        }

        private fun compareBytes(a: ByteArray, b: ByteArray): Int {
            val n = minOf(a.size, b.size)
            for (k in 0 until n) {
                val d = (a[k].toInt() and 0xff) - (b[k].toInt() and 0xff)
                if (d != 0) return d
            }
            return a.size - b.size
        }

        /** The host of one line, or null. The first token that is not an IP address, after an optional hosts-file IP. */
        private fun lineHost(b: ByteArray, from: Int, to: Int): String? {
            var s = from
            var e = to
            while (s < e && b[s] <= ' '.code.toByte()) s++
            if (s >= e || b[s] == '#'.code.toByte()) return null
            var hash = s
            while (hash < e && b[hash] != '#'.code.toByte()) hash++
            e = hash
            val text = String(b, s, e - s, Charsets.US_ASCII).trim()
            val parts = text.split(' ', '\t').filter { it.isNotEmpty() }
            val host = when {
                parts.size == 1 -> parts[0]
                parts.size >= 2 && isIpLiteral(parts[0]) -> parts[1]
                else -> return null
            }
            return if (isIpLiteral(host)) null else host
        }

        private fun isIpLiteral(s: String): Boolean =
            ':' in s || (s.isNotEmpty() && s.all { it in '0'..'9' || it == '.' })
    }
}
