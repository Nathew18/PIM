package com.pim.wspolnemieszkanie.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceCalculatorTest {

    private fun expense(amount: Long, paidBy: String, vararg people: String) = Expense(
        amount = amount,
        paidBy = paidBy,
        shares = BalanceCalculator.splitEqually(amount, people.toList()),
    )

    // --- splitEqually (FR6) ---

    @Test
    fun splitDividesEvenly() {
        assertEquals(mapOf("a" to 5000L, "b" to 5000L), BalanceCalculator.splitEqually(10000, listOf("a", "b")))
    }

    @Test
    fun splitGivesRemainderToFirstPeople() {
        val shares = BalanceCalculator.splitEqually(10000, listOf("a", "b", "c"))
        assertEquals(mapOf("a" to 3334L, "b" to 3333L, "c" to 3333L), shares)
    }

    @Test
    fun splitSumAlwaysEqualsAmount() {
        for (amount in 1L..2000L) {
            for (n in 1..7) {
                val people = (1..n).map { "u$it" }
                assertEquals(amount, BalanceCalculator.splitEqually(amount, people).values.sum())
            }
        }
    }

    @Test
    fun splitIgnoresDuplicates() {
        assertEquals(mapOf("a" to 300L), BalanceCalculator.splitEqually(300, listOf("a", "a")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun splitRejectsNoPeople() {
        BalanceCalculator.splitEqually(100, emptyList())
    }

    // --- netBalances (FR7) ---

    @Test
    fun oneExpenseSplitInTwo() {
        val balances = BalanceCalculator.netBalances(listOf(expense(10000, "a", "a", "b")), emptyList())
        assertEquals(5000L, balances["a"])
        assertEquals(-5000L, balances["b"])
    }

    @Test
    fun balancesAlwaysSumToZero() {
        val expenses = listOf(
            expense(10000, "a", "a", "b", "c"),
            expense(4599, "b", "a", "b"),
            expense(1, "c", "a", "b", "c"),
        )
        val balances = BalanceCalculator.netBalances(expenses, emptyList())
        assertEquals(0L, balances.values.sum())
    }

    @Test
    fun settlementClearsDebt() {
        val expenses = listOf(expense(10000, "a", "a", "b"))
        val settlements = listOf(Settlement(from = "b", to = "a", amount = 5000))
        val balances = BalanceCalculator.netBalances(expenses, settlements)
        assertEquals(0L, balances["a"])
        assertEquals(0L, balances["b"])
        assertTrue(BalanceCalculator.simplifyDebts(balances).isEmpty())
    }

    @Test
    fun partialSettlementLeavesRest() {
        val expenses = listOf(expense(10000, "a", "a", "b"))
        val settlements = listOf(Settlement(from = "b", to = "a", amount = 2000))
        val debts = BalanceCalculator.simplifyDebts(BalanceCalculator.netBalances(expenses, settlements))
        assertEquals(listOf(Debt("b", "a", 3000)), debts)
    }

    @Test
    fun expenseForOthersOnly() {
        // a płaci za b i c, sam nie korzysta.
        val balances = BalanceCalculator.netBalances(listOf(expense(6000, "a", "b", "c")), emptyList())
        assertEquals(6000L, balances["a"])
        assertEquals(-3000L, balances["b"])
        assertEquals(-3000L, balances["c"])
    }

    // --- simplifyDebts (FR7) ---

    @Test
    fun simplifiesChainOfDebts() {
        // b jest winien a 50 zł, c jest winien b 50 zł → wystarczy jeden przelew c → a.
        val balances = mapOf("a" to 5000L, "b" to 0L, "c" to -5000L)
        assertEquals(listOf(Debt("c", "a", 5000)), BalanceCalculator.simplifyDebts(balances))
    }

    @Test
    fun simplifiedDebtsSettleEveryone() {
        val expenses = listOf(
            expense(12000, "a", "a", "b", "c", "d"),
            expense(3000, "b", "b", "c"),
            expense(9999, "c", "a", "b", "c", "d"),
            expense(150, "d", "a", "d"),
        )
        val balances = BalanceCalculator.netBalances(expenses, emptyList())
        val debts = BalanceCalculator.simplifyDebts(balances)

        // Po wykonaniu wszystkich przelewów każdy ma saldo 0.
        val after = BalanceCalculator.netBalances(
            expenses,
            debts.map { Settlement(from = it.from, to = it.to, amount = it.amount) },
        )
        assertTrue(after.values.all { it == 0L })
        // Przy 4 osobach wystarczą maksymalnie 3 przelewy.
        assertTrue(debts.size <= 3)
        assertTrue(debts.all { it.amount > 0 })
    }

    @Test
    fun noDebtsWhenEveryoneIsEven() {
        assertTrue(BalanceCalculator.simplifyDebts(mapOf("a" to 0L, "b" to 0L)).isEmpty())
    }
}
