package com.pim.wspolnemieszkanie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.pim.wspolnemieszkanie.ui.MainScreen
import com.pim.wspolnemieszkanie.ui.screens.LoginScreen
import com.pim.wspolnemieszkanie.ui.theme.WspolneMieszkanieTheme
import com.pim.wspolnemieszkanie.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WspolneMieszkanieTheme {
                if (authViewModel.isLoggedIn) {
                    MainScreen(onLogout = authViewModel::logout)
                } else {
                    LoginScreen(authViewModel)
                }
            }
        }
    }
}
