package pe.edu.upeu.pharmamobil.domain.model

sealed class EstadoPedido {
    data object Pendiente : EstadoPedido()
    data object Procesando : EstadoPedido()
    data object Entregado : EstadoPedido()

    data class Rechazado(val motivo: String) : EstadoPedido() {
        init {
            require(motivo.isNotBlank()) { "El rechazo debe incluir un motivo" }
        }
    }

    fun descripcion(): String = when (this) {
        Pendiente -> "Pedido pendiente"
        Procesando -> "Pedido en proceso"
        Entregado -> "Pedido entregado"
        is Rechazado -> "Pedido rechazado: $motivo"
    }
}
