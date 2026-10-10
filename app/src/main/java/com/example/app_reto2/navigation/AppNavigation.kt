package com.example.app_reto2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.app_reto2.data.model.User
import com.example.app_reto2.data.model.UserRole
import com.example.app_reto2.data.repository.AuthRepositoryImpl
import com.example.app_reto2.ui.auth.AuthViewModel
import com.example.app_reto2.ui.auth.AuthViewModelFactory
import com.example.app_reto2.ui.auth.screens.LoginScreen
import com.example.app_reto2.ui.auth.screens.RegisterScreen
import com.example.app_reto2.ui.dashboard.AdminDashboardScreen
import com.example.app_reto2.ui.dashboard.UserDashboardScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val authRepository = remember { AuthRepositoryImpl() }
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository)
    )

    // Redirige según el rol. Con sesión activa, el ViewModel emite Success al iniciar
    // y Login navega automáticamente al dashboard correspondiente.
    val goToDashboard: (User) -> Unit = { user ->
        val destination = if (user.role == UserRole.ADMIN) {
            Screen.AdminDashboard.route
        } else {
            Screen.UserDashboard.route
        }
        navController.navigate(destination) {
            popUpTo(Screen.Login.route) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = goToDashboard,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = goToDashboard,
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Screen.UserDashboard.route) { UserDashboardScreen() }
        composable(Screen.AdminDashboard.route) { AdminDashboardScreen() }
    }
}