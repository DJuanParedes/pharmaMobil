package pe.edu.upeu.pharmamobil.domain.model

/** Producto creado sin conexión junto con su estado en la cola de envío. */
data class FilaPendiente(
    val producto: Producto,
    val intentos: Int,
    val ultimoError: String?,
) {
    val id: Long get() = producto.id
}
