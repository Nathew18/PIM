package com.pim.wspolnemieszkanie.data

import com.google.firebase.Timestamp

data class ShoppingItem (
    var id: String = "",
    var name: String = "",
    var quantity: Int = 1,
    var bought: Boolean = false,
    var addedBy: String = "",
    var addedAt: Timestamp? = null
)
