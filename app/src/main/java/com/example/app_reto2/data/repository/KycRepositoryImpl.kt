package com.example.app_reto2.data.repository

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.app_reto2.BuildConfig
import com.example.app_reto2.util.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class KycRepositoryImpl : KycRepository {

    override suspend fun uploadKycDocument(
        imageUri: Uri,
        uploadPreset: String
    ): Resource<String> {
        if (BuildConfig.CLOUDINARY_CLOUD_NAME.isBlank()) {
            return Resource.Error("Falta CLOUDINARY_CLOUD_NAME en local.properties.")
        }
        if (uploadPreset.isBlank()) {
            return Resource.Error("Falta CLOUDINARY_UPLOAD_PRESET en local.properties.")
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                val requestId = MediaManager.get()
                    .upload(imageUri)
                    .unsigned(uploadPreset)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String?) = Unit

                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) = Unit

                        override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                            val url = resultData?.get("secure_url") as? String
                            if (continuation.isActive) {
                                continuation.resume(
                                    if (url != null) {
                                        Resource.Success(url)
                                    } else {
                                        Resource.Error("Cloudinary no devolvió la URL de la imagen.")
                                    }
                                )
                            }
                        }

                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            if (continuation.isActive) {
                                continuation.resume(
                                    Resource.Error(error?.description ?: "Error al subir la imagen.")
                                )
                            }
                        }

                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                            if (continuation.isActive) {
                                continuation.resume(
                                    Resource.Error(error?.description ?: "La subida fue reprogramada. Intenta de nuevo.")
                                )
                            }
                        }
                    })
                    .dispatch()

                continuation.invokeOnCancellation {
                    MediaManager.get().cancelRequest(requestId)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Ocurrió un error inesperado.", e)
        }
    }
}