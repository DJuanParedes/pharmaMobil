package pe.edu.upeu.pharmamobil.domain.model

data class Producto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val activo: Boolean = true,
) {
    companion object {
        const val STOCK_MINIMO = 5
    }

    init {
        require(id > 0) { "El id del producto debe ser positivo" }
        require(nombre.isNotBlank()) { "El nombre del producto es obligatorio" }
        require(precio >= 0.0) { "El precio no puede ser negativo" }
        require(stock >= 0) { "El stock no puede ser negativo" }
    }

    fun actualizarStock(nuevoStock: Int): Producto {
        require(nuevoStock >= 0) { "El stock no puede ser negativo" }
        return copy(stock = nuevoStock)
    }

    fun requiereReposicion(): Boolean = activo && stock <= STOCK_MINIMO
}
