package com.pim.wspolnemieszkanie.data

import kotlin.math.abs

// Kwoty to grosze jako Long, bo Double daje błędy zaokrągleń (0.1 + 0.2 != 0.3).
object Money {

    // 1 000 000,00 zł
    const val MAX_AMOUNT = 100_000_000L

    private val amountPattern = Regex("""^(\d{1,7})(?:\.(\d{1,2}))?$""")

    // Akceptuje "12", "12,5", "12.50", "1 234,56", "12 zł". null dla złych danych.
    fun parse(input: String): Long? {
        val cleaned = input
            .replace("zł", "", ignoreCase = true)
            .filterNot { it.isWhitespace() }
            .replace(',', '.')
        val match = amountPattern.matchEntire(cleaned) ?: return null
        val zloty = match.groupValues[1].toLong()
        val grosze = match.groupValues[2].padEnd(2, '0').toLong()
        return zloty * 100 + grosze
    }

    fun format(grosze: Long): String {
        val sign = if (grosze < 0) "-" else ""
        val value = abs(grosze)
        val zloty = (value / 100).toString()
            .reversed()
            .chunked(3)
            .joinToString(" ")
            .reversed()
        val rest = (value % 100).toString().padStart(2, '0')
        return "$sign$zloty,$rest zł"
    }
}
