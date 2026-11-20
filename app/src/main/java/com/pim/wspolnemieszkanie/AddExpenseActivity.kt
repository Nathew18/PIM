package com.pim.wspolnemieszkanie

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import com.pim.wspolnemieszkanie.data.ExpenseCategory
import com.pim.wspolnemieszkanie.data.Flat
import com.pim.wspolnemieszkanie.ui.screens.AddExpenseScreen
import com.pim.wspolnemieszkanie.ui.theme.WspolneMieszkanieTheme
import com.pim.wspolnemieszkanie.viewmodel.AddExpenseViewModel

// Wstępnie wypełniony formularz, np. przy zamianie kupionych produktów na wydatek (#16).
data class ExpenseDraft(
    val description: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val amountText: String = "",
)

data class AddExpenseRequest(val flat: Flat, val draft: ExpenseDraft = ExpenseDraft())

data class AddedExpense(val description: String, val amount: Long)

class AddExpenseActivity : ComponentActivity() {
    private val viewModel: AddExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val flatId = intent.getStringExtra(EXTRA_FLAT_ID)
        val memberIds = intent.getStringArrayExtra(EXTRA_MEMBER_IDS)
        val memberNames = intent.getStringArrayExtra(EXTRA_MEMBER_NAMES)
        if (flatId == null || memberIds == null || memberNames == null) {
            finish()
            return
        }
        val draft = ExpenseDraft(
            description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty(),
            category = ExpenseCategory.fromName(intent.getStringExtra(EXTRA_CATEGORY).orEmpty()),
            amountText = intent.getStringExtra(EXTRA_AMOUNT).orEmpty(),
        )

        enableEdgeToEdge()
        setContent {
            WspolneMieszkanieTheme {
                AddExpenseScreen(
                    flatId = flatId,
                    members = memberIds.zip(memberNames),
                    draft = draft,
                    viewModel = viewModel,
                    onSaved = { added ->
                        val result = Intent()
                            .putExtra(EXTRA_RESULT_DESCRIPTION, added.description)
                            .putExtra(EXTRA_RESULT_AMOUNT, added.amount)
                        setResult(Activity.RESULT_OK, result)
                        finish()
                    },
                    onCancel = ::finish,
                )
            }
        }
    }

    class Contract : ActivityResultContract<AddExpenseRequest, AddedExpense?>() {
        override fun createIntent(context: Context, input: AddExpenseRequest): Intent {
            val flat = input.flat
            return Intent(context, AddExpenseActivity::class.java)
                .putExtra(EXTRA_FLAT_ID, flat.id)
                .putExtra(EXTRA_MEMBER_IDS, flat.memberIds.toTypedArray())
                .putExtra(
                    EXTRA_MEMBER_NAMES,
                    flat.memberIds.map { flat.memberNames[it] ?: "Współlokator" }.toTypedArray(),
                )
                .putExtra(EXTRA_DESCRIPTION, input.draft.description)
                .putExtra(EXTRA_CATEGORY, input.draft.category.name)
                .putExtra(EXTRA_AMOUNT, input.draft.amountText)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): AddedExpense? {
            if (resultCode != Activity.RESULT_OK || intent == null) return null
            return AddedExpense(
                description = intent.getStringExtra(EXTRA_RESULT_DESCRIPTION).orEmpty(),
                amount = intent.getLongExtra(EXTRA_RESULT_AMOUNT, 0),
            )
        }
    }

    companion object {
        private const val EXTRA_FLAT_ID = "flat_id"
        private const val EXTRA_MEMBER_IDS = "member_ids"
        private const val EXTRA_MEMBER_NAMES = "member_names"
        private const val EXTRA_DESCRIPTION = "description"
        private const val EXTRA_CATEGORY = "category"
        private const val EXTRA_AMOUNT = "amount"
        private const val EXTRA_RESULT_DESCRIPTION = "result_description"
        private const val EXTRA_RESULT_AMOUNT = "result_amount"
    }
}
