package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun crearHttpClient(): HttpClient = crearHttpClientComun(
    engine = Darwin.create(),
    baseUrl = "http://localhost:8080/api/v1/",
)
