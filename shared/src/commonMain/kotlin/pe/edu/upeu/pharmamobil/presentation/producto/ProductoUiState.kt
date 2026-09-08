package pe.edu.upeu.pharmamobil.presentation.producto

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.usecase.CampoProducto

sealed interface ProductoFase {
    data object Cargando : ProductoFase

    data object SinProductos : ProductoFase

    data class ConProductos(
        val productos: List<Producto>,
    ) : ProductoFase

    data class Error(
        val mensaje: String,
    ) : ProductoFase
}

data class ProductoFormularioState(
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val campoConError: CampoProducto? = null,
    val mensaje: String? = null,
    val registroExitoso: Boolean = false,
    val ultimoProductoRegistrado: Producto? = null,
    val enProceso: Boolean = false,
)

data class ProductoUiState(
    val fase: ProductoFase = ProductoFase.Cargando,
    val filtro: FiltroInventario = FiltroInventario.ACTIVOS,
    val formulario: ProductoFormularioState = ProductoFormularioState(),
)
