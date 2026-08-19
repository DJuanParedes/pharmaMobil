package pe.edu.upeu.pharmamobil.demo

import kotlinx.coroutines.flow.first
import pe.edu.upeu.pharmamobil.data.repository.InventarioRepositoryMemoria
import pe.edu.upeu.pharmamobil.domain.model.Cliente
import pe.edu.upeu.pharmamobil.domain.model.DetallePedido
import pe.edu.upeu.pharmamobil.domain.model.Pedido
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.result.ResultadoProcesamientoPedido
import pe.edu.upeu.pharmamobil.domain.usecase.ProcesarPedidoUseCase

suspend fun probarActividadAutonoma() {
    val paracetamol = Producto(
        id = 1L,
        nombre = "Paracetamol",
        precio = 8.0,
        stock = 100
    )
    val repository = InventarioRepositoryMemoria(
        productosIniciales = listOf(paracetamol),
        latenciaMillis = 1_000
    )
    val procesarPedido = ProcesarPedidoUseCase(repository)
    val pedido = Pedido(
        id = 1L,
        cliente = Cliente(
            id = 1L,
            nombre = "Farmacia Nueva Vida",
            correo = "ventas@central.pe",
            telefono = null
        ),
        detalles = listOf(DetallePedido(paracetamol, cantidad = 5))
    )

    val stockInicial = repository.observarInventario().first().first().stock
    val resultado = procesarPedido(pedido)
    val stockFinal = repository.observarInventario().first().first().stock

    when (resultado) {
        is ResultadoProcesamientoPedido.Exito -> {
            println("${resultado.pedido.estado.descripcion()}: stock $stockInicial -> $stockFinal")
        }

        is ResultadoProcesamientoPedido.ProductoNoEncontrado -> {
            println("Producto ${resultado.productoId} no encontrado")
        }

        is ResultadoProcesamientoPedido.StockInsuficiente -> {
            println("Stock insuficiente para el producto ${resultado.productoId}")
        }

        is ResultadoProcesamientoPedido.EstadoInvalido -> {
            println("Estado no permitido: ${resultado.estadoActual.descripcion()}")
        }
    }
}
