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
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.http.Url
import io.ktor.http.encodedPath
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.EstadoSesion

internal fun crearHttpClientComun(
    engine: HttpClientEngine,
    baseUrl: String,
    requestTimeoutMillis: Long = 15_000,
    connectTimeoutMillis: Long = 10_000,
    socketTimeoutMillis: Long = 15_000,
    almacen: AlmacenSeguro? = null,
    estado: EstadoSesion? = null,
    esDepuracion: Boolean = false,
    registro: Logger = object : Logger {
        override fun log(message: String) { println("Ktor: $message") }
    },
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
                encodeDefaults = true
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
    if (almacen != null) {
        val origen = Url(baseUrl)
        install(Auth) {
            bearer {
                loadTokens { almacen.leerToken()?.let { BearerTokens(it, "") } }
                sendWithoutRequest { request ->
                    request.url.host == origen.host && request.url.port == origen.port &&
                        request.url.protocol == origen.protocol &&
                        !request.url.encodedPath.endsWith("/auth/login")
                }
            }
        }
        HttpResponseValidator {
            handleResponseExceptionWithRequest { cause, request ->
                if ((cause as? ResponseException)?.response?.status?.value == 401 &&
                    !request.url.encodedPath.endsWith("/auth/login")) {
                    almacen.borrarToken()
                    (cause as ResponseException).response.call.client.authProvider<BearerAuthProvider>()?.clearToken()
                    estado?.expirar()
                }
            }
        }
    }
    install(Logging) {
        logger = registro
        level = if (esDepuracion) LogLevel.HEADERS else LogLevel.NONE
        sanitizeHeader { header -> header.equals("Authorization", ignoreCase = true) }
    }
}

expect fun crearHttpClient(almacen: AlmacenSeguro, estado: EstadoSesion): HttpClient
