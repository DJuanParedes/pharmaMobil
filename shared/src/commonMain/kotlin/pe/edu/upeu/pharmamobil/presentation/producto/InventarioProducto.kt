package pe.edu.upeu.pharmamobil.presentation.producto

import pe.edu.upeu.pharmamobil.domain.model.Producto

internal const val LIMITE_BAJO_STOCK = 5

internal enum class FiltroInventario(val titulo: String) {
    ACTIVOS("Activos"),
    INACTIVOS("Inactivos"),
    BAJO_STOCK("Bajo stock"),
}

internal val productosSimulados = listOf(
    Producto(id = 1L, nombre = "Paracetamol", precio = 15.50, stock = 100),
    Producto(id = 2L, nombre = "Ibuprofeno", precio = 18.90, stock = 50),
    Producto(id = 3L, nombre = "Amoxicilina", precio = 25.00, stock = 5),
    Producto(id = 4L, nombre = "Loratadina", precio = 12.50, stock = 0, activo = false),
    Producto(id = 5L, nombre = "Diclofenaco", precio = 20.00, stock = 3),
)

internal fun esProductoDeBajoStock(producto: Producto): Boolean =
    producto.activo && producto.stock <= LIMITE_BAJO_STOCK

internal fun filtrarInventario(
    productos: List<Producto>,
    filtro: FiltroInventario,
): List<Producto> = when (filtro) {
    FiltroInventario.ACTIVOS -> productos.filter { it.activo }
    FiltroInventario.INACTIVOS -> productos.filterNot { it.activo }
    FiltroInventario.BAJO_STOCK -> productos.filter(::esProductoDeBajoStock)
}

internal fun agregarProductoAlInventario(
    productos: List<Producto>,
    producto: Producto,
): List<Producto> {
    val siguienteId = (productos.maxOfOrNull { it.id } ?: 0L) + 1L
    return productos + producto.copy(id = siguienteId, activo = true)
}
