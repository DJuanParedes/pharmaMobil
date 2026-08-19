package pe.edu.upeu.pharmamobil.domain

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.repository.InventarioRepositoryMemoria
import pe.edu.upeu.pharmamobil.domain.model.Cliente
import pe.edu.upeu.pharmamobil.domain.model.DetallePedido
import pe.edu.upeu.pharmamobil.domain.model.EstadoPedido
import pe.edu.upeu.pharmamobil.domain.model.Pedido
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.result.ResultadoProcesamientoPedido
import pe.edu.upeu.pharmamobil.domain.service.PedidoService
import pe.edu.upeu.pharmamobil.domain.usecase.ProcesarPedidoUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ActividadAutonomaTest {
    private val cliente = Cliente(
        id = 1L,
        nombre = "Farmacia Nueva Vida",
        correo = "ventas@central.pe",
        telefono = null
    )

    private val paracetamol = Producto(
        id = 1L,
        nombre = "Paracetamol",
        precio = 8.0,
        stock = 100
    )

    private val ibuprofeno = Producto(
        id = 2L,
        nombre = "Ibuprofeno",
        precio = 12.5,
        stock = 10
    )

    private val inventario = listOf(paracetamol, ibuprofeno)
    private val service = PedidoService()

    @Test
    fun pedidoValidoDescuentaStockYCambiaAProcesando() {
        val pedido = crearPedido(DetallePedido(paracetamol, cantidad = 5))

        val resultado = assertIs<ResultadoProcesamientoPedido.Exito>(
            service.procesarPedido(pedido, inventario)
        )

        assertEquals(EstadoPedido.Procesando, resultado.pedido.estado)
        assertEquals(95, resultado.inventarioActualizado.find { it.id == 1L }?.stock)
        assertEquals(100, paracetamol.stock)
    }

    @Test
    fun productoInexistenteDevuelveResultadoSeguro() {
        val productoInexistente = Producto(
            id = 99L,
            nombre = "Producto externo",
            precio = 1.0,
            stock = 1
        )
        val pedido = crearPedido(DetallePedido(productoInexistente, cantidad = 1))

        val resultado = assertIs<ResultadoProcesamientoPedido.ProductoNoEncontrado>(
            service.procesarPedido(pedido, inventario)
        )

        assertEquals(99L, resultado.productoId)
    }

    @Test
    fun stockInsuficienteNoCambiaElInventario() {
        val pedido = crearPedido(DetallePedido(ibuprofeno, cantidad = 11))

        val resultado = assertIs<ResultadoProcesamientoPedido.StockInsuficiente>(
            service.procesarPedido(pedido, inventario)
        )

        assertEquals(11, resultado.cantidadSolicitada)
        assertEquals(10, resultado.stockDisponible)
        assertEquals(10, ibuprofeno.stock)
    }

    @Test
    fun detallesRepetidosSeSumanAntesDeDescontarStock() {
        val pedido = crearPedido(
            DetallePedido(paracetamol, cantidad = 3),
            DetallePedido(paracetamol, cantidad = 4)
        )

        val resultado = assertIs<ResultadoProcesamientoPedido.Exito>(
            service.procesarPedido(pedido, inventario)
        )

        assertEquals(93, resultado.inventarioActualizado.find { it.id == 1L }?.stock)
    }

    @Test
    fun pedidoQueNoEstaPendienteEsRechazado() {
        val pedido = crearPedido(DetallePedido(paracetamol, cantidad = 1))
            .actualizarEstado(EstadoPedido.Procesando)

        val resultado = assertIs<ResultadoProcesamientoPedido.EstadoInvalido>(
            service.procesarPedido(pedido, inventario)
        )

        assertEquals(EstadoPedido.Procesando, resultado.estadoActual)
    }

    @Test
    fun flowEmiteInventarioInicialYActualizado() = runTest {
        val repository = InventarioRepositoryMemoria(
            productosIniciales = inventario,
            latenciaMillis = 1_000
        )
        val useCase = ProcesarPedidoUseCase(repository)
        val emisiones = mutableListOf<List<Producto>>()
        val recolector = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.observarInventario().take(2).toList(emisiones)
        }

        val resultado = useCase(
            crearPedido(DetallePedido(paracetamol, cantidad = 5))
        )
        recolector.join()

        assertIs<ResultadoProcesamientoPedido.Exito>(resultado)
        assertEquals(listOf(100, 95), emisiones.map { productos ->
            productos.find { it.id == 1L }?.stock
        })
    }

    private fun crearPedido(vararg detalles: DetallePedido): Pedido =
        Pedido(
            id = 10L,
            cliente = cliente,
            detalles = detalles.toList()
        )
}
