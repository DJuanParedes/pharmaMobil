package pe.edu.upeu.pharmamobil.data.sync

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClientComun
import pe.edu.upeu.pharmamobil.data.repository.EnvioRepositorioRest
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioRest
import pe.edu.upeu.pharmamobil.data.repository.driverEnMemoria
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarUseCase.Companion.MAX_INTENTOS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Sesión 13: base en memoria + motor de red simulado, sin dispositivo. */
class SincronizacionTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private fun productoJson(id: Long, nombre: String) =
        """{"id":$id,"nombre":"$nombre","precio":4.5,"stock":20,"estado":true,"categoriaId":1}"""
    private fun paginaJson(vararg productos: String) =
        """{"contenido":[${productos.joinToString(",")}],"pagina":0,"tamanio":20,"totalElementos":${productos.size},"totalPaginas":1,"ultima":true}"""

    private class Caso(
        val local: ProductoLocal,
        val caso: SincronizarUseCase,
        val esperas: MutableList<Long>,
        val solicitudes: MutableList<HttpMethod>,
        val avisos: MutableList<Int>,
    )

    private fun armarCasoDeUso(
        responder: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): Caso {
        val solicitudes = mutableListOf<HttpMethod>()
        val motor = MockEngine { request -> solicitudes += request.method; responder(request) }
        val client = crearHttpClientComun(motor, "https://pharma.test/api/v1/")
        val local = ProductoLocal(PharmaDb(driverEnMemoria()))
        val esperas = mutableListOf<Long>()
        val avisos = mutableListOf<Int>()
        val caso = SincronizarUseCase(
            local = local,
            remoto = EnvioRepositorioRest(ProductoRepositorioRest(ProductoApi(client), 1)),
            notificador = object : Notificador { override fun avisarFilasFallidas(cantidad: Int) { avisos += cantidad } },
            reloj = { 1_700_000_000_000L },
            esperar = { esperas += it },
        )
        return Caso(local, caso, esperas, solicitudes, avisos)
    }

    private fun Caso.insertarPendiente(nombre: String = "Ibuprofeno"): Producto =
        local.guardarPendiente(Producto(-1, nombre, 4.5, 20))

    @Test
    fun unFalloTransitorioSumaIntentoYNoMarcaFallida() = runTest {
        val c = armarCasoDeUso { respondError(HttpStatusCode.ServiceUnavailable) }
        val pendiente = c.insertarPendiente()

        val resumen = c.caso()

        val fila = c.local.pendientes(MAX_INTENTOS).single()
        assertEquals(pendiente.id, fila.id)
        assertEquals(1, fila.intentos)
        assertTrue(fila.producto.pendiente)
        assertTrue(fila.ultimoError!!.isNotBlank())
        assertEquals(1, c.local.contarPendientes(MAX_INTENTOS))
        assertTrue(c.local.fallidas(MAX_INTENTOS).isEmpty())
        assertEquals(ErrorApi.Servidor, resumen.errorDescarga)
    }

    @Test
    fun unFalloPermanenteMarcaLaFilaYNoLaReintenta() = runTest {
        val c = armarCasoDeUso { request ->
            if (request.method == HttpMethod.Post)
                respondError(HttpStatusCode.Conflict, """{"message":"Ya existe un producto con ese nombre"}""", jsonHeaders)
            else respond(paginaJson(), headers = jsonHeaders)
        }
        c.insertarPendiente("Duplicado")

        val primero = c.caso()
        c.solicitudes.clear()
        c.caso()

        assertEquals(1, primero.fallidas)
        val fallida = c.local.fallidas(MAX_INTENTOS).single()
        assertEquals("Ya existe un producto con ese nombre", fallida.ultimoError)
        assertTrue(fallida.producto.pendiente, "La fila sigue visible para el usuario")
        assertEquals(listOf(HttpMethod.Get), c.solicitudes, "No vuelve a enviar la fila fallida")
        assertEquals(listOf(1), c.avisos, "Se avisa una vez al usuario")
        assertTrue(c.esperas.isEmpty(), "Un fallo permanente no espera")
    }

    @Test
    fun laEsperaCreceEntreIntentosYTieneTope() = runTest {
        val c = armarCasoDeUso { respondError(HttpStatusCode.ServiceUnavailable) }
        c.insertarPendiente()

        repeat(MAX_INTENTOS) { c.caso() }

        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L), c.esperas)
        assertEquals(16_000L, SincronizarUseCase.espera(10), "La espera no pasa de 16 s")
    }

    @Test
    fun alAgotarLosIntentosLaFilaQuedaFallidaYVisible() = runTest {
        val c = armarCasoDeUso { respondError(HttpStatusCode.ServiceUnavailable) }
        c.insertarPendiente()

        repeat(MAX_INTENTOS) { c.caso() }
        c.solicitudes.clear()
        c.caso()

        assertTrue(c.local.pendientes(MAX_INTENTOS).isEmpty())
        assertEquals(1, c.local.fallidas(MAX_INTENTOS).size)
        assertEquals(1, c.local.listar().size, "Nada se pierde")
        assertEquals(listOf(HttpMethod.Get), c.solicitudes, "Ya no se reintenta sola")
    }

    @Test
    fun exitoSubeAntesDeBajarYRegistraLaHora() = runTest {
        val c = armarCasoDeUso { request ->
            if (request.method == HttpMethod.Post) respond(productoJson(31, "Ibuprofeno"), HttpStatusCode.Created, jsonHeaders)
            else respond(paginaJson(productoJson(31, "Ibuprofeno")), headers = jsonHeaders)
        }
        c.insertarPendiente()

        val resumen = c.caso()

        assertEquals(listOf(HttpMethod.Post, HttpMethod.Get), c.solicitudes)
        assertEquals(1, resumen.enviadas)
        assertEquals(0, resumen.pendientes)
        assertNull(resumen.errorDescarga)
        val unico = c.local.listar().single()
        assertEquals(31L, unico.id)
        assertTrue(!unico.pendiente)
    }

    @Test
    fun reintentarFallidasLasDevuelveALaCola() = runTest {
        var conflicto = true
        val c = armarCasoDeUso { request ->
            when {
                request.method == HttpMethod.Post && conflicto ->
                    respondError(HttpStatusCode.Conflict, """{"message":"Conflicto"}""", jsonHeaders)
                request.method == HttpMethod.Post -> respond(productoJson(40, "Ibuprofeno"), HttpStatusCode.Created, jsonHeaders)
                else -> respond(paginaJson(), headers = jsonHeaders)
            }
        }
        c.insertarPendiente()
        c.caso()
        assertEquals(1, c.local.fallidas(MAX_INTENTOS).size)

        conflicto = false
        c.local.reintentarFallidas(MAX_INTENTOS)
        val resumen = c.caso()

        assertEquals(1, resumen.enviadas)
        assertTrue(c.local.fallidas(MAX_INTENTOS).isEmpty())
    }
}
