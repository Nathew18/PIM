package com.pim.wspolnemieszkanie.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.pim.wspolnemieszkanie.AddedExpense
import com.pim.wspolnemieszkanie.data.BalanceCalculator
import com.pim.wspolnemieszkanie.data.ExpenseCategory
import com.pim.wspolnemieszkanie.data.ExpenseRepository
import com.pim.wspolnemieszkanie.data.Money

class AddExpenseViewModel : ViewModel() {

    private val repository = ExpenseRepository()

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Aktywność zamyka się z opóźnieniem, więc szybkie drugie kliknięcie dodałoby wydatek drugi raz.
    var isSaved by mutableStateOf(false)
        private set

    val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    fun clearError() {
        errorMessage = null
    }

    // null = błąd walidacji, formularz zostaje otwarty.
    fun save(
        flatId: String,
        memberIds: List<String>,
        amountText: String,
        description: String,
        category: ExpenseCategory,
        paidBy: String,
        participants: List<String>,
    ): AddedExpense? {
        if (isSaved) return null
        val amount = Money.parse(amountText)
        errorMessage = when {
            amount == null || amount <= 0 -> "Podaj poprawną kwotę, np. 12,50"
            amount > Money.MAX_AMOUNT -> "Kwota jest za duża"
            paidBy.isBlank() -> "Wybierz, kto zapłacił"
            participants.isEmpty() -> "Wybierz co najmniej jedną osobę do podziału"
            else -> null
        }
        if (errorMessage != null || amount == null) return null

        // Kolejność jak w mieszkaniu, żeby reszta groszy trafiała zawsze do tych samych osób.
        val ordered = participants.sortedBy { memberIds.indexOf(it) }
        val finalDescription = description.trim().ifBlank { category.label }
        // Nie czekamy na serwer: Firestore pokazuje wydatek od razu, także offline.
        repository.addExpense(
            flatId = flatId,
            amount = amount,
            description = finalDescription,
            category = category,
            paidBy = paidBy,
            shares = BalanceCalculator.splitEqually(amount, ordered),
            createdBy = currentUserId,
        )
        isSaved = true
        return AddedExpense(finalDescription, amount)
    }
}
