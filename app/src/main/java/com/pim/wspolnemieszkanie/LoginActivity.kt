package com.pim.wspolnemieszkanie

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.pim.wspolnemieszkanie.ui.screens.LoginScreen
import com.pim.wspolnemieszkanie.ui.theme.WspolneMieszkanieTheme
import com.pim.wspolnemieszkanie.viewmodel.AuthViewModel

class LoginActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (authViewModel.isLoggedIn) {
            openMain()
            return
        }
        enableEdgeToEdge()
        setContent {
            WspolneMieszkanieTheme {
                LaunchedEffect(authViewModel.isLoggedIn) {
                    if (authViewModel.isLoggedIn) openMain()
                }
                LoginScreen(authViewModel)
            }
        }
    }

    // finish(), żeby przycisk "wstecz" w MainActivity nie wracał do logowania.
    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
