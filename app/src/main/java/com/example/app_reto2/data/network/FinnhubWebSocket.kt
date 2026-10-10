package com.example.app_reto2.data.network

import com.example.app_reto2.data.model.FinnhubSocketMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException

class FinnhubSocketException(
    val httpCode: Int?,
    cause: Throwable? = null
) : IOException(cause)

class FinnhubWebSocket(
    private val client: OkHttpClient = FinnhubClient.okHttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /** Emite el precio de cada nuevo trade del [symbol] (ej. "OANDA:EUR_USD"). */
    fun observePrice(symbol: String): Flow<Double> = callbackFlow {
        val request = Request.Builder().url(WS_URL).build()

        val socket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(message("subscribe", symbol))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val parsed = runCatching {
                    json.decodeFromString<FinnhubSocketMessage>(text)
                }.getOrNull() ?: return
                if (parsed.type != "trade") return
                parsed.data.lastOrNull { it.symbol == symbol }?.let { trySend(it.price) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
                close(FinnhubSocketException(httpCode = null))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(FinnhubSocketException(httpCode = response?.code, cause = t))
            }
        })

        awaitClose {
            socket.send(message("unsubscribe", symbol))
            socket.close(1000, null)
        }
    }

    private fun message(type: String, symbol: String) =
        """{"type":"$type","symbol":"$symbol"}"""

    private companion object {
        const val WS_URL = "wss://ws.finnhub.io"
    }
}