package com.pim.wspolnemieszkanie.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExpensesScreen() = PlaceholderScreen("Wydatki", "Tu będzie lista wydatków i saldo")

@Composable
fun ShoppingScreen() = PlaceholderScreen("Zakupy", "Tu będzie wspólna lista zakupów")

@Composable
fun ChoresScreen() = PlaceholderScreen("Obowiązki", "Tu będzie grafik obowiązków")

@Composable
fun FlatScreen(onLogout: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        PlaceholderScreen("Mieszkanie", "Tu będą współlokatorzy i kod zaproszenia")
        Button(
            onClick = onLogout,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
        ) {
            Text("Wyloguj się")
        }
    }
}
