package com.example.app_reto2.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        when (BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onError("No hay huellas registradas. Configúralas en los ajustes del dispositivo.")
                return
            }
            else -> {
                onError("La autenticación biométrica no está disponible en este dispositivo.")
                return
            }
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val message = when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED -> "Autenticación cancelada."
                    else -> errString.toString()
                }
                onError(message)
            }

            // onAuthenticationFailed (huella no reconocida) no se maneja: el prompt sigue abierto para reintentar.
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autorizar Transacción")
            .setSubtitle("Confirma con tu huella para ejecutar la operación")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()

        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
            .authenticate(promptInfo)
    }
}
