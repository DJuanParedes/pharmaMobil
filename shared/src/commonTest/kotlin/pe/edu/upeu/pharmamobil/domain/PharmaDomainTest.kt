package pe.edu.upeu.pharmamobil.domain

import pe.edu.upeu.pharmamobil.domain.model.Cliente
import pe.edu.upeu.pharmamobil.domain.model.DetallePedido
import pe.edu.upeu.pharmamobil.domain.model.EstadoPedido
import pe.edu.upeu.pharmamobil.domain.model.Pedido
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.service.ProductoService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull

class PharmaDomainTest {
    private val cliente = Cliente(
        id = 1L,
        nombre = "Farmacia Nueva Vida",
        correo = "ventas@central.pe",
        telefono = null
    )

    private val productos = listOf(
        Producto(id = 1L, nombre = "Paracetamol", precio = 8.0, stock = 100),
        Producto(id = 2L, nombre = "Ibuprofeno", precio = 12.5, stock = 0),
        Producto(id = 3L, nombre = "Vitamina C", precio = 15.0, stock = 40)
    )

    @Test
    fun telefonoNuloUsaValorSeguro() {
        assertEquals("No registrado", cliente.obtenerTelefono())
    }

    @Test
    fun clienteInvalidoEsRechazado() {
        assertFailsWith<IllegalArgumentException> {
            Cliente(id = 0, nombre = "", correo = "correo-invalido", telefono = null)
        }
    }

    @Test
    fun copyActualizaStockSinMutarElProductoOriginal() {
        val original = productos.first()
        val actualizado = original.actualizarStock(90)

        assertNotSame(original, actualizado)
        assertEquals(100, original.stock)
        assertEquals(90, actualizado.stock)
    }

    @Test
    fun consultasUsanFilterMapYFindConNullSafety() {
        val service = ProductoService()

        assertEquals(listOf(1L, 3L), service.productosConStock(productos).map { it.id })
        assertEquals(listOf("Paracetamol", "Ibuprofeno", "Vitamina C"), service.nombresDeProductos(productos))
        assertEquals("Ibuprofeno", service.buscarPorId(productos, 2L)?.nombre)
        assertNull(service.buscarPorId(productos, 99L))
    }

    @Test
    fun pedidoCalculaTotalYExponeEstadoSellado() {
        val pedido = Pedido(
            id = 10L,
            cliente = cliente,
            detalles = listOf(DetallePedido(productos.first(), cantidad = 2))
        )
        val rechazado = pedido.actualizarEstado(EstadoPedido.Rechazado("Receta vencida"))

        assertEquals(16.0, pedido.total())
        assertEquals("Pedido pendiente", pedido.estado.descripcion())
        assertEquals("Pedido rechazado: Receta vencida", rechazado.estado.descripcion())
    }

    @Test
    fun detallePedidoRechazaCantidadIgualACero() {
        assertFailsWith<IllegalArgumentException> {
            DetallePedido(productos.first(), cantidad = 0)
        }
    }

    @Test
    fun reglaDeReposicionConsideraStockMinimoYEstadoActivo() {
        assertEquals(true, productos[1].requiereReposicion())
        assertEquals(false, productos[2].requiereReposicion())
        assertEquals(
            false,
            Producto(
                id = 4L,
                nombre = "Producto inactivo",
                precio = 5.0,
                stock = 0,
                activo = false,
            ).requiereReposicion(),
        )
    }
}
