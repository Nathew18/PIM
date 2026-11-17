package com.pim.wspolnemieszkanie.data

/** Jeden przelew do zrobienia: `from` oddaje `to` kwotę `amount` (w groszach). */
data class Debt(val from: String, val to: String, val amount: Long)

/**
 * Cała matematyka wydatków (FR6, FR7) w czystym Kotlinie, bez Firebase,
 * żeby dało się ją sprawdzić testami jednostkowymi (NFR6).
 */
object BalanceCalculator {

    /**
     * Dzieli kwotę równo między osoby (FR6). Grosze, które się nie dzielą,
     * dostają pierwsze osoby z listy, więc suma udziałów zawsze równa się kwocie.
     * Przykład: 10000 gr na 3 osoby → 3334, 3333, 3333.
     */
    fun splitEqually(amount: Long, people: List<String>): Map<String, Long> {
        require(amount > 0) { "Kwota musi być dodatnia" }
        val unique = people.distinct()
        require(unique.isNotEmpty()) { "Brak osób do podziału" }

        val base = amount / unique.size
        val remainder = (amount % unique.size).toInt()
        return unique.mapIndexed { index, person ->
            person to base + if (index < remainder) 1 else 0
        }.toMap()
    }

    /**
     * Saldo netto każdej osoby w groszach (FR7).
     * Dodatnie = inni są jej winni, ujemne = ona jest winna innym.
     *
     * Wydatek: płacący +amount, każda osoba z podziału −swój udział.
     * Spłata `from` → `to`: dłużnik `from` +amount, wierzyciel `to` −amount.
     */
    fun netBalances(expenses: List<Expense>, settlements: List<Settlement>): Map<String, Long> {
        val balances = mutableMapOf<String, Long>()
        fun add(uid: String, value: Long) {
            balances[uid] = (balances[uid] ?: 0L) + value
        }

        expenses.forEach { expense ->
            add(expense.paidBy, expense.amount)
            expense.shares.forEach { (uid, share) -> add(uid, -share) }
        }
        settlements.forEach { settlement ->
            add(settlement.from, settlement.amount)
            add(settlement.to, -settlement.amount)
        }
        return balances
    }

    /**
     * Zamienia salda na jak najmniej przelewów (algorytm zachłanny):
     * największy dłużnik oddaje największemu wierzycielowi, ile się da, i tak do zera.
     * Przy remisach decyduje uid, żeby u wszystkich wynik był ten sam.
     */
    fun simplifyDebts(balances: Map<String, Long>): List<Debt> {
        val order = compareByDescending<Pair<String, Long>> { it.second }.thenBy { it.first }
        val debtors = balances.filter { it.value < 0 }.map { it.key to -it.value }.toMutableList()
        val creditors = balances.filter { it.value > 0 }.map { it.key to it.value }.toMutableList()
        val result = mutableListOf<Debt>()

        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            debtors.sortWith(order)
            creditors.sortWith(order)
            val (debtor, owes) = debtors[0]
            val (creditor, isOwed) = creditors[0]
            val transfer = minOf(owes, isOwed)

            result += Debt(from = debtor, to = creditor, amount = transfer)

            if (owes == transfer) debtors.removeAt(0) else debtors[0] = debtor to owes - transfer
            if (isOwed == transfer) creditors.removeAt(0) else creditors[0] = creditor to isOwed - transfer
        }
        return result
    }
}
