package com.example.swachhbot.network

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Single entry point for talking to the SwachhBot backend.
 *
 * Usage:
 * ```
 * val client = BackendClient("http://10.0.2.2:8080/")
 * client.api.pushRobotState("swachhbot-01", state)
 * client.connectTelemetry { telemetry -> /* update UI */ }
 * ```
 */
class BackendClient(baseUrl: String) {

    private val gson = Gson()

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // keep WebSocket open
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val api: SwachhBotApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(SwachhBotApi::class.java)

    private val telemetryUrl: String =
        baseUrl.trimEnd('/').replaceFirst("http", "ws") + "/ws/telemetry"

    private var socket: WebSocket? = null

    /**
     * Opens the real-time telemetry stream. The listener is invoked on every
     * frame the server pushes (position, battery, status, progress).
     */
    fun connectTelemetry(listener: (TelemetryMessage) -> Unit) {
        closeTelemetry()
        val request = Request.Builder().url(telemetryUrl).build()
        socket = okHttp.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text == "pong") return
                runCatching { gson.fromJson(text, TelemetryMessage::class.java) }
                    .onSuccess(listener)
            }
        })
    }

    fun closeTelemetry() {
        socket?.close(1000, "client closing")
        socket = null
    }
}
