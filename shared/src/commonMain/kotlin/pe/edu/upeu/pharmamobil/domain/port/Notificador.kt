package pe.edu.upeu.pharmamobil.domain.port

/** Aviso local al usuario; usa el permiso de notificaciones cuando está concedido. */
interface Notificador {
    fun avisarFilasFallidas(cantidad: Int)
}

object NotificadorNulo : Notificador {
    override fun avisarFilasFallidas(cantidad: Int) = Unit
}
