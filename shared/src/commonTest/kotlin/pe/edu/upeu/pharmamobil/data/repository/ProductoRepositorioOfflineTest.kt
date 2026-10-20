package pe.edu.upeu.pharmamobil.data.repository

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import kotlin.test.*

expect fun driverEnMemoria(): SqlDriver

class ProductoRepositorioOfflineTest {
    private class Escenario {
        val driver = driverEnMemoria()
        val local = ProductoLocal(PharmaDb(driver))
        val remoto = Remoto()
        val repo = ProductoRepositorioOffline(local, remoto)
    }
    private class Remoto : ProductoRepository {
        val datos = mutableListOf<Producto>()
        val eventos = mutableListOf<String>()
        var sinRed = false
        var falloGet = false
        var falloNombre: String? = null
        private fun red() { if (sinRed) throw ErrorApiException(ErrorApi.SinConexion) }
        override suspend fun listar(): List<Producto> {
            eventos += "GET"; red()
            if (falloGet) throw ErrorApiException(ErrorApi.Servidor)
            return datos.toList()
        }
        override suspend fun registrar(nombre: String, precio: Double, stock: Int): Producto {
            eventos += "POST:$nombre"; red()
            if (nombre == falloNombre) throw ErrorApiException(ErrorApi.Conflicto("Nombre duplicado"))
            return Producto((datos.maxOfOrNull { it.id } ?: 0L) + 1, nombre, precio, stock).also(datos::add)
        }
        override suspend fun obtener(id: Long) = datos.first { it.id == id }
        override suspend fun actualizar(producto: Producto): Producto = error("No usado")
        override suspend fun eliminar(id: Long) { red(); datos.removeAll { it.id == id } }
    }

    @Test fun conRedLaBaseYElFlowRecibenProductos() = runTest {
        val e = Escenario()
        try {
            e.remoto.datos += Producto(1, "Paracetamol", 3.0, 10)
            e.repo.sincronizar()
            assertEquals(e.remoto.datos, e.repo.observar().first())
            assertTrue(e.local.pendientes().isEmpty())
        } finally { e.driver.close() }
    }
    @Test fun sinRedElRegistroQuedaVisibleYPendiente() = runTest {
        val e = Escenario()
        try {
            e.remoto.sinRed = true
            val producto = e.repo.registrar("Ibuprofeno", 4.5, 20)
            assertTrue(producto.id < 0)
            assertTrue(e.repo.observar().first().single().pendiente)
            assertEquals(producto, e.local.pendientes().single())
        } finally { e.driver.close() }
    }
    @Test fun reconexionSubeAntesDeBajarYNoDuplicaEnElReintento() = runTest {
        val e = Escenario()
        try {
            e.remoto.sinRed = true
            e.repo.registrar("Ibuprofeno", 4.5, 20)
            e.remoto.sinRed = false; e.remoto.eventos.clear()
            e.repo.sincronizar()
            assertEquals(listOf("POST:Ibuprofeno", "GET"), e.remoto.eventos)
            assertTrue(e.local.pendientes().isEmpty())
            assertTrue(e.local.listar().single().id > 0)
            e.repo.sincronizar()
            assertEquals(1, e.remoto.datos.size)
            assertEquals(1, e.local.listar().size)
        } finally { e.driver.close() }
    }
    @Test fun reemplazarSincronizadosConservaAltasLocales() = runTest {
        val e = Escenario()
        try {
            val pendiente = e.local.guardarPendiente(Producto(-1, "Local", 1.0, 1))
            e.local.reemplazarSincronizados(listOf(Producto(5, "Servidor", 2.0, 2)))
            assertEquals(pendiente, e.local.pendientes().single())
            assertEquals(2, e.local.listar().size)
        } finally { e.driver.close() }
    }
    @Test fun falloDeDescargaNoReenviaAltasConfirmadas() = runTest {
        val e = Escenario()
        try {
            e.local.guardarPendiente(Producto(-1, "Confirmado", 1.0, 1))
            e.remoto.falloGet = true
            assertFailsWith<ErrorApiException> { e.repo.sincronizar() }
            assertTrue(e.local.pendientes().isEmpty())
            assertEquals(1, e.local.listar().size)
            e.remoto.falloGet = false; e.repo.sincronizar()
            assertEquals(1, e.remoto.datos.size)
        } finally { e.driver.close() }
    }
    @Test fun conflictoConservaPendienteYNoDescarga() = runTest {
        val e = Escenario()
        try {
            e.local.guardarPendiente(Producto(-1, "Duplicado", 1.0, 1))
            e.remoto.falloNombre = "Duplicado"
            val fallo = assertFailsWith<ErrorApiException> { e.repo.sincronizar() }
            assertIs<ErrorApi.Conflicto>(fallo.error)
            assertEquals(1, e.local.pendientes().size)
            assertFalse("GET" in e.remoto.eventos)
        } finally { e.driver.close() }
    }
    @Test fun idsNegativosSonDistintosSinColisionConServidor() = runTest {
        val e = Escenario()
        try {
            e.local.reemplazarSincronizados(listOf(Producto(1, "Servidor", 1.0, 1)))
            e.remoto.sinRed = true
            val a = e.repo.registrar("A", 1.0, 1)
            val b = e.repo.registrar("B", 1.0, 1)
            assertTrue(a.id < 0 && b.id < 0 && a.id != b.id)
            assertEquals(3, e.local.listar().size)
        } finally { e.driver.close() }
    }
    @Test fun servidorApagadoConservaListadoAnterior() = runTest {
        val e = Escenario()
        try {
            e.local.reemplazarSincronizados(listOf(Producto(1, "Guardado", 1.0, 1)))
            e.remoto.sinRed = true
            assertFailsWith<ErrorApiException> { e.repo.sincronizar() }
            assertEquals("Guardado", e.repo.observar().first().single().nombre)
        } finally { e.driver.close() }
    }
}
