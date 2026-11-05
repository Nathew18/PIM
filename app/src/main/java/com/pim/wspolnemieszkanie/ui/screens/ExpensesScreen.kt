package com.pim.wspolnemieszkanie.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pim.wspolnemieszkanie.data.BalanceCalculator
import com.pim.wspolnemieszkanie.data.Debt
import com.pim.wspolnemieszkanie.data.Expense
import com.pim.wspolnemieszkanie.data.ExpenseCategory
import com.pim.wspolnemieszkanie.data.Flat
import com.pim.wspolnemieszkanie.data.Money
import com.pim.wspolnemieszkanie.data.Settlement
import com.pim.wspolnemieszkanie.viewmodel.ExpenseDraft
import com.pim.wspolnemieszkanie.viewmodel.ExpensesViewModel
import com.google.firebase.Timestamp
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val polish: Locale = Locale.forLanguageTag("pl")
private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", polish)
private val dayFormatter = DateTimeFormatter.ofPattern("d MMM", polish)

/** Zakładka „Wydatki” (Osoba 2): saldo, kto komu ile, historia z filtrami, dodawanie. */
@Composable
fun ExpensesScreen(flat: Flat?, viewModel: ExpensesViewModel) {
    if (flat == null) {
        PlaceholderScreen("Wydatki", "Najpierw utwórz mieszkanie w zakładce Mieszkanie")
        return
    }
    LaunchedEffect(flat.id, flat.memberIds) { viewModel.bind(flat.id, flat.memberIds) }

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
            onClick = { viewModel.openForm() },
            icon = { Text("+", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Dodaj wydatek") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }

    viewModel.formDraft?.let { draft -> AddExpenseDialog(draft, viewModel) }

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

// ---------- Saldo (FR7, FR8) ----------

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

// ---------- Historia i filtry (FR9) ----------

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

// ---------- Formularz (FR5, FR6) ----------

@Composable
private fun AddExpenseDialog(draft: ExpenseDraft, viewModel: ExpensesViewModel) {
    val members = viewModel.memberIds
    var amountText by remember { mutableStateOf(draft.amountText) }
    var description by remember { mutableStateOf(draft.description) }
    var category by remember { mutableStateOf(draft.category) }
    var paidBy by remember { mutableStateOf(viewModel.currentUserId) }
    var participants by remember { mutableStateOf(members.toSet()) }

    // Podgląd podziału na żywo.
    val amount = Money.parse(amountText)
    val ordered = members.filter { it in participants }
    val preview = if (amount != null && amount > 0 && ordered.isNotEmpty()) {
        BalanceCalculator.splitEqually(amount, ordered)
    } else {
        emptyMap()
    }

    AlertDialog(
        onDismissRequest = viewModel::closeForm,
        title = { Text("Nowy wydatek") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Kwota (zł)") },
                    placeholder = { Text("np. 45,90") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis (opcjonalnie)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Kategoria", style = MaterialTheme.typography.titleSmall)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExpenseCategory.entries.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text("${option.icon} ${option.label}") },
                        )
                    }
                }

                Text("Kto zapłacił", style = MaterialTheme.typography.titleSmall)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    members.forEach { uid ->
                        FilterChip(
                            selected = paidBy == uid,
                            onClick = { paidBy = uid },
                            label = { Text(viewModel.displayName(uid)) },
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Podziel między", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        participants = if (participants.size == members.size) emptySet() else members.toSet()
                    }) {
                        Text(if (participants.size == members.size) "Odznacz" else "Wszyscy")
                    }
                }
                members.forEach { uid ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                participants = if (uid in participants) participants - uid else participants + uid
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = uid in participants,
                            onCheckedChange = { checked ->
                                participants = if (checked) participants + uid else participants - uid
                            },
                        )
                        Text(viewModel.displayName(uid), modifier = Modifier.weight(1f))
                        preview[uid]?.let {
                            Text(
                                Money.format(it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                viewModel.formError?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.addExpense(amountText, description, category, paidBy, ordered)
            }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = viewModel::closeForm) { Text("Anuluj") } },
    )
}

// ---------- Pomocnicze ----------

private fun monthLabel(month: YearMonth): String =
    month.format(monthFormatter).replaceFirstChar { it.titlecase(polish) }

private fun dayLabel(timestamp: Timestamp?): String =
    timestamp?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.format(dayFormatter) ?: "teraz"
