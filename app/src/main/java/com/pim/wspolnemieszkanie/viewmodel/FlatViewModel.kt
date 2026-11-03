package com.pim.wspolnemieszkanie.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.pim.wspolnemieszkanie.data.Flat
import com.pim.wspolnemieszkanie.data.FlatRepository

class FlatViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val repository = FlatRepository()
    private var flatListener: ListenerRegistration? = null

    var flat by mutableStateOf<Flat?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isCreating by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // ViewModel żyje dłużej niż sesja, więc po zmianie konta trzeba wczytać mieszkanie od nowa.
    // Bez tego następny użytkownik zobaczyłby mieszkanie i kod poprzedniego.
    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        observeFlat(firebaseAuth.currentUser?.uid)
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun createFlat(name: String) {
        val userId = auth.currentUser?.uid ?: return
        if (name.isBlank()) {
            errorMessage = "Podaj nazwę mieszkania"
            return
        }
        isCreating = true
        errorMessage = null
        repository.createFlat(
            name = name.trim(),
            userId = userId,
            onSuccess = { isCreating = false },
            onError = {
                isCreating = false
                errorMessage = "Nie udało się utworzyć mieszkania, sprawdź połączenie z internetem"
            },
        )
    }

    private fun observeFlat(userId: String?) {
        flatListener?.remove()
        flatListener = null
        flat = null
        errorMessage = null
        isLoading = userId != null
        if (userId == null) return

        flatListener = repository.observeUserFlat(
            userId = userId,
            onChange = { newFlat ->
                flat = newFlat
                isLoading = false
            },
            onError = {
                isLoading = false
                errorMessage = "Nie udało się wczytać mieszkania"
            },
        )
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        flatListener?.remove()
    }
}
