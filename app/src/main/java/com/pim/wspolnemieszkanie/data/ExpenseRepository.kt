package com.pim.wspolnemieszkanie.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/** Wydatki i spłaty mieszkania: flats/{flatId}/expenses i flats/{flatId}/settlements. */
class ExpenseRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun expenses(flatId: String) =
        db.collection("flats").document(flatId).collection("expenses")

    private fun settlements(flatId: String) =
        db.collection("flats").document(flatId).collection("settlements")

    /** Wszystkie wydatki mieszkania na żywo, najnowsze pierwsze (NFR2). */
    fun observeExpenses(
        flatId: String,
        onChange: (List<Expense>) -> Unit,
        onError: () -> Unit,
    ): ListenerRegistration =
        expenses(flatId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onError()
                    return@addSnapshotListener
                }
                onChange(snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Expense::class.java)?.copy(id = doc.id)
                })
            }

    /** Wszystkie spłaty mieszkania na żywo, najnowsze pierwsze. */
    fun observeSettlements(
        flatId: String,
        onChange: (List<Settlement>) -> Unit,
        onError: () -> Unit,
    ): ListenerRegistration =
        settlements(flatId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onError()
                    return@addSnapshotListener
                }
                onChange(snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Settlement::class.java)?.copy(id = doc.id)
                })
            }

    /**
     * Zapisuje wydatek (FR5, FR6). Firestore od razu pokazuje go w listenerze (także offline),
     * a onError przychodzi tylko, gdy serwer go odrzuci.
     */
    fun addExpense(
        flatId: String,
        amount: Long,
        description: String,
        category: ExpenseCategory,
        paidBy: String,
        shares: Map<String, Long>,
        createdBy: String,
        onError: () -> Unit,
    ) {
        expenses(flatId).add(
            hashMapOf(
                "amount" to amount,
                "description" to description,
                "category" to category.name,
                "paidBy" to paidBy,
                "shares" to shares,
                "date" to Timestamp.now(),
                "createdBy" to createdBy,
            ),
        ).addOnFailureListener { onError() }
    }

    fun deleteExpense(flatId: String, expenseId: String, onError: () -> Unit) {
        expenses(flatId).document(expenseId).delete().addOnFailureListener { onError() }
    }

    /** Oznacza dług jako spłacony (FR8): `from` oddał `to` kwotę `amount`. */
    fun addSettlement(
        flatId: String,
        from: String,
        to: String,
        amount: Long,
        createdBy: String,
        onError: () -> Unit,
    ) {
        settlements(flatId).add(
            hashMapOf(
                "from" to from,
                "to" to to,
                "amount" to amount,
                "date" to Timestamp.now(),
                "createdBy" to createdBy,
            ),
        ).addOnFailureListener { onError() }
    }
}
