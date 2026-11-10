package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.EstadoSesion
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClientComun
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Sesión 14, hueco 7: al cerrar sesión el equipo queda limpio para otro usuario. */
class CierreSesionLimpiezaTest {
    private class Almacen : AlmacenSeguro {
        var token: String? = null
        override fun guardarToken(token: String) { this.token = token }
        override fun leerToken() = token
        override fun borrarToken() { token = null }
    }

    @Test fun cerrarSesionBorraTokenInventarioColaYFecha() = runTest {
        val local = ProductoLocal(PharmaDb(driverEnMemoria()))
        local.guardar(Producto(10, "S14 Paracetamol", 4.5, 40))
        local.guardarPendiente(Producto(1, "S14 Ibuprofeno", 6.0, 12))
        local.registrarSincronizacion(1_000L)
        val almacen = Almacen()
        val motor = MockEngine {
            respond(
                """{"token":"t-1","tipo":"Bearer","expiraEnSegundos":3600,"username":"admin","nombreCompleto":"Prueba","roles":["ADMIN"],"permisos":["CATALOGO_LEER"]}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val cliente = crearHttpClientComun(motor, "https://pharma.test/api/v1/", almacen = almacen, estado = EstadoSesion())
        try {
            val repo = AuthRepositorioRest(cliente, almacen, EstadoSesion()) { local.limpiar() }
            repo.iniciarSesion(Credenciales("admin", "prueba"))
            assertEquals(2, local.listar().size)

            repo.cerrarSesion()

            assertNull(almacen.token)
            assertEquals(0, local.listar().size)
            assertEquals(0, local.contarPendientes(5))
            assertNull(local.observarUltimaSincronizacion().first())
        } finally { cliente.close() }
    }
}
