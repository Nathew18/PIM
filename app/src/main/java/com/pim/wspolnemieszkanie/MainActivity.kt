package com.pim.wspolnemieszkanie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pim.wspolnemieszkanie.ui.MainScreen
import com.pim.wspolnemieszkanie.ui.theme.WspolneMieszkanieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WspolneMieszkanieTheme {
                MainScreen()
            }
        }
    }
}
