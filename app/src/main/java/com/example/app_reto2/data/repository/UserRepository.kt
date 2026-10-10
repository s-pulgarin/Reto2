package com.example.app_reto2.data.repository

import com.example.app_reto2.util.Resource

interface UserRepository {
    /** Guarda la URL del documento KYC en el perfil del usuario con sesión activa. */
    suspend fun updateKycDocumentUrl(url: String): Resource<Unit>
}