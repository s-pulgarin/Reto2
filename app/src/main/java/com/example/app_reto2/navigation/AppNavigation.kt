package com.example.app_reto2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.app_reto2.data.model.User
import com.example.app_reto2.data.model.UserRole
import com.example.app_reto2.data.repository.AuthRepositoryImpl
import com.example.app_reto2.data.repository.FinnhubRepositoryImpl
import com.example.app_reto2.data.repository.KycRepositoryImpl
import com.example.app_reto2.data.repository.TransactionRepositoryImpl
import com.example.app_reto2.data.repository.UserRepositoryImpl
import com.example.app_reto2.ui.admin.AdminDashboardScreen
import com.example.app_reto2.ui.admin.AdminDashboardViewModel
import com.example.app_reto2.ui.admin.AdminDashboardViewModelFactory
import com.example.app_reto2.ui.auth.AuthState
import com.example.app_reto2.ui.auth.AuthViewModel
import com.example.app_reto2.ui.auth.AuthViewModelFactory
import com.example.app_reto2.ui.auth.screens.LoginScreen
import com.example.app_reto2.ui.auth.screens.RegisterScreen
import com.example.app_reto2.ui.kyc.KycScreen
import com.example.app_reto2.ui.kyc.KycViewModel
import com.example.app_reto2.ui.kyc.KycViewModelFactory
import com.example.app_reto2.ui.user.UserDashboardScreen
import com.example.app_reto2.ui.user.UserDashboardViewModel
import com.example.app_reto2.ui.user.UserDashboardViewModelFactory

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val authRepository = remember { AuthRepositoryImpl() }
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository)
    )
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    // Al cerrar sesión: vuelve a Login y destruye TODO el backstack
    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedOut) {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
            authViewModel.resetState()
        }
    }

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
        composable(Screen.UserDashboard.route) {
            val userDashboardViewModel: UserDashboardViewModel = viewModel(
                factory = remember {
                    UserDashboardViewModelFactory(
                        finnhubRepository = FinnhubRepositoryImpl(),
                        transactionRepository = TransactionRepositoryImpl(),
                        userRepository = UserRepositoryImpl()
                    )
                }
            )
            UserDashboardScreen(
                viewModel = userDashboardViewModel,
                onNavigateToKyc = { navController.navigate(Screen.Kyc.route) },
                onLogout = authViewModel::logout
            )
        }
        composable(Screen.Kyc.route) {
            val kycViewModel: KycViewModel = viewModel(
                factory = remember {
                    KycViewModelFactory(
                        kycRepository = KycRepositoryImpl(),
                        userRepository = UserRepositoryImpl()
                    )
                }
            )
            KycScreen(
                viewModel = kycViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AdminDashboard.route) {
            val adminDashboardViewModel: AdminDashboardViewModel = viewModel(
                factory = remember {
                    AdminDashboardViewModelFactory(
                        finnhubRepository = FinnhubRepositoryImpl(),
                        transactionRepository = TransactionRepositoryImpl()
                    )
                }
            )
            AdminDashboardScreen(
                viewModel = adminDashboardViewModel,
                onLogout = authViewModel::logout
            )
        }
    }
}