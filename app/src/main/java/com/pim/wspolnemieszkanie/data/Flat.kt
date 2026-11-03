package com.pim.wspolnemieszkanie.data

// Pola var z wartościami domyślnymi są potrzebne, żeby Firestore umiał odtworzyć obiekt.
data class Flat(
    var id: String = "",
    var name: String = "",
    var inviteCode: String = "",
    var ownerId: String = "",
    var memberIds: List<String> = emptyList(),
)
