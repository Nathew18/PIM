package com.pim.wspolnemieszkanie.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.pim.wspolnemieszkanie.data.Flat
import com.pim.wspolnemieszkanie.data.ShoppingItem
import com.pim.wspolnemieszkanie.data.ShoppingRepository

class ShoppingViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val repository = ShoppingRepository()

    private var flatId: String? = null
    private var listener: ListenerRegistration? = null

    var items by mutableStateOf<List<ShoppingItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val currentUserId: String get() = auth.currentUser?.uid.orEmpty()

    fun bind(flat: Flat) {
        if (flat.id == flatId) return
        flatId = flat.id
        listener?.remove()
        items = emptyList()
        isLoading = true
        errorMessage = null

        listener = repository.observeItems(
            flatId = flat.id,
            onChange = { list ->
                // Niekupione na górze, kupione na dole
                items = list.sortedBy { it.bought }
                isLoading = false
            },
            onError = {
                isLoading = false
                errorMessage = "Nie udało się wczytać listy zakupów"
            },
        )
    }

    fun unbind() {
        listener?.remove()
        listener = null
        flatId = null
        items = emptyList()
    }

    fun addItem(name: String, quantityText: String): Boolean {
        val currentFlatId = flatId ?: return false
        val trimmed = name.trim()
        val quantity = quantityText.trim().toIntOrNull()

        if (trimmed.isEmpty()) {
            errorMessage = "Podaj nazwę produktu"
            return false
        }
        if (quantity == null || quantity <= 0) {
            errorMessage = "Podaj poprawną ilość"
            return false
        }

        errorMessage = null
        repository.addItem(currentFlatId, trimmed, quantity, currentUserId) {
            errorMessage = "Nie udało się dodać produktu"
        }
        return true
    }

    fun toggleBought(item: ShoppingItem) {
        val currentFlatId = flatId ?: return
        repository.toggleBought(currentFlatId, item.id, !item.bought) {
            errorMessage = "Nie udało się zaktualizować produktu"
        }
    }

    fun deleteItem(item: ShoppingItem) {
        val currentFlatId = flatId ?: return
        repository.deleteItem(currentFlatId, item.id) {
            errorMessage = "Nie udało się usunąć produktu"
        }
    }

    fun clearError() {
        errorMessage = null
    }

    override fun onCleared() {
        listener?.remove()
    }
}