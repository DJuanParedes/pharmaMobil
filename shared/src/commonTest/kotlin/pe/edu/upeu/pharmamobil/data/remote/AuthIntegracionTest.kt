package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.engine.mock.*
import io.ktor.client.request.get
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import pe.edu.upeu.pharmamobil.data.local.*
import pe.edu.upeu.pharmamobil.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobil.domain.model.*

class AuthIntegracionTest {
    private class Almacen : AlmacenSeguro {
        var token: String? = null
        override fun guardarToken(token: String) { this.token = token }
        override fun leerToken() = token
        override fun borrarToken() { token = null }
    }
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private fun login(token: String, user: String = "admin") =
        """{"token":"$token","tipo":"Bearer","expiraEnSegundos":3600,"username":"$user","nombreCompleto":"Prueba","roles":["ADMIN"],"permisos":["CATALOGO_LEER","CATALOGO_ESCRIBIR"]}"""
    @Test fun loginNoEnviaTokenYSegundoLoginInvalidaCache() = runTest {
        val almacen = Almacen(); val estado = EstadoSesion(); var numeroLogin = 0
        val tokens = mutableListOf<String?>()
        val motor = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/auth/login")) {
                assertNull(request.headers[HttpHeaders.Authorization])
                numeroLogin++; respond(login("token-$numeroLogin"), HttpStatusCode.OK, jsonHeaders)
            } else {
                tokens += request.headers[HttpHeaders.Authorization]
                respond("{}", HttpStatusCode.OK, jsonHeaders)
            }
        }
        val client = crearHttpClientComun(motor, "https://pharma.test/api/v1/", almacen = almacen, estado = estado)
        try {
            val repo = AuthRepositorioRest(client, almacen, estado)
            repo.iniciarSesion(Credenciales("admin", "prueba")); client.get("productos")
            repo.iniciarSesion(Credenciales("admin", "prueba")); client.get("productos")
            assertEquals(listOf<String?>("Bearer token-1", "Bearer token-2"), tokens)
            repo.cerrarSesion(); assertNull(almacen.token); assertNull(estado.sesion.value)
            client.get("productos"); assertNull(tokens.last())
        } finally { client.close() }
    }
    @Test fun restauraSesionDesdeAlmacenYMe() = runTest {
        val almacen = Almacen().apply { token = "persistido" }; val estado = EstadoSesion()
        val motor = MockEngine { request ->
            assertEquals("Bearer persistido", request.headers[HttpHeaders.Authorization])
            respond("""{"username":"almacen","empleadoNombre":"Almacenero","permisos":["CATALOGO_LEER"]}""", HttpStatusCode.OK, jsonHeaders)
        }
        val client = crearHttpClientComun(motor, "https://pharma.test/api/v1/", almacen = almacen, estado = estado)
        try {
            val sesion = AuthRepositorioRest(client, almacen, estado).sesionGuardada()
            assertEquals("almacen", sesion?.usuario); assertTrue(sesion!!.puede("CATALOGO_LEER"))
            assertFalse(sesion.puede("CLIENTES_LEER"))
        } finally { client.close() }
    }
    @Test fun un401BorraTokenYCierraSesion() = runTest {
        val almacen = Almacen().apply { token = "expirado" }; val estado = EstadoSesion()
        estado.actualizar(Sesion("expirado", "admin", "Admin", emptySet()))
        val client = crearHttpClientComun(MockEngine { respond("{}", HttpStatusCode.Unauthorized, jsonHeaders) },
            "https://pharma.test/api/v1/", almacen = almacen, estado = estado)
        try {
            assertFailsWith<ClientRequestException> { client.get("productos") }
            assertNull(almacen.token); assertNull(estado.sesion.value); assertNotNull(estado.aviso.value)
        } finally { client.close() }
    }
    @Test fun un403MantieneSesion() = runTest {
        val almacen = Almacen().apply { token = "valido" }; val estado = EstadoSesion()
        estado.actualizar(Sesion("valido", "almacen", "Almacenero", emptySet()))
        val client = crearHttpClientComun(MockEngine { respond("{}", HttpStatusCode.Forbidden, jsonHeaders) },
            "https://pharma.test/api/v1/", almacen = almacen, estado = estado)
        try {
            assertFailsWith<ClientRequestException> { client.get("clientes") }
            assertEquals("valido", almacen.token); assertEquals("almacen", estado.sesion.value?.usuario)
        } finally { client.close() }
    }
    @Test fun noFiltraTokenAOtroServidor() = runTest {
        val almacen = Almacen().apply { token = "secreto" }
        val client = crearHttpClientComun(MockEngine { request ->
            assertNull(request.headers[HttpHeaders.Authorization]); respond("{}", HttpStatusCode.OK, jsonHeaders)
        }, "https://pharma.test/api/v1/", almacen = almacen)
        try { client.get("https://otro.test/productos") } finally { client.close() }
    }
    @Test fun loggingSaneadoYDesactivadoFueraDeDebug() = runTest {
        val almacen = Almacen().apply { token = "NO-DEBE-APARECER" }
        for (debug in listOf(true, false)) {
            val logs = mutableListOf<String>()
            val client = crearHttpClientComun(MockEngine { respond("{}", HttpStatusCode.OK, jsonHeaders) },
                "https://pharma.test/api/v1/", almacen = almacen, esDepuracion = debug,
                registro = object : Logger { override fun log(message: String) { logs += message } })
            try {
                client.get("productos")
                assertFalse(logs.any { "NO-DEBE-APARECER" in it })
                if (debug) assertTrue(logs.any { "Authorization" in it && "***" in it })
                else assertTrue(logs.isEmpty())
            } finally { client.close() }
        }
    }
}
