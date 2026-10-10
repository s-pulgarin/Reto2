package com.example.app_reto2.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.app_reto2.data.repository.FinnhubRepository
import com.example.app_reto2.data.repository.TransactionRepository

class UserDashboardViewModelFactory(
    private val finnhubRepository: FinnhubRepository,
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(UserDashboardViewModel::class.java)) {
            "ViewModel desconocido: ${modelClass.name}"
        }
        return UserDashboardViewModel(finnhubRepository, transactionRepository) as T
    }
}