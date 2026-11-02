package de.belkis.spyhound.ioc

/**
 * Positive list against poisoned or broken hash sources: fingerprints of files that can never be a malware sample
 * (the empty file and trivial one-byte or line-break files that every system contains). A source that lists one is
 * dropped as a whole by [IocStore.load], and [IocSet] never answers a lookup for them.
 */
object KnownGood {
    private val trivial = listOf(
        ByteArray(0),
        byteArrayOf(0),
        "\n".toByteArray(),
        "\r\n".toByteArray(),
    )

    val sha256: Set<String> = trivial.map { IocStore.sha256(it) }.toSet()

    fun contains(hash: String): Boolean = hash.trim().lowercase() in sha256

    fun conflicts(indicators: Iterable<Indicator>): List<Indicator> =
        indicators.filter { it.type == IocType.SHA256 && contains(it.value) }
}
