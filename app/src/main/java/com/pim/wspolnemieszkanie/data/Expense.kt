package com.pim.wspolnemieszkanie.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/** Kategorie wydatków (FR5). W Firestore zapisujemy nazwę, np. "RENT". */
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

/**
 * flats/{flatId}/expenses/{id}
 *
 * Kwoty w groszach (NFR6). `shares` = ile przypada na każdą osobę: uid → grosze.
 * Suma `shares` zawsze równa się `amount` (pilnuje tego BalanceCalculator.splitEqually).
 * Pola var z wartościami domyślnymi są potrzebne, żeby Firestore umiał odtworzyć obiekt.
 */
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

/** flats/{flatId}/settlements/{id}: spłata długu (FR8). `from` oddał `to` kwotę `amount`. */
data class Settlement(
    var id: String = "",
    var from: String = "",
    var to: String = "",
    var amount: Long = 0,
    var date: Timestamp? = null,
    var createdBy: String = "",
)
