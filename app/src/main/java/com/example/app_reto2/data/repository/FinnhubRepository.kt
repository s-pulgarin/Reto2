package com.example.app_reto2.data.repository

import com.example.app_reto2.data.model.ForexRate
import com.example.app_reto2.util.Resource

interface FinnhubRepository {

    /** Tasas de [base] contra todas las divisas disponibles (ej. USD -> EUR, GBP, JPY...). */
    suspend fun getRates(base: String = "USD"): Resource<Map<String, Double>>

    /** Precio de un par concreto (ej. base = "USD", quote = "EUR"). */
    suspend fun getRate(base: String, quote: String): Resource<ForexRate>
}