package com.collageMaker.features.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.collageMaker.features.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

@Serializable
object HomeScreenRoute

@Composable
fun AppRoot(
    modifier: Modifier = Modifier,
    onSplashScreenRemoved: () -> Unit,
) {
    val navController = rememberNavController()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SplashRoute
        ) {

            composable<SplashRoute> {
                onSplashScreenRemoved()
                navController.navigateToHomeScreen()
            }

            composable<HomeScreenRoute> {
                HomeScreen(
                    modifier = modifier.padding(innerPadding),
                )
            }

        }
    }
}

fun NavController.navigateToHomeScreen(){
    this.navigate(HomeScreenRoute)
}