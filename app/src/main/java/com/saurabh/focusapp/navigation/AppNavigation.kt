package com.saurabh.focusapp.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.saurabh.focusapp.route.ScreenRoute
import com.saurabh.focusapp.screens.MainScreen
import com.saurabh.focusapp.screens.PermissionScreen
import com.saurabh.focusapp.screens.SplashScreen

@Composable
fun AppNavigation(
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ScreenRoute.Splash.route) {
        composable(ScreenRoute.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    // Check if notification permission is already granted (Android 13+)
                    val isNotificationGranted =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }

                    // Decide destination based on permission status
                    val destination = if (isNotificationGranted) {
                        ScreenRoute.Main.route
                    } else {
                        ScreenRoute.Permission.route
                    }

                    navController.navigate(destination) {
                        popUpTo(ScreenRoute.Splash.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(ScreenRoute.Permission.route) {
            PermissionScreen {
                navController.navigate(ScreenRoute.Main.route) {
                    popUpTo(ScreenRoute.Permission.route) {
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
