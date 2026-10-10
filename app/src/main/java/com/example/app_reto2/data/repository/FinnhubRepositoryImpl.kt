package com.example.app_reto2.data.repository

import com.example.app_reto2.BuildConfig
import com.example.app_reto2.data.model.ForexRate
import com.example.app_reto2.data.network.FinnhubClient
import com.example.app_reto2.data.network.FinnhubService
import com.example.app_reto2.util.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

class FinnhubRepositoryImpl(
    private val service: FinnhubService = FinnhubClient.service
) : FinnhubRepository {

    override suspend fun getRates(base: String): Resource<Map<String, Double>> =
        safeApiCall {
            service.getForexRates(base.trim().uppercase()).quote
        }

    override suspend fun getRate(base: String, quote: String): Resource<ForexRate> =
        safeApiCall {
            val from = base.trim().uppercase()
            val to = quote.trim().uppercase()
            val rate = service.getForexRates(from).quote[to]
                ?: throw NoSuchElementException("El par $from/$to no está disponible.")
            ForexRate(base = from, quote = to, rate = rate)
        }

    private suspend fun <T> safeApiCall(block: suspend () -> T): Resource<T> {
        if (BuildConfig.FINNHUB_API_KEY.isBlank()) {
            return Resource.Error("Falta FINNHUB_API_KEY en local.properties.")
        }
        return try {
            Resource.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Resource.Error(e.toFriendlyMessage(), e)
        } catch (e: IOException) {
            Resource.Error("Sin conexión. Revisa tu internet.", e)
        } catch (e: SerializationException) {
            Resource.Error("Respuesta inválida del servidor.", e)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Ocurrió un error inesperado.", e)
        }
    }

    private fun HttpException.toFriendlyMessage(): String = when (code()) {
        401, 403 -> "API Key de Finnhub inválida o sin permisos."
        429 -> "Límite de peticiones alcanzado. Intenta en un minuto."
        else -> "Error del servidor (${code()})."
    }
}