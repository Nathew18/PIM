package com.pim.wspolnemieszkanie.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pim.wspolnemieszkanie.ui.screens.ChoresScreen
import com.pim.wspolnemieszkanie.ui.screens.ExpensesScreen
import com.pim.wspolnemieszkanie.ui.screens.FlatScreen
import com.pim.wspolnemieszkanie.ui.screens.ShoppingScreen
import com.pim.wspolnemieszkanie.viewmodel.FlatViewModel

enum class Tab(val label: String, val icon: String) {
    EXPENSES("Wydatki", "💰"),
    SHOPPING("Zakupy", "🛒"),
    CHORES("Obowiązki", "🧹"),
    FLAT("Mieszkanie", "🏠"),
}

@Composable
fun MainScreen(flatViewModel: FlatViewModel, onLogout: () -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.EXPENSES) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.icon) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                Tab.EXPENSES -> ExpensesScreen()
                Tab.SHOPPING -> ShoppingScreen()
                Tab.CHORES -> ChoresScreen()
                Tab.FLAT -> FlatScreen(flatViewModel, onLogout)
            }
        }
    }
}
