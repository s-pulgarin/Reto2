package com.example.app_reto2.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object UserDashboard : Screen("user_dashboard")
    data object AdminDashboard : Screen("admin_dashboard")
}