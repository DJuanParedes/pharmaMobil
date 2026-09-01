package pe.edu.upeu.pharmamobil.presentation.pedido

import pe.edu.upeu.pharmamobil.domain.model.Cliente
import pe.edu.upeu.pharmamobil.domain.model.DetallePedido
import pe.edu.upeu.pharmamobil.domain.model.EstadoPedido
import pe.edu.upeu.pharmamobil.domain.model.Pedido
import pe.edu.upeu.pharmamobil.domain.model.Producto

internal enum class CampoPedido {
    CLIENTE,
    PRODUCTO,
    CANTIDAD,
}

internal sealed interface ResultadoRegistroPedido {
    data class Exito(val pedido: Pedido) : ResultadoRegistroPedido

    data class Error(
        val campo: CampoPedido,
        val mensaje: String,
    ) : ResultadoRegistroPedido
}

fun pedidosSimulados(
    clientes: List<Cliente>,
    productos: List<Producto>,
): List<Pedido> {
    val cliente = clientes.firstOrNull() ?: return emptyList()
    val producto = productos.firstOrNull { it.activo && it.stock > 0 } ?: return emptyList()
    return listOf(
        Pedido(
            id = 1L,
            cliente = cliente,
            detalles = listOf(DetallePedido(producto = producto, cantidad = 2)),
            estado = EstadoPedido.Procesando,
        )
    )
}

internal fun validarPedidoRegistro(
    cliente: Cliente?,
    producto: Producto?,
    cantidad: String,
): ResultadoRegistroPedido {
    val cantidadNumerica = cantidad.toIntOrNull()

    return when {
        cliente == null -> ResultadoRegistroPedido.Error(
            CampoPedido.CLIENTE,
            "Selecciona un cliente.",
        )

        producto == null -> ResultadoRegistroPedido.Error(
            CampoPedido.PRODUCTO,
            "Selecciona un producto disponible.",
        )

        cantidadNumerica == null || cantidadNumerica <= 0 -> ResultadoRegistroPedido.Error(
            CampoPedido.CANTIDAD,
            "La cantidad debe ser un entero mayor que cero.",
        )

        cantidadNumerica > producto.stock -> ResultadoRegistroPedido.Error(
            CampoPedido.CANTIDAD,
            "Solo hay ${producto.stock} unidades disponibles.",
        )

        else -> ResultadoRegistroPedido.Exito(
            Pedido(
                id = 1L,
                cliente = cliente,
                detalles = listOf(
                    DetallePedido(
                        producto = producto,
                        cantidad = cantidadNumerica,
                    )
                ),
            )
        )
    }
}

internal fun agregarPedidoAlRegistro(
    pedidos: List<Pedido>,
    pedido: Pedido,
): List<Pedido> {
    val siguienteId = (pedidos.maxOfOrNull { it.id } ?: 0L) + 1L
    return pedidos + pedido.copy(id = siguienteId)
}
