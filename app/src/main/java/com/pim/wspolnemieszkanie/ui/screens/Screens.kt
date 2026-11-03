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

// Każdy ekran ma właściciela, zgodnie z podziałem tasków na GitHubie.
// Gdy ekran urośnie, przenieście go do osobnego pliku, np. ExpensesScreen.kt.

/** Osoba 2: wydatki i saldo (issues #8–#13). */
@Composable
fun ExpensesScreen() = PlaceholderScreen("Wydatki", "Tu będzie lista wydatków i saldo")

/** Osoba 3: wspólna lista zakupów (issues #14–#16). */
@Composable
fun ShoppingScreen() = PlaceholderScreen("Zakupy", "Tu będzie wspólna lista zakupów")

/** Osoba 3: grafik obowiązków (issues #17–#18). */
@Composable
fun ChoresScreen() = PlaceholderScreen("Obowiązki", "Tu będzie grafik obowiązków")

/** Osoba 1: mieszkanie, współlokatorzy, kod zaproszenia (issues #5–#6). */
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
