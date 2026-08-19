package pe.edu.upeu.pharmamobil.domain.model

data class Pedido(
    val id: Long,
    val cliente: Cliente,
    val detalles: List<DetallePedido>,
    val estado: EstadoPedido = EstadoPedido.Pendiente
) {
    init {
        require(id > 0) { "El id del pedido debe ser positivo" }
        require(detalles.isNotEmpty()) { "El pedido debe contener al menos un detalle" }
    }

    fun total(): Double = detalles.sumOf { it.subtotal() }

    fun actualizarEstado(nuevoEstado: EstadoPedido): Pedido =
        copy(estado = nuevoEstado)
}
