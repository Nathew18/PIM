package com.pim.wspolnemieszkanie.ui.screens

import androidx.compose.runtime.Composable

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
fun FlatScreen() = PlaceholderScreen("Mieszkanie", "Tu będą współlokatorzy i kod zaproszenia")
