package com.saurabh.skipad.route

sealed class ScreenRoute(val route: String) {
    object Splash : ScreenRoute("splash")
    object Main : ScreenRoute("Main")
    object Dashboard : ScreenRoute("dashboard")
    object Settings : ScreenRoute("settings")
    object Analytics : ScreenRoute("analytics")
}