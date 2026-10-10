package com.example.app_reto2.ui.kyc

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_reto2.BuildConfig
import com.example.app_reto2.data.repository.KycRepository
import com.example.app_reto2.data.repository.UserRepository
import com.example.app_reto2.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface KycState {
    data object Idle : KycState
    data object Loading : KycState
    data class Success(val documentUrl: String) : KycState
    data class Error(val message: String) : KycState
}

class KycViewModel(
    private val kycRepository: KycRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow<KycState>(KycState.Idle)
    val state: StateFlow<KycState> = _state.asStateFlow()

    fun uploadDocument(uri: Uri) {
        if (_state.value is KycState.Loading) return
        _state.value = KycState.Loading

        viewModelScope.launch {
            _state.value = when (
                val upload = kycRepository.uploadKycDocument(uri, BuildConfig.CLOUDINARY_UPLOAD_PRESET)
            ) {
                is Resource.Error -> KycState.Error(upload.message)
                is Resource.Success -> when (val save = userRepository.updateKycDocumentUrl(upload.data)) {
                    is Resource.Success -> KycState.Success(upload.data)
                    is Resource.Error -> KycState.Error(save.message)
                }
            }
        }
    }

    fun resetState() {
        _state.value = KycState.Idle
    }
}