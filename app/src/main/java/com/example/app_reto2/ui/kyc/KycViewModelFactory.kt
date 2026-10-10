package com.example.app_reto2.ui.kyc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.app_reto2.data.repository.KycRepository
import com.example.app_reto2.data.repository.UserRepository

class KycViewModelFactory(
    private val kycRepository: KycRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(KycViewModel::class.java)) {
            "ViewModel desconocido: ${modelClass.name}"
        }
        return KycViewModel(kycRepository, userRepository) as T
    }
}