package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.EstadoSesion
import android.content.pm.ApplicationInfo
import org.koin.core.context.GlobalContext
import android.content.Context

actual fun crearHttpClient(almacen: AlmacenSeguro, estado: EstadoSesion): HttpClient = crearHttpClientComun(
    engine = CIO.create(),
    baseUrl = "http://10.0.2.2:8080/api/v1/",
    almacen = almacen,
    estado = estado,
    esDepuracion = GlobalContext.get().get<Context>().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
)
