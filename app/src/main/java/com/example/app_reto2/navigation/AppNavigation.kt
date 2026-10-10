package com.example.app_reto2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.app_reto2.data.repository.AuthRepositoryImpl
import com.example.app_reto2.ui.auth.AuthViewModel
import com.example.app_reto2.ui.auth.AuthViewModelFactory
import com.example.app_reto2.ui.auth.screens.LoginScreen
import com.example.app_reto2.ui.auth.screens.RegisterScreen
import com.example.app_reto2.ui.home.HomeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val authRepository = remember { AuthRepositoryImpl() }
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository)
    )

    // Si ya hay sesión activa en Firebase, arranca directamente en Home
    val startDestination = remember {
        if (authRepository.currentUser != null) Screen.Home.route else Screen.Login.route
    }

    val goToHome: () -> Unit = {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Login.route) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { goToHome() },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = { goToHome() },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen()
        }
    }
}