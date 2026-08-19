package com.saurabh.focusapp.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.saurabh.focusapp.BuildConfig
import com.saurabh.focusapp.route.ScreenRoute

/**
 * MainScreen / bottom nav — visual layer only.
 * NavHost destinations, navigate()/popUpTo/launchSingleTop/restoreState
 * wiring, and BottomNavItem data are unchanged.
 */

@Immutable
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

/**
 * Wrapper for the items list to ensure stability in the Compose compiler.
 * This prevents unnecessary recompositions of the bottom navigation bar.
 */
@Immutable
data class NavigationItems(
    val items: List<BottomNavItem>
)

private val NavItems = NavigationItems(
    items = listOf(
        BottomNavItem("Home", Icons.Default.Home, ScreenRoute.Dashboard.route),
        BottomNavItem("Analytics", Icons.Default.Analytics, ScreenRoute.Analytics.route),
        BottomNavItem("Settings", Icons.Default.Settings, ScreenRoute.Settings.route)
    )
)

private object NavPalette {
    val Accent = Color(0xFFE8A33D)
    val OnAccent = Color(0xFF241A05)
}

@Composable
fun MainScreen(
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit
) {
    if (BuildConfig.DEBUG) {
        // Log to track MainScreen recompositions.
        // Optimization: Ideally, we should avoid recomposing MainScreen on theme toggle
        // by moving theme state higher or using a more targeted update mechanism.
        SideEffect { Log.d("MainScreen", "MainScreen recomposed (isDarkMode: $isDarkMode)") }
    }

    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            // Optimization: Pass a stable wrapper for navigation items.
            CustomBottomNavigation(navController, NavItems)
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
    navItems: NavigationItems,
    modifier: Modifier = Modifier
) {
    // Collect the current backstack entry as state. This is the primary trigger for 
    // bottom navigation updates.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val dividerColor = remember(outlineColor) { outlineColor.copy(alpha = 0.6f) }

    // Optimization: Create a stable navigation lambda once and reuse it.
    // This prevents recreating lambdas inside the loop and ensures items skip recomposition.
    val onNavigate = remember(navController) {
        { route: String ->
            if (currentDestination?.route != route) {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(84.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(
                thickness = 1.dp,
                color = dividerColor
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.items.forEach { item ->
                    // Optimization: Use derived state or keep simple boolean checks.
                    // Since it's a small list, the direct check is efficient.
                    val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CustomBottomNavItem(
                            item = item,
                            isSelected = isSelected,
                            onNavigate = onNavigate
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual bottom navigation item.
 * Optimized to skip recomposition if selection state doesn't change.
 */
@Composable
fun CustomBottomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (BuildConfig.DEBUG) {
        // Optimization check: This should only log when selection state actually changes
        // or the theme changes.
        SideEffect { Log.d("CustomBottomNavItem", "Item ${item.label} recomposed. Selected: $isSelected") }
    }

    val neutralColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    // Optimization: Cache derived colors to avoid re-calculation/re-reading on every recomposition.
    val background = remember(isSelected) {
        if (isSelected) NavPalette.Accent else Color.Transparent
    }
    val contentColor = remember(isSelected, neutralColor) {
        if (isSelected) NavPalette.OnAccent else neutralColor
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            // Use item.route with the hoisted navigation lambda.
            .clickable { onNavigate(item.route) }
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
                modifier = Modifier.size(22.dp)
            )
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Text(
                    text = item.label,
                    color = contentColor,
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}
