package com.example.app_reto2.data.repository

import com.example.app_reto2.BuildConfig
import com.example.app_reto2.data.network.FinnhubSocketException
import com.example.app_reto2.data.network.FinnhubWebSocket
import com.example.app_reto2.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException

class FinnhubRepositoryImpl(
    private val webSocket: FinnhubWebSocket = FinnhubWebSocket()
) : FinnhubRepository {

    override fun observePrice(symbol: String): Flow<Resource<Double>> {
        if (BuildConfig.FINNHUB_API_KEY.isBlank()) {
            return flowOf(Resource.Error("Falta FINNHUB_API_KEY en local.properties."))
        }
        return webSocket.observePrice(symbol)
            .conflate()
            .map<Double, Resource<Double>> { Resource.Success(it) }
            .catch { emit(Resource.Error(it.toFriendlyMessage(), it)) }
    }

    private fun Throwable.toFriendlyMessage(): String = when {
        this is FinnhubSocketException -> when (httpCode) {
            401, 403 -> "API Key de Finnhub inválida o sin acceso al WebSocket."
            429 -> "Límite de conexiones alcanzado. Intenta en un minuto."
            else -> "Se perdió la conexión de precios. Toca Reconectar."
        }
        this is IOException -> "Sin conexión. Revisa tu internet."
        else -> "Error en la conexión de precios."
    }
}