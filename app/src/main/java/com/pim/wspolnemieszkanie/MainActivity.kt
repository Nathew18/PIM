package com.pim.wspolnemieszkanie

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.google.firebase.auth.FirebaseAuth
import com.pim.wspolnemieszkanie.ui.MainScreen
import com.pim.wspolnemieszkanie.ui.theme.WspolneMieszkanieTheme
import com.pim.wspolnemieszkanie.viewmodel.ExpensesViewModel
import com.pim.wspolnemieszkanie.viewmodel.FlatViewModel
import com.pim.wspolnemieszkanie.viewmodel.ShoppingViewModel

class MainActivity : ComponentActivity() {
    private val flatViewModel: FlatViewModel by viewModels()
    private val expensesViewModel: ExpensesViewModel by viewModels()
    private val shoppingViewModel: ShoppingViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseAuth.getInstance().currentUser == null) {
            openLogin()
            return
        }
        enableEdgeToEdge()
        setContent {
            WspolneMieszkanieTheme {
                MainScreen(flatViewModel, expensesViewModel, shoppingViewModel = shoppingViewModel,onLogout = ::logout)
            }
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        openLogin()
    }

    // CLEAR_TASK czyści stos, żeby po wylogowaniu nie dało się wrócić "wstecz" do aplikacji.
    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
