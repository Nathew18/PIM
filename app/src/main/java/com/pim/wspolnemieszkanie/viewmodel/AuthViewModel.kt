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
import com.google.firebase.auth.UserProfileChangeRequest

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    var isLoggedIn by mutableStateOf(auth.currentUser != null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Konto istnieje już przed zapisaniem imienia, a do tego czasu nie wpuszczamy dalej,
    // bo mieszkanie zapisałoby e-mail zamiast imienia.
    private var isRegistering = false

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        isLoggedIn = firebaseAuth.currentUser != null && !isRegistering
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

    fun register(name: String, email: String, password: String, repeatedPassword: String) {
        if (name.isBlank()) {
            errorMessage = "Podaj imię"
            return
        }
        if (email.isBlank() || password.isEmpty()) {
            errorMessage = "Podaj e-mail i hasło"
            return
        }
        if (password != repeatedPassword) {
            errorMessage = "Hasła nie są takie same"
            return
        }
        isRegistering = true
        val task = auth.createUserWithEmailAndPassword(email.trim(), password)
            .continueWithTask { created ->
                if (!created.isSuccessful) return@continueWithTask created
                val user = created.result.user ?: return@continueWithTask created
                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(name.trim())
                    .build()
                // Błąd zapisu imienia nie cofa rejestracji, wtedy w mieszkaniu pojawi się e-mail.
                user.updateProfile(profile).continueWithTask { created }
            }
        runAuthTask(task)
    }

    fun clearError() {
        errorMessage = null
    }

    private fun runAuthTask(task: Task<AuthResult>) {
        isLoading = true
        errorMessage = null
        task.addOnCompleteListener { result ->
            isLoading = false
            isRegistering = false
            isLoggedIn = auth.currentUser != null
            if (!result.isSuccessful) {
                errorMessage = polishMessage(result.exception)
            }
        }
    }

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
