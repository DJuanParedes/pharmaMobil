package pe.edu.upeu.pharmamobil.domain.service

import pe.edu.upeu.pharmamobil.domain.model.EstadoPedido
import pe.edu.upeu.pharmamobil.domain.model.Pedido
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.result.ResultadoProcesamientoPedido

class PedidoService {
    fun procesarPedido(
        pedido: Pedido,
        inventario: List<Producto>
    ): ResultadoProcesamientoPedido {
        if (pedido.estado != EstadoPedido.Pendiente) {
            return ResultadoProcesamientoPedido.EstadoInvalido(pedido.estado)
        }

        val cantidadesSolicitadas = pedido.detalles
            .groupBy { it.producto.id }
            .mapValues { (_, detalles) -> detalles.sumOf { it.cantidad } }

        cantidadesSolicitadas.forEach { (productoId, cantidadSolicitada) ->
            val producto = inventario.find { it.id == productoId }
                ?: return ResultadoProcesamientoPedido.ProductoNoEncontrado(productoId)

            if (producto.stock < cantidadSolicitada) {
                return ResultadoProcesamientoPedido.StockInsuficiente(
                    productoId = productoId,
                    cantidadSolicitada = cantidadSolicitada,
                    stockDisponible = producto.stock
                )
            }
        }

        val inventarioActualizado = inventario.map { producto ->
            val cantidadSolicitada = cantidadesSolicitadas[producto.id]
            if (cantidadSolicitada == null) {
                producto
            } else {
                producto.actualizarStock(producto.stock - cantidadSolicitada)
            }
        }

        return ResultadoProcesamientoPedido.Exito(
            pedido = pedido.actualizarEstado(EstadoPedido.Procesando),
            inventarioActualizado = inventarioActualizado
        )
    }
}
