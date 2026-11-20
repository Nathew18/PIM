package com.pim.wspolnemieszkanie.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

enum class ExpenseCategory(val label: String, val icon: String) {
    RENT("Czynsz", "🏠"),
    UTILITIES("Media", "💡"),
    SHOPPING("Zakupy", "🛒"),
    OTHER("Inne", "📦");

    companion object {
        fun fromName(name: String): ExpenseCategory =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}

// Kwoty w groszach. shares: uid -> grosze, suma zawsze równa amount.
data class Expense(
    var id: String = "",
    var amount: Long = 0,
    var description: String = "",
    var category: String = ExpenseCategory.OTHER.name,
    var paidBy: String = "",
    var shares: Map<String, Long> = emptyMap(),
    var date: Timestamp? = null,
    var createdBy: String = "",
) {
    @get:Exclude
    val categoryEnum: ExpenseCategory get() = ExpenseCategory.fromName(category)
}

// Spłata długu: from oddał to kwotę amount.
data class Settlement(
    var id: String = "",
    var from: String = "",
    var to: String = "",
    var amount: Long = 0,
    var date: Timestamp? = null,
    var createdBy: String = "",
)
