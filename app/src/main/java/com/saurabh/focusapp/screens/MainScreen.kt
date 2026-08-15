package com.saurabh.focusapp.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.saurabh.focusapp.route.ScreenRoute

/**
 * Optimization: Marked as @Immutable to inform the Compose compiler that 
 * the properties of this class will not change after construction. 
 * This enables the compiler to skip recompositions of composables using this 
 * class if the instance remains the same.
 */
@Immutable
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun MainScreen(
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit
) {
    // Performance Metric: Log recompositions to track if optimizations are working
    SideEffect {
        Log.d("MainScreen", "MainScreen recomposed (isDarkMode: $isDarkMode)")
    }

    val navController = rememberNavController()

    // Optimization: Remember the items list. Recreating the list on every 
    // recomposition (e.g., when isDarkMode changes) would prevent 
    // CustomBottomNavigation from skipping recomposition.
    val items = remember {
        listOf(
            BottomNavItem("Home", Icons.Default.Home, ScreenRoute.Dashboard.route),
            BottomNavItem("Analytics", Icons.Default.Analytics, ScreenRoute.Analytics.route),
            BottomNavItem("Settings", Icons.Default.Settings, ScreenRoute.Settings.route)
        )
    }

    Scaffold(
        bottomBar = { 
            // Optimization: Scaffold calls this lambda. By passing remembered 
            // stable items, we help CustomBottomNavigation skip unnecessary work.
            CustomBottomNavigation(navController, items) 
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = ScreenRoute.Dashboard.route,
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            composable(ScreenRoute.Dashboard.route) {
                DashboardScreen()
            }
            composable(ScreenRoute.Settings.route) {
                SettingsScreen(isDarkMode, onThemeToggle)
            }
            composable(ScreenRoute.Analytics.route) {
                AnalyticsScreen()
            }
        }
    }
}

@Composable
fun CustomBottomNavigation(
    navController: NavHostController,
    items: List<BottomNavItem>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Optimization: Pre-calculate the divider color based on the theme to 
    // avoid creating new Color objects and derivations inside the Row/loop.
    val outlineColor = MaterialTheme.colorScheme.outline
    val dividerColor = remember(outlineColor) { outlineColor.copy(alpha = 0.5f) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(80.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = dividerColor
            )
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected =
                        currentDestination?.hierarchy?.any { it.route == item.route } == true

                    /**
                     * Optimization: Remember the onClick lambda for each route. 
                     * If we pass a raw lambda to CustomBottomNavItem, it would be 
                     * considered "unstable" and trigger a recomposition of the 
                     * item every time the bottom bar recomposes, even if the 
                     * selection state didn't change.
                     */
                    val onClick = remember(item.route, navController) {
                        {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }

                    CustomBottomNavItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Optimization: Derived colors are remembered to avoid redundant 'copy' calls
    val primaryColor = MaterialTheme.colorScheme.primary
    val background = remember(isSelected, primaryColor) {
        if (isSelected) primaryColor.copy(alpha = 0.1f) else Color.Transparent
    }
    val contentColor = if (isSelected) primaryColor else Color.Gray

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            AnimatedVisibility(visible = isSelected) {
                Text(
                    text = item.label,
                    color = contentColor,
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
