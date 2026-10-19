package io.github.meko123456.barati.android

import java.text.NumberFormat
import java.util.Locale

/**
 * [count] with its digits grouped as [locale]'s language groups them: "1,234" in English, "1 234"
 * in Georgian. An imported deck runs into the thousands, and "1234 cards" beside "1234 due" was a
 * row of digits to count.
 */
internal fun grouped(count: Int, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(count)
