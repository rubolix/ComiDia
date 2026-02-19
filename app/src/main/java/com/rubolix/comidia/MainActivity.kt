package com.rubolix.comidia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rubolix.comidia.ui.ComiDiaAppUI
import com.rubolix.comidia.ui.theme.ComiDiaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComiDiaTheme {
                ComiDiaAppUI()
            }
        }
    }
}
