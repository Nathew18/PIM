package com.pim.wspolnemieszkanie.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.pim.wspolnemieszkanie.data.Flat
import com.pim.wspolnemieszkanie.data.FlatRepository
import com.pim.wspolnemieszkanie.data.isValidInviteCode
import com.pim.wspolnemieszkanie.data.normalizeInviteCode

class FlatViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val repository = FlatRepository()
    private var flatListener: ListenerRegistration? = null

    var flat by mutableStateOf<Flat?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val currentUserId: String?
        get() = auth.currentUser?.uid

    // ViewModel żyje dłużej niż sesja, więc po zmianie konta trzeba wczytać mieszkanie od nowa.
    // Bez tego następny użytkownik zobaczyłby mieszkanie i kod poprzedniego.
    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        observeFlat(firebaseAuth.currentUser?.uid)
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun createFlat(name: String) {
        val userId = currentUserId ?: return
        if (name.isBlank()) {
            errorMessage = "Podaj nazwę mieszkania"
            return
        }
        startSaving()
        repository.createFlat(
            name = name.trim(),
            userId = userId,
            userName = currentUserName(),
            onSuccess = { isSaving = false },
            onError = { showError("Nie udało się utworzyć mieszkania, sprawdź połączenie z internetem") },
        )
    }

    fun joinFlat(codeText: String) {
        val userId = currentUserId ?: return
        val code = normalizeInviteCode(codeText)
        if (!isValidInviteCode(code)) {
            errorMessage = "Nieprawidłowy kod zaproszenia"
            return
        }
        startSaving()
        repository.joinFlat(
            code = code,
            userId = userId,
            userName = currentUserName(),
            onSuccess = { isSaving = false },
            onCodeNotFound = { showError("Nie ma mieszkania z tym kodem") },
            onError = { showError("Nie udało się dołączyć, sprawdź połączenie z internetem") },
        )
    }

    fun leaveFlat() {
        val userId = currentUserId ?: return
        val currentFlat = flat ?: return
        startSaving()
        repository.leaveFlat(
            flat = currentFlat,
            userId = userId,
            onSuccess = { isSaving = false },
            onError = { showError("Nie udało się opuścić mieszkania, sprawdź połączenie z internetem") },
        )
    }

    fun clearError() {
        errorMessage = null
    }

    fun showScannerError() {
        errorMessage = "Nie udało się uruchomić skanera, wpisz kod ręcznie"
    }

    private fun currentUserName(): String {
        val user = auth.currentUser
        return user?.displayName?.takeIf { it.isNotBlank() } ?: user?.email ?: "Współlokator"
    }

    private fun startSaving() {
        isSaving = true
        errorMessage = null
    }

    private fun showError(message: String) {
        isSaving = false
        errorMessage = message
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
