package com.collageMaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.collageMaker.features.navigation.AppRoot
import com.collageMaker.ui.theme.Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var shouldShowSplashScreen = true
        installSplashScreen().setKeepOnScreenCondition { shouldShowSplashScreen }
        enableEdgeToEdge()
        setContent {
            Theme {
                AppRoot(
                    onSplashScreenRemoved = {
                        shouldShowSplashScreen = false
                    }
                )
            }
        }
    }
}
