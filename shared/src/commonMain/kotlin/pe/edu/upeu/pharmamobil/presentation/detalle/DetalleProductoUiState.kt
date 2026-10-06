package pe.edu.upeu.pharmamobil.presentation.detalle

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoUi

sealed interface DetalleProductoUiState {
    data object Cargando : DetalleProductoUiState
    data class ConProducto(
        val producto: Producto,
        val productoUi: ProductoUi,
        val errorCompartir: String? = null,
    ) : DetalleProductoUiState
    data class Error(val mensaje: String) : DetalleProductoUiState
}
