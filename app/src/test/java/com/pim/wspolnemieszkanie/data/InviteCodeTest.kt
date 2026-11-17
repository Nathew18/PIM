package com.pim.wspolnemieszkanie.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCodeTest {

    @Test
    fun codeHasCorrectLengthAndOnlyAllowedCharacters() {
        repeat(1000) {
            val code = generateInviteCode()
            assertEquals(INVITE_CODE_LENGTH, code.length)
            assertTrue(code, code.all { it in INVITE_CODE_ALPHABET })
        }
    }

    @Test
    fun alphabetHasNoConfusingCharacters() {
        assertTrue(INVITE_CODE_ALPHABET.none { it in "01OI" })
    }

    @Test
    fun normalizeRemovesSpacesAndUppercases() {
        assertEquals("MCHBDX", normalizeInviteCode("  mchbdx "))
    }

    @Test
    fun generatedCodeIsValid() {
        assertTrue(isValidInviteCode(generateInviteCode()))
    }

    @Test
    fun invalidCodesAreRejected() {
        assertFalse(isValidInviteCode(""))
        assertFalse(isValidInviteCode("ABC"))
        assertFalse(isValidInviteCode("ABCDEFG"))
        assertFalse(isValidInviteCode("ABC0EF"))
        assertFalse(isValidInviteCode("AB/DEF"))
        assertFalse(isValidInviteCode("https://example.com"))
    }
}
