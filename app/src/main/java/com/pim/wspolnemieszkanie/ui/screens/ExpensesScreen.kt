package com.pim.wspolnemieszkanie.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pim.wspolnemieszkanie.AddExpenseActivity
import com.pim.wspolnemieszkanie.AddExpenseRequest
import com.pim.wspolnemieszkanie.data.Debt
import com.pim.wspolnemieszkanie.data.Expense
import com.pim.wspolnemieszkanie.data.ExpenseCategory
import com.pim.wspolnemieszkanie.data.Flat
import com.pim.wspolnemieszkanie.data.Money
import com.pim.wspolnemieszkanie.data.Settlement
import com.pim.wspolnemieszkanie.viewmodel.ExpensesViewModel
import com.google.firebase.Timestamp
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val polish: Locale = Locale.forLanguageTag("pl")
private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", polish)
private val dayFormatter = DateTimeFormatter.ofPattern("d MMM", polish)

@Composable
fun ExpensesScreen(flat: Flat?, viewModel: ExpensesViewModel) {
    if (flat == null) {
        PlaceholderScreen("Wydatki", "Najpierw utwórz mieszkanie albo dołącz do niego w zakładce Mieszkanie")
        return
    }
    LaunchedEffect(flat) { viewModel.bind(flat) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addExpense = rememberLauncherForActivityResult(AddExpenseActivity.Contract()) { added ->
        if (added != null) {
            scope.launch {
                snackbarHostState.showSnackbar("Dodano: ${added.description} (${Money.format(added.amount)})")
            }
        }
    }

    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    var debtToSettle by remember { mutableStateOf<Debt?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (viewModel.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { BalanceCard(viewModel, onSettle = { debtToSettle = it }) }

                viewModel.errorMessage?.let { message ->
                    item {
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { viewModel.clearError() },
                        )
                    }
                }

                item { HistoryHeader(viewModel) }

                val filtered = viewModel.filteredExpenses
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            if (viewModel.expenses.isEmpty()) {
                                "Nie ma jeszcze wydatków. Dodaj pierwszy przyciskiem na dole."
                            } else {
                                "Brak wydatków dla wybranych filtrów."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
                items(filtered, key = { it.id }) { expense ->
                    ExpenseRow(expense, viewModel, onClick = { selectedExpense = expense })
                }

                if (viewModel.settlements.isNotEmpty()) {
                    item {
                        Text(
                            "Spłaty",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(viewModel.settlements.take(10), key = { "s-" + it.id }) { settlement ->
                        SettlementRow(settlement, viewModel)
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { addExpense.launch(AddExpenseRequest(flat)) },
            icon = { Text("+", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Dodaj wydatek") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
        )
    }

    selectedExpense?.let { expense ->
        ExpenseDetailsDialog(
            expense = expense,
            viewModel = viewModel,
            onDismiss = { selectedExpense = null },
        )
    }

    debtToSettle?.let { debt ->
        AlertDialog(
            onDismissRequest = { debtToSettle = null },
            title = { Text("Oznaczyć jako spłacone?") },
            text = {
                Text(
                    "${viewModel.displayName(debt.from)} → ${viewModel.displayName(debt.to)}: " +
                        "${Money.format(debt.amount)}.\nZrób to dopiero, gdy pieniądze faktycznie zostały oddane."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.settle(debt)
                    debtToSettle = null
                }) { Text("Spłacone") }
            },
            dismissButton = { TextButton(onClick = { debtToSettle = null }) { Text("Anuluj") } },
        )
    }
}

@Composable
private fun BalanceCard(viewModel: ExpensesViewModel, onSettle: (Debt) -> Unit) {
    val me = viewModel.currentUserId
    val myBalance = viewModel.myBalance
    val debts = viewModel.debts

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Twoje saldo", style = MaterialTheme.typography.titleMedium)
            Text(
                when {
                    myBalance > 0 -> "Inni są Ci winni ${Money.format(myBalance)}"
                    myBalance < 0 -> "Jesteś winien ${Money.format(-myBalance)}"
                    else -> "Jesteś rozliczony 🎉"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    myBalance < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                },
            )

            if (debts.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Kto komu ile", style = MaterialTheme.typography.titleSmall)
                debts.forEach { debt ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${viewModel.displayName(debt.from)} → ${viewModel.displayName(debt.to)}",
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(Money.format(debt.amount), fontWeight = FontWeight.SemiBold)
                        // Spłatę może oznaczyć dłużnik albo wierzyciel.
                        if (debt.from == me || debt.to == me) {
                            TextButton(onClick = { onSettle(debt) }) { Text("Spłacone") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryHeader(viewModel: ExpensesViewModel) {
    var monthMenuOpen by remember { mutableStateOf(false) }
    val total = viewModel.filteredExpenses.sumOf { it.amount }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Historia", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Box {
                OutlinedButton(onClick = { monthMenuOpen = true }) {
                    Text((viewModel.monthFilter?.let { monthLabel(it) } ?: "Wszystkie miesiące") + " ▾")
                }
                DropdownMenu(expanded = monthMenuOpen, onDismissRequest = { monthMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Wszystkie miesiące") },
                        onClick = {
                            viewModel.monthFilter = null
                            monthMenuOpen = false
                        },
                    )
                    viewModel.availableMonths.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(monthLabel(month)) },
                            onClick = {
                                viewModel.monthFilter = month
                                monthMenuOpen = false
                            },
                        )
                    }
                }
            }
        }

        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = viewModel.categoryFilter == null,
                onClick = { viewModel.categoryFilter = null },
                label = { Text("Wszystkie") },
            )
            ExpenseCategory.entries.forEach { category ->
                FilterChip(
                    selected = viewModel.categoryFilter == category,
                    onClick = {
                        viewModel.categoryFilter = if (viewModel.categoryFilter == category) null else category
                    },
                    label = { Text("${category.icon} ${category.label}") },
                )
            }
        }

        if (viewModel.filteredExpenses.isNotEmpty()) {
            Text(
                "Suma: ${Money.format(total)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense, viewModel: ExpensesViewModel, onClick: () -> Unit) {
    val myShare = expense.shares[viewModel.currentUserId]

    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(expense.categoryEnum.icon, style = MaterialTheme.typography.titleLarge)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(expense.description, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(
                    "${dayLabel(expense.date)} · płacił(a): ${viewModel.displayName(expense.paidBy)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(expense.amount), fontWeight = FontWeight.Bold)
                if (myShare != null) {
                    Text(
                        "Twoje: ${Money.format(myShare)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettlementRow(settlement: Settlement, viewModel: ExpensesViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "✅ ${viewModel.displayName(settlement.from)} → ${viewModel.displayName(settlement.to)} · ${dayLabel(settlement.date)}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(Money.format(settlement.amount), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ExpenseDetailsDialog(expense: Expense, viewModel: ExpensesViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(expense.description) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${expense.categoryEnum.icon} ${expense.categoryEnum.label} · ${dayLabel(expense.date)}")
                Text("Kwota: ${Money.format(expense.amount)}", fontWeight = FontWeight.SemiBold)
                Text("Zapłacił(a): ${viewModel.displayName(expense.paidBy)}")
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Podział", style = MaterialTheme.typography.titleSmall)
                expense.shares.forEach { (uid, share) ->
                    Row {
                        Text(viewModel.displayName(uid), modifier = Modifier.weight(1f))
                        Text(Money.format(share))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } },
        dismissButton = {
            // Usunąć może tylko osoba, która dodała wydatek.
            if (expense.createdBy == viewModel.currentUserId) {
                TextButton(onClick = {
                    viewModel.deleteExpense(expense)
                    onDismiss()
                }) { Text("Usuń", color = MaterialTheme.colorScheme.error) }
            }
        },
    )
}

private fun monthLabel(month: YearMonth): String =
    month.format(monthFormatter).replaceFirstChar { it.titlecase(polish) }

private fun dayLabel(timestamp: Timestamp?): String =
    timestamp?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.format(dayFormatter) ?: "teraz"
