package de.belkis.spyhound.ioc

/**
 * Positive list against poisoned or broken hash sources: fingerprints of files that can never be a malware sample
 * (the empty file, trivial one-byte or line-break files, and the system files of a stock Pixel from
 * `assets/iocs/knowngood-pixel.sha256`). A source that lists one is dropped as a whole by [IocStore.load], and
 * [IocSet] never answers a lookup for them.
 */
object KnownGood {
    private val trivial = listOf(
        ByteArray(0),
        byteArrayOf(0),
        "\n".toByteArray(),
        "\r\n".toByteArray(),
    )

    val sha256: Set<String> = trivial.map { IocStore.sha256(it) }.toSet()

    @Volatile private var system: Set<String> = emptySet()

    /** Adds the system-file list; called once by [IocStore.load] before any source is parsed. */
    fun useSystemList(text: String) {
        system = parseList(text)
    }

    /** One hash per line, optionally followed by the file path; `#` starts a comment. Malformed lines are skipped. */
    fun parseList(text: String): Set<String> =
        text.lineSequence()
            .map { it.substringBefore('#').trim().substringBefore(' ').lowercase() }
            .filter { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } }
            .toSet()

    fun contains(hash: String): Boolean = hash.trim().lowercase().let { it in sha256 || it in system }

    fun conflicts(indicators: Iterable<Indicator>): List<Indicator> =
        indicators.filter { it.type == IocType.SHA256 && contains(it.value) }
}
