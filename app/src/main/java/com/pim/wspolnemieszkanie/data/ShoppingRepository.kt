package com.pim.wspolnemieszkanie.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ShoppingRepository {
    private val db = FirebaseFirestore.getInstance()
    private fun shoppingList(flatId: String) =
        db.collection("flats").document(flatId).collection("shopping")

    fun observeItems(
        flatId: String,
        onChange: (List<ShoppingItem>) -> Unit,
        onError: () -> Unit,
    ): ListenerRegistration =
        shoppingList(flatId)
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onError()
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ShoppingItem::class.java)?.copy(id = doc.id)
                }
                onChange(list)
            }

    fun addItem(
        flatId: String,
        name: String,
        quantity: Int,
        addedBy: String,
        onError: () -> Unit,
    ) {
        shoppingList(flatId).add(
            hashMapOf(
                "name" to name,
                "quantity" to quantity,
                "bought" to false,
                "addedBy" to addedBy,
                "addedAt" to Timestamp.now(),
            )
        ).addOnFailureListener { onError() }
    }

    fun toggleBought(flatId: String, itemId: String, bought: Boolean, onError: () -> Unit) {
        shoppingList(flatId).document(itemId)
            .update("bought", bought)
            .addOnFailureListener { onError() }
    }

    fun deleteItem(flatId: String, itemId: String, onError: () -> Unit) {
        shoppingList(flatId).document(itemId)
            .delete()
            .addOnFailureListener { onError() }
    }
}