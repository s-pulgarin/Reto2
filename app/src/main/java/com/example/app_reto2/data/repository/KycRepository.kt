package com.example.app_reto2.data.repository

import android.net.Uri
import com.example.app_reto2.util.Resource

interface KycRepository {
    /** Sube la imagen a Cloudinary (unsigned) y devuelve su `secure_url`. */
    suspend fun uploadKycDocument(imageUri: Uri, uploadPreset: String): Resource<String>
}