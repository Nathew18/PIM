package com.pim.wspolnemieszkanie.data

data class Debt(val from: String, val to: String, val amount: Long)

// Bez Firebase, żeby dało się to sprawdzić testami jednostkowymi.
object BalanceCalculator {

    // Reszta groszy trafia do pierwszych osób, np. 10000 na 3 -> 3334, 3333, 3333.
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

    // Dodatnie saldo = inni są tej osobie winni, ujemne = ona jest winna innym.
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

    // Zachłannie: największy dłużnik oddaje największemu wierzycielowi, ile się da.
    // Przy remisach decyduje uid, żeby u wszystkich wynik był ten sam.
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
