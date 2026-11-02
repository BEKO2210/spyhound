package de.belkis.spyhound.ui

import java.text.NumberFormat
import java.util.Locale

/** Groups digits for the given language (4.200 in German, 4,200 in English); never uses the system default. */
fun formatCount(value: Int, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(value)
