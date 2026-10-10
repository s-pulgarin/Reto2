package com.example.app_reto2.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.app_reto2.data.repository.FinnhubRepository
import com.example.app_reto2.data.repository.TransactionRepository

class AdminDashboardViewModelFactory(
    private val finnhubRepository: FinnhubRepository,
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AdminDashboardViewModel::class.java)) {
            "ViewModel desconocido: ${modelClass.name}"
        }
        return AdminDashboardViewModel(finnhubRepository, transactionRepository) as T
    }
}