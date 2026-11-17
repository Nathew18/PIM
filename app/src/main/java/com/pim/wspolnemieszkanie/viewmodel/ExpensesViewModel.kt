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
import com.pim.wspolnemieszkanie.data.MemberRepository
import com.pim.wspolnemieszkanie.data.Money
import com.pim.wspolnemieszkanie.data.Settlement
import java.time.YearMonth
import java.time.ZoneId

/** Dane startowe formularza. Osoba 3 może go otworzyć z listy zakupów (FR12) przez openForm(). */
data class ExpenseDraft(
    val description: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val amountText: String = "",
)

/** Zakładka „Wydatki”: lista, saldo, spłaty i filtry (FR5–FR9). */
class ExpensesViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val repository = ExpenseRepository()
    private val memberRepository = MemberRepository()

    private var flatId: String? = null
    private var dataListeners: List<ListenerRegistration> = emptyList()
    private var membersListener: ListenerRegistration? = null

    var expenses by mutableStateOf<List<Expense>>(emptyList())
        private set
    var settlements by mutableStateOf<List<Settlement>>(emptyList())
        private set
    var memberIds by mutableStateOf<List<String>>(emptyList())
        private set
    private var memberNames by mutableStateOf<Map<String, String>>(emptyMap())

    var isLoading by mutableStateOf(true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Filtry historii (FR9). null = wszystkie.
    var monthFilter by mutableStateOf<YearMonth?>(null)
    var categoryFilter by mutableStateOf<ExpenseCategory?>(null)

    // Formularz dodawania. null = zamknięty.
    var formDraft by mutableStateOf<ExpenseDraft?>(null)
        private set
    var formError by mutableStateOf<String?>(null)
        private set

    val currentUserId: String get() = auth.currentUser?.uid.orEmpty()

    /** Saldo netto każdej osoby w groszach (FR7). */
    val balances by derivedStateOf { BalanceCalculator.netBalances(expenses, settlements) }

    /** Lista przelewów „kto komu ile” (FR7). */
    val debts: List<Debt> by derivedStateOf { BalanceCalculator.simplifyDebts(balances) }

    val myBalance: Long get() = balances[currentUserId] ?: 0L

    val filteredExpenses: List<Expense> by derivedStateOf {
        expenses.filter { expense ->
            (monthFilter == null || expense.yearMonth() == monthFilter) &&
                (categoryFilter == null || expense.categoryEnum == categoryFilter)
        }
    }

    /** Miesiące, w których są wydatki, od najnowszego. */
    val availableMonths: List<YearMonth> by derivedStateOf {
        expenses.mapNotNull { it.yearMonth() }.distinct().sortedDescending()
    }

    /** Wywoływane z ekranu, gdy znane jest mieszkanie i jego członkowie. */
    fun bind(flatId: String, memberIds: List<String>) {
        if (flatId != this.flatId) {
            this.flatId = flatId
            dataListeners.forEach { it.remove() }
            expenses = emptyList()
            settlements = emptyList()
            isLoading = true
            errorMessage = null
            dataListeners = listOf(
                repository.observeExpenses(
                    flatId,
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
                    flatId,
                    onChange = { settlements = it },
                    onError = { errorMessage = "Nie udało się wczytać spłat" },
                ),
            )
        }
        if (memberIds != this.memberIds) {
            this.memberIds = memberIds
            membersListener?.remove()
            membersListener = memberRepository.observeNames(memberIds) { memberNames = it }
        }
    }

    /** Imię do wyświetlenia. Zawsze "Ty" dla zalogowanej osoby. */
    fun displayName(uid: String): String {
        if (uid == currentUserId) return "Ty"
        memberNames[uid]?.let { return it }
        val index = memberIds.indexOf(uid)
        return if (index >= 0) "Współlokator ${index + 1}" else "Były współlokator"
    }

    fun openForm(draft: ExpenseDraft = ExpenseDraft()) {
        formError = null
        formDraft = draft
    }

    fun closeForm() {
        formDraft = null
        formError = null
    }

    /**
     * Dodaje wydatek podzielony równo między `participants` (FR5, FR6).
     * Zwraca true, gdy dane są poprawne i wydatek poszedł do zapisu.
     */
    fun addExpense(
        amountText: String,
        description: String,
        category: ExpenseCategory,
        paidBy: String,
        participants: List<String>,
    ): Boolean {
        val flatId = flatId ?: return false
        val amount = Money.parse(amountText)
        formError = when {
            amount == null || amount <= 0 -> "Podaj poprawną kwotę, np. 12,50"
            amount > Money.MAX_AMOUNT -> "Kwota jest za duża"
            paidBy.isBlank() -> "Wybierz, kto zapłacił"
            participants.isEmpty() -> "Wybierz co najmniej jedną osobę do podziału"
            else -> null
        }
        if (formError != null || amount == null) return false

        // Kolejność jak w mieszkaniu, żeby reszta groszy trafiała zawsze do tych samych osób.
        val ordered = participants.sortedBy { memberIds.indexOf(it) }
        repository.addExpense(
            flatId = flatId,
            amount = amount,
            description = description.trim().ifBlank { category.label },
            category = category,
            paidBy = paidBy,
            shares = BalanceCalculator.splitEqually(amount, ordered),
            createdBy = currentUserId,
            onError = { errorMessage = "Nie udało się zapisać wydatku" },
        )
        closeForm()
        return true
    }

    fun deleteExpense(expense: Expense) {
        val flatId = flatId ?: return
        repository.deleteExpense(flatId, expense.id) {
            errorMessage = "Nie udało się usunąć wydatku"
        }
    }

    /** Oznacza dług jako spłacony (FR8). */
    fun settle(debt: Debt) {
        val flatId = flatId ?: return
        repository.addSettlement(
            flatId = flatId,
            from = debt.from,
            to = debt.to,
            amount = debt.amount,
            createdBy = currentUserId,
            onError = { errorMessage = "Nie udało się zapisać spłaty" },
        )
    }

    fun clearError() {
        errorMessage = null
    }

    override fun onCleared() {
        dataListeners.forEach { it.remove() }
        membersListener?.remove()
    }
}

private fun Expense.yearMonth(): YearMonth? =
    date?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.let { YearMonth.from(it) }
