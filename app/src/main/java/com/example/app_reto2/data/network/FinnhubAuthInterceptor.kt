package com.example.app_reto2.data.network

import okhttp3.Interceptor
import okhttp3.Response

class FinnhubAuthInterceptor(private val apiKey: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url.newBuilder()
            .addQueryParameter("token", apiKey)
            .build()
        return chain.proceed(original.newBuilder().url(url).build())
    }
}