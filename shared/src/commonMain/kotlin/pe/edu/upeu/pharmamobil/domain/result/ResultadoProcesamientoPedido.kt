package pe.edu.upeu.pharmamobil.domain.result

import pe.edu.upeu.pharmamobil.domain.model.EstadoPedido
import pe.edu.upeu.pharmamobil.domain.model.Pedido
import pe.edu.upeu.pharmamobil.domain.model.Producto

sealed class ResultadoProcesamientoPedido {
    data class Exito(
        val pedido: Pedido,
        val inventarioActualizado: List<Producto>
    ) : ResultadoProcesamientoPedido()

    data class ProductoNoEncontrado(
        val productoId: Long
    ) : ResultadoProcesamientoPedido()

    data class StockInsuficiente(
        val productoId: Long,
        val cantidadSolicitada: Int,
        val stockDisponible: Int
    ) : ResultadoProcesamientoPedido()

    data class EstadoInvalido(
        val estadoActual: EstadoPedido
    ) : ResultadoProcesamientoPedido()
}
