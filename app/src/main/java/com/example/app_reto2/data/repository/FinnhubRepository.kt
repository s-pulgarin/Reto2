package com.example.app_reto2.data.repository

import com.example.app_reto2.util.Resource
import kotlinx.coroutines.flow.Flow

interface FinnhubRepository {
    /** Flujo con el último precio del [symbol] en tiempo real. */
    fun observePrice(symbol: String): Flow<Resource<Double>>
}