package com.example.app_reto2.data.network

import okhttp3.Interceptor
import okhttp3.Response

class FinnhubAuthInterceptor(private val apiKey: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(TOKEN_HEADER, apiKey)
            .build()
        return chain.proceed(request)
    }

    companion object {
        const val TOKEN_HEADER = "X-Finnhub-Token"
    }
}