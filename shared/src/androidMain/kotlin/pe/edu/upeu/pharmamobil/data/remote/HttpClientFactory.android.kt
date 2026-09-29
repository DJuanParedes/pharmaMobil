package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun crearHttpClient(): HttpClient = crearHttpClientComun(
    engine = CIO.create(),
    baseUrl = "http://10.0.2.2:8080/api/v1/",
)
