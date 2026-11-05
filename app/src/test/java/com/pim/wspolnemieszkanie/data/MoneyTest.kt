package com.pim.wspolnemieszkanie.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun parsesTypicalInputs() {
        assertEquals(1200L, Money.parse("12"))
        assertEquals(1250L, Money.parse("12,5"))
        assertEquals(1250L, Money.parse("12,50"))
        assertEquals(1250L, Money.parse("12.50"))
        assertEquals(5L, Money.parse("0,05"))
        assertEquals(123456L, Money.parse("1 234,56"))
        assertEquals(1999L, Money.parse(" 19,99 zł "))
    }

    @Test
    fun rejectsInvalidInputs() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("-5"))
        assertNull(Money.parse("12,345"))   // 3 miejsca po przecinku
        assertNull(Money.parse("12,"))
        assertNull(Money.parse(",50"))
        assertNull(Money.parse("1,2,3"))
    }

    @Test
    fun formatsAmounts() {
        assertEquals("0,00 zł", Money.format(0))
        assertEquals("0,05 zł", Money.format(5))
        assertEquals("12,50 zł", Money.format(1250))
        assertEquals("1 234,56 zł", Money.format(123456))
        assertEquals("1 000 000,00 zł", Money.format(100_000_000))
        assertEquals("-5,00 zł", Money.format(-500))
    }

    @Test
    fun parseAndFormatRoundTrip() {
        listOf("0,01", "9,99", "100,00", "2 500,75").forEach { text ->
            assertEquals("$text zł", Money.format(Money.parse(text)!!))
        }
    }
}
