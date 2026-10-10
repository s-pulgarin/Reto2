package com.example.app_reto2.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FinnhubSocketMessage(
    val type: String = "",
    val data: List<FinnhubTrade> = emptyList()
)

@Serializable
data class FinnhubTrade(
    @SerialName("p") val price: Double = 0.0,
    @SerialName("s") val symbol: String = "",
    @SerialName("t") val timestamp: Long = 0L,
    @SerialName("v") val volume: Double = 0.0
)