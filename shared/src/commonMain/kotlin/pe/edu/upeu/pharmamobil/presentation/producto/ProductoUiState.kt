package pe.edu.upeu.pharmamobil.presentation.producto

import pe.edu.upeu.pharmamobil.domain.model.Producto

sealed interface ProductoFase {
    data object Cargando : ProductoFase
    data object SinProductos : ProductoFase
    data class ConProductos(val productos: List<Producto>) : ProductoFase
    data class Error(val mensaje: String) : ProductoFase
}

sealed interface ProductoOperacion {
    data object Inactiva : ProductoOperacion
    data class EnCurso(val tipo: Tipo, val productoId: Long? = null) : ProductoOperacion
    data class Fallida(val mensaje: String) : ProductoOperacion

    enum class Tipo { Crear, Actualizar, Eliminar }
}

data class ProductoFormularioState(
    val productoId: Long? = null,
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val nombreError: String? = null,
    val precioError: String? = null,
    val stockError: String? = null,
) {
    val enEdicion: Boolean get() = productoId != null
}

data class ProductoUiState(
    val fase: ProductoFase = ProductoFase.Cargando,
    val filtro: FiltroInventario = FiltroInventario.ACTIVOS,
    val formulario: ProductoFormularioState = ProductoFormularioState(),
    val operacion: ProductoOperacion = ProductoOperacion.Inactiva,
    val mensajeExito: String? = null,
)
