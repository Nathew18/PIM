package com.pim.wspolnemieszkanie.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pim.wspolnemieszkanie.AddedExpense
import com.pim.wspolnemieszkanie.ExpenseDraft
import com.pim.wspolnemieszkanie.data.BalanceCalculator
import com.pim.wspolnemieszkanie.data.ExpenseCategory
import com.pim.wspolnemieszkanie.data.Money
import com.pim.wspolnemieszkanie.viewmodel.AddExpenseViewModel

@Composable
fun AddExpenseScreen(
    flatId: String,
    members: List<Pair<String, String>>,
    draft: ExpenseDraft,
    viewModel: AddExpenseViewModel,
    onSaved: (AddedExpense) -> Unit,
    onCancel: () -> Unit,
) {
    val memberIds = members.map { it.first }
    fun nameOf(uid: String, name: String) = if (uid == viewModel.currentUserId) "$name (Ty)" else name

    var amountText by rememberSaveable { mutableStateOf(draft.amountText) }
    var description by rememberSaveable { mutableStateOf(draft.description) }
    var category by rememberSaveable { mutableStateOf(draft.category) }
    var paidBy by rememberSaveable { mutableStateOf(viewModel.currentUserId) }
    var participants by rememberSaveable { mutableStateOf(memberIds) }

    val amount = Money.parse(amountText)
    val preview = if (amount != null && amount > 0 && participants.isNotEmpty()) {
        BalanceCalculator.splitEqually(amount, memberIds.filter { it in participants })
    } else {
        emptyMap()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Nowy wydatek", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    viewModel.clearError()
                },
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

            SectionTitle("Kategoria")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExpenseCategory.entries.forEach { option ->
                    FilterChip(
                        selected = option == category,
                        onClick = { category = option },
                        label = { Text("${option.icon} ${option.label}") },
                    )
                }
            }

            SectionTitle("Kto zapłacił")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                members.forEach { (uid, name) ->
                    FilterChip(
                        selected = uid == paidBy,
                        onClick = { paidBy = uid },
                        label = { Text(nameOf(uid, name)) },
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Podziel między", Modifier.weight(1f))
                val everyone = participants.size == memberIds.size
                TextButton(onClick = { participants = if (everyone) emptyList() else memberIds }) {
                    Text(if (everyone) "Odznacz" else "Wszyscy")
                }
            }
            members.forEach { (uid, name) ->
                val checked = uid in participants
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = checked,
                            onValueChange = { participants = if (it) participants + uid else participants - uid },
                            role = Role.Checkbox,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Text(
                        nameOf(uid, name),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                    )
                    preview[uid]?.let {
                        Text(
                            Money.format(it),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            viewModel.errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    viewModel.save(flatId, memberIds, amountText, description, category, paidBy, participants)
                        ?.let(onSaved)
                },
                enabled = !viewModel.isSaved,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Zapisz wydatek")
            }
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Anuluj")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(top = 8.dp))
}
