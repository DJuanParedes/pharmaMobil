package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.EstadoSesion
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import org.koin.core.context.GlobalContext
import android.content.Context

/** Solo el emulador alcanza el equipo de desarrollo en 10.0.2.2. */
internal const val URL_EMULADOR = "http://10.0.2.2:8080/api/v1/"

/**
 * Sesión 14: la dirección del servicio llega desde el manifiesto (meta-data «pharma.api.url»),
 * que la toma de la propiedad Gradle pharmaApiUrl. Un teléfono físico necesita la IP de la
 * máquina en la red local, por ejemplo http://192.168.1.40:8080/api/v1/.
 */
private fun urlDelServicio(contexto: Context): String = runCatching {
    contexto.packageManager.getApplicationInfo(contexto.packageName, PackageManager.GET_META_DATA)
        .metaData?.getString("pharma.api.url")
}.getOrNull()?.takeIf { it.isNotBlank() }?.let { if (it.endsWith("/")) it else "$it/" } ?: URL_EMULADOR

actual fun crearHttpClient(almacen: AlmacenSeguro, estado: EstadoSesion): HttpClient {
    val contexto = GlobalContext.get().get<Context>()
    return crearHttpClientComun(
        engine = CIO.create(),
        baseUrl = urlDelServicio(contexto),
        almacen = almacen,
        estado = estado,
        esDepuracion = contexto.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
    )
}
