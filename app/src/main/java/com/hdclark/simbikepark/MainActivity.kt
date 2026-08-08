package com.hdclark.simbikepark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF176B45), secondary = Color(0xFFE66B2E),
                    tertiary = Color(0xFF1F78B4), background = Color(0xFFF7F2DE), surface = Color(0xFFFFFBEA)
                )
            ) {
                BikeParkScreen(viewModel<ParkViewModel>())
            }
        }
    }
}
