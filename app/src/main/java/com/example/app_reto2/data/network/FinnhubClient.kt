package com.example.app_reto2.data.network

import com.example.app_reto2.BuildConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object FinnhubClient {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(FinnhubAuthInterceptor(BuildConfig.FINNHUB_API_KEY))
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }
}