package de.belkis.spyhound.ui

/** Joins accessibility parts into one spoken chain with ". " between sentences, without doubling end punctuation. */
fun joinAccessibilitySentenceParts(parts: Iterable<String>): String {
    val cleaned = parts.map { it.trim() }.filter { it.isNotEmpty() }
    if (cleaned.isEmpty()) return ""
    return buildString {
        append(cleaned.first())
        for (i in 1 until cleaned.size) {
            append(if (cleaned[i - 1].endsWithAccessibilitySentenceEnd()) " " else ". ")
            append(cleaned[i])
        }
    }
}

private val TRAILING_CLOSERS = setOf(
    '"',
    '\'',
    '\u201C',
    '\u201D',
    '\u201E',
    '\u201F',
    '\u2018',
    '\u2019',
    '\u00BB',
    '\u203A',
    ')',
    ']',
    '}',
)

private fun String.endsWithAccessibilitySentenceEnd(): Boolean {
    var s = trimEnd()
    while (s.isNotEmpty() && s.last() in TRAILING_CLOSERS) {
        s = s.dropLast(1).trimEnd()
    }
    if (s.isEmpty()) return false
    return when (s.last()) {
        '.', '!', '?', ':', '\u2026' -> true
        else -> false
    }
}