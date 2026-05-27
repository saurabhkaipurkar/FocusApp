package com.saurabh.skipad.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.saurabh.skipad.route.ScreenRoute
import com.saurabh.skipad.screens.DashboardScreen
import com.saurabh.skipad.screens.MainScreen
import com.saurabh.skipad.screens.SplashScreen

@Composable
fun AppNavigation(
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit
) {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ScreenRoute.Splash.route) {
        composable(ScreenRoute.Splash.route) {
            SplashScreen {
                navController.navigate(ScreenRoute.Main.route) {
                    popUpTo(ScreenRoute.Splash.route) {
                        inclusive = true
                    }
                }
            }
        }
        composable(ScreenRoute.Main.route) {
            MainScreen(isDarkMode, onThemeToggle)
        }
    }
}