package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.EstadoSesion
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual fun crearHttpClient(almacen: AlmacenSeguro, estado: EstadoSesion): HttpClient = crearHttpClientComun(
    engine = Darwin.create(),
    baseUrl = "http://localhost:8080/api/v1/",
    almacen = almacen,
    estado = estado,
    esDepuracion = kotlin.native.Platform.isDebugBinary,
)
