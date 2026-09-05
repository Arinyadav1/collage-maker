package com.example.travenorcustomer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.travenorcustomer.features.details.DetailsScreen
import com.example.travenorcustomer.features.navigation.AppRoot
import com.example.travenorcustomer.ui.theme.TravenorCustomerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var shouldShowSplashScreen = true
        installSplashScreen().setKeepOnScreenCondition { shouldShowSplashScreen }

        enableEdgeToEdge()
        setContent {
            TravenorCustomerTheme {
                AppRoot(
                    onSplashScreenRemoved = {
                        shouldShowSplashScreen = false
                    }
                )
            }
        }
    }
}
