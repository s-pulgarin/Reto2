package com.example.app_reto2.data.repository

import com.example.app_reto2.data.model.User
import com.example.app_reto2.util.Resource
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /** Guarda la URL del documento KYC en el perfil del usuario con sesión activa. */
    suspend fun updateKycDocumentUrl(url: String): Resource<Unit>

    /** Emite el perfil del usuario (balance y portfolio) cada vez que cambia en Firestore. */
    fun observeUser(uid: String): Flow<User>
}