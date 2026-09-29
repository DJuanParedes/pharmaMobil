package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioRest
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProductoApiTest {
    @Test
    fun ejecutaLosCincoEndpointsDelCrud() = runTest {
        val solicitudes = mutableListOf<Pair<HttpMethod, String>>()
        val engine = MockEngine { request ->
            solicitudes += request.method to request.url.encodedPath
            when {
                request.method == HttpMethod.Get && request.url.encodedPath.endsWith("/productos") ->
                    respond(paginaJson, headers = jsonHeaders)

                request.method == HttpMethod.Get -> respond(productoJson(7, "Naproxeno"), headers = jsonHeaders)
                request.method == HttpMethod.Post ->
                    respond(productoJson(8, "Cetirizina"), HttpStatusCode.Created, jsonHeaders)

                request.method == HttpMethod.Put -> respond(productoJson(8, "Cetirizina Forte"), headers = jsonHeaders)
                request.method == HttpMethod.Delete -> respond("", HttpStatusCode.NoContent)
                else -> error("Solicitud no esperada: ${request.method} ${request.url}")
            }
        }
        val client = crearHttpClientComun(engine, "https://pharma.test/api/v1/")
        val api = ProductoApi(client)
        val request = ProductoRequestDto("Cetirizina", 9.90, 15, true, 1)

        assertEquals(1, api.listar().contenido.size)
        assertEquals(7, api.obtener(7).id)
        assertEquals(8, api.crear(request).id)
        assertEquals("Cetirizina Forte", api.actualizar(8, request).nombre)
        api.eliminar(8)

        assertEquals(
            listOf(HttpMethod.Get, HttpMethod.Get, HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete),
            solicitudes.map { it.first },
        )
        assertTrue(solicitudes.all { it.second.startsWith("/api/v1/productos") })
        client.close()
    }

    @Test
    fun traduceRespuesta400AErroresPorCampo() = runTest {
        val engine = MockEngine {
            respondError(
                HttpStatusCode.BadRequest,
                """{"message":"Datos inválidos","validationErrors":{"nombre":"El nombre ya existe"}}""",
                jsonHeaders,
            )
        }
        val client = crearHttpClientComun(engine, "https://pharma.test/api/v1/")
        val repository = ProductoRepositorioRest(ProductoApi(client), categoriaPorDefecto = 1)

        val exception = assertFailsWith<ErrorApiException> {
            repository.registrar("Paracetamol", 8.50, 10)
        }
        val error = assertIs<ErrorApi.Validacion>(exception.error)
        assertEquals("El nombre ya existe", error.porCampo["nombre"])
        client.close()
    }

    private fun productoJson(id: Long, nombre: String) =
        """{"id":$id,"nombre":"$nombre","precio":9.9,"stock":15,"estado":true,"categoriaId":1}"""

    private val paginaJson =
        """{"contenido":[${productoJson(7, "Naproxeno")}],"pagina":0,"tamanio":20,"totalElementos":1,"totalPaginas":1,"ultima":true}"""

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
}
