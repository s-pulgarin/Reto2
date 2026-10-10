package com.example.app_reto2.data.network

import com.example.app_reto2.data.model.ForexRatesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface FinnhubService {

    @GET("forex/rates")
    suspend fun getForexRates(@Query("base") base: String): ForexRatesResponse
}