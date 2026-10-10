package com.example.app_reto2.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ForexRatesResponse(
    val base: String = "",
    val quote: Map<String, Double> = emptyMap()
)