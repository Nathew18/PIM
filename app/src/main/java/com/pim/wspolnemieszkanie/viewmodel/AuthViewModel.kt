package com.pim.wspolnemieszkanie.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/** Logowanie i rejestracja przez Firebase Authentication (FR1). */
class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    var isLoggedIn by mutableStateOf(auth.currentUser != null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Firebase sam zgłasza każdą zmianę sesji: logowanie, wylogowanie, wygaśnięcie konta.
    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        isLoggedIn = firebaseAuth.currentUser != null
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isEmpty()) {
            errorMessage = "Podaj e-mail i hasło"
            return
        }
        runAuthTask(auth.signInWithEmailAndPassword(email.trim(), password))
    }

    fun register(email: String, password: String, repeatedPassword: String) {
        if (email.isBlank() || password.isEmpty()) {
            errorMessage = "Podaj e-mail i hasło"
            return
        }
        if (password != repeatedPassword) {
            errorMessage = "Hasła nie są takie same"
            return
        }
        runAuthTask(auth.createUserWithEmailAndPassword(email.trim(), password))
    }

    fun logout() {
        auth.signOut()
    }

    fun clearError() {
        errorMessage = null
    }

    /** Wspólna obsługa ładowania i błędów. Po sukcesie ekran zmieni authListener. */
    private fun runAuthTask(task: Task<AuthResult>) {
        isLoading = true
        errorMessage = null
        task.addOnCompleteListener { result ->
            isLoading = false
            if (!result.isSuccessful) {
                errorMessage = polishMessage(result.exception)
            }
        }
    }

    /** Komunikaty Firebase są po angielsku, a interfejs ma być po polsku. */
    private fun polishMessage(error: Exception?): String = when (error) {
        // Słabe hasło musi być przed złymi danymi, bo dziedziczy po tym wyjątku.
        is FirebaseAuthWeakPasswordException -> "Hasło musi mieć co najmniej 6 znaków"
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException -> "Nieprawidłowy e-mail lub hasło"
        is FirebaseAuthUserCollisionException -> "Konto z tym adresem już istnieje"
        is FirebaseNetworkException -> "Brak połączenia z internetem"
        is FirebaseTooManyRequestsException -> "Zbyt wiele prób, spróbuj ponownie później"
        else -> "Coś poszło nie tak, spróbuj ponownie"
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
    }
}
