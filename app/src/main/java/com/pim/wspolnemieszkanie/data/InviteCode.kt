package com.pim.wspolnemieszkanie.data

import java.security.SecureRandom

const val INVITE_CODE_LENGTH = 6

// Bez znaków łatwych do pomylenia przy przepisywaniu: 0 i O, 1 i I.
const val INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

private val random = SecureRandom()

fun generateInviteCode(): String =
    (1..INVITE_CODE_LENGTH)
        .map { INVITE_CODE_ALPHABET[random.nextInt(INVITE_CODE_ALPHABET.length)] }
        .joinToString("")
