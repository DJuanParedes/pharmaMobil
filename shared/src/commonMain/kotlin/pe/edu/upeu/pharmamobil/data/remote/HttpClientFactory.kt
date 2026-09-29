package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun crearHttpClientComun(
    engine: HttpClientEngine,
    baseUrl: String,
    requestTimeoutMillis: Long = 15_000,
    connectTimeoutMillis: Long = 10_000,
    socketTimeoutMillis: Long = 15_000,
): HttpClient = HttpClient(engine) {
    expectSuccess = true
    defaultRequest {
        url(baseUrl)
        contentType(ContentType.Application.Json)
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                isLenient = true
            },
        )
    }
    install(HttpTimeout) {
        this.requestTimeoutMillis = requestTimeoutMillis
        this.connectTimeoutMillis = connectTimeoutMillis
        this.socketTimeoutMillis = socketTimeoutMillis
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                println("Ktor: $message")
            }
        }
        level = LogLevel.HEADERS
        sanitizeHeader { header -> header.equals("Authorization", ignoreCase = true) }
    }
}

expect fun crearHttpClient(): HttpClient
