package com.pim.wspolnemieszkanie.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.pim.wspolnemieszkanie.data.BalanceCalculator
import com.pim.wspolnemieszkanie.data.Debt
import com.pim.wspolnemieszkanie.data.Expense
import com.pim.wspolnemieszkanie.data.ExpenseCategory
import com.pim.wspolnemieszkanie.data.ExpenseRepository
import com.pim.wspolnemieszkanie.data.Flat
import com.pim.wspolnemieszkanie.data.Settlement
import java.time.YearMonth
import java.time.ZoneId

class ExpensesViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val repository = ExpenseRepository()

    private var flatId: String? = null
    private var listeners: List<ListenerRegistration> = emptyList()

    // Blokuje drugą spłatę, zanim lokalny zapis wróci w listenerze i przeliczy długi.
    private var isSettling = false

    var expenses by mutableStateOf<List<Expense>>(emptyList())
        private set
    var settlements by mutableStateOf<List<Settlement>>(emptyList())
        private set
    private var memberIds by mutableStateOf<List<String>>(emptyList())
    private var memberNames by mutableStateOf<Map<String, String>>(emptyMap())

    var isLoading by mutableStateOf(true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // null = bez filtra.
    var monthFilter by mutableStateOf<YearMonth?>(null)
    var categoryFilter by mutableStateOf<ExpenseCategory?>(null)

    val currentUserId: String get() = auth.currentUser?.uid.orEmpty()

    val balances by derivedStateOf { BalanceCalculator.netBalances(expenses, settlements) }

    val debts: List<Debt> by derivedStateOf { BalanceCalculator.simplifyDebts(balances) }

    val myBalance: Long get() = balances[currentUserId] ?: 0L

    val filteredExpenses: List<Expense> by derivedStateOf {
        expenses.filter { expense ->
            (monthFilter == null || expense.yearMonth() == monthFilter) &&
                (categoryFilter == null || expense.categoryEnum == categoryFilter)
        }
    }

    val availableMonths: List<YearMonth> by derivedStateOf {
        expenses.mapNotNull { it.yearMonth() }.distinct().sortedDescending()
    }

    fun bind(flat: Flat) {
        memberIds = flat.memberIds
        memberNames = flat.memberNames
        if (flat.id == flatId) return

        flatId = flat.id
        listeners.forEach { it.remove() }
        expenses = emptyList()
        settlements = emptyList()
        isSettling = false
        isLoading = true
        errorMessage = null
        listeners = listOf(
            repository.observeExpenses(
                flat.id,
                onChange = {
                    expenses = it
                    isLoading = false
                },
                onError = {
                    isLoading = false
                    errorMessage = "Nie udało się wczytać wydatków"
                },
            ),
            repository.observeSettlements(
                flat.id,
                onChange = {
                    settlements = it
                    isSettling = false
                },
                onError = { errorMessage = "Nie udało się wczytać spłat" },
            ),
        )
    }

    fun unbind() {
        isSettling = false
        listeners.forEach { it.remove() }
        listeners = emptyList()
        flatId = null
        expenses = emptyList()
        settlements = emptyList()
    }

    fun displayName(uid: String): String = when {
        uid == currentUserId -> "Ty"
        uid in memberIds -> memberNames[uid] ?: "Współlokator"
        else -> "Były współlokator"
    }

    fun deleteExpense(expense: Expense) {
        val flatId = flatId ?: return
        repository.deleteExpense(flatId, expense.id) {
            errorMessage = "Nie udało się usunąć wydatku"
        }
    }

    // Dialog pokazuje dług z chwili otwarcia, a w międzyczasie ktoś mógł go już spłacić.
    fun settle(debt: Debt) {
        val flatId = flatId ?: return
        if (isSettling) return
        val current = debts.firstOrNull { it.from == debt.from && it.to == debt.to } ?: return
        isSettling = true
        repository.addSettlement(
            flatId = flatId,
            from = debt.from,
            to = debt.to,
            amount = minOf(debt.amount, current.amount),
            createdBy = currentUserId,
            onError = {
                isSettling = false
                errorMessage = "Nie udało się zapisać spłaty"
            },
        )
    }

    fun clearError() {
        errorMessage = null
    }

    override fun onCleared() {
        listeners.forEach { it.remove() }
    }
}

private fun Expense.yearMonth(): YearMonth? =
    date?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.let { YearMonth.from(it) }
