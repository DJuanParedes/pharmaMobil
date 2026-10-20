package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.error.mensajeDe
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.CampoProducto
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ValidacionProductoException

class ProductoViewModel(
    private val listarProductos: ListarProductosUseCase,
    private val registrarProducto: RegistrarProductoUseCase,
    private val actualizarProducto: ActualizarProductoUseCase,
    private val eliminarProducto: EliminarProductoUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            listarProductos.observar().collect { productos ->
                _uiState.update { it.copy(fase = faseDe(productos)) }
            }
        }
        cargarProductos()
    }

    fun cargarProductos() {
        viewModelScope.launch {
            _uiState.update { it.copy(sincronizando = true) }
            val resultado = listarProductos.sincronizar()
            _uiState.update { it.copy(sincronizando = false, avisoRed = resultado.exceptionOrNull()?.let { fallo ->
                val error = (fallo as? ErrorApiException)?.error
                if (error == ErrorApi.SinConexion || error == ErrorApi.TiempoAgotado)
                    "Sin conexión: mostrando datos guardados"
                else mensajeDe(error)
            }) }
        }
    }

    fun cambiarFiltro(filtro: FiltroInventario) = _uiState.update { it.copy(filtro = filtro) }
    fun cambiarNombre(valor: String) = actualizarFormulario { copy(nombre = valor, nombreError = null) }
    fun cambiarPrecio(valor: String) = actualizarFormulario { copy(precio = valor, precioError = null) }
    fun cambiarStock(valor: String) = actualizarFormulario { copy(stock = valor, stockError = null) }

    fun iniciarEdicion(producto: Producto) {
        _uiState.update {
            it.copy(
                formulario = ProductoFormularioState(
                    productoId = producto.id,
                    nombre = producto.nombre,
                    precio = producto.precio.toString(),
                    stock = producto.stock.toString(),
                ),
                operacion = ProductoOperacion.Inactiva,
                mensajeExito = null,
            )
        }
    }

    fun cancelarEdicion() = _uiState.update {
        it.copy(formulario = ProductoFormularioState(), operacion = ProductoOperacion.Inactiva)
    }

    fun guardar() {
        val formulario = _uiState.value.formulario
        if (formulario.productoId == null) registrar(formulario) else actualizar(formulario)
    }

    fun eliminar(id: Long) = viewModelScope.launch {
        marcarOperacion(ProductoOperacion.Tipo.Eliminar, id)
        eliminarProducto(id).fold(
            onSuccess = { finalizarMutacion("Producto eliminado correctamente.") },
            onFailure = ::manejarFallo,
        )
    }

    fun limpiarMensaje() = _uiState.update {
        it.copy(mensajeExito = null, operacion = ProductoOperacion.Inactiva)
    }

    private fun registrar(formulario: ProductoFormularioState) = viewModelScope.launch {
        marcarOperacion(ProductoOperacion.Tipo.Crear)
        registrarProducto(formulario.nombre, formulario.precio, formulario.stock).fold(
            onSuccess = { finalizarMutacion("Producto guardado. Si no hay conexión, queda pendiente.") },
            onFailure = ::manejarFallo,
        )
    }

    private fun actualizar(formulario: ProductoFormularioState) = viewModelScope.launch {
        val id = checkNotNull(formulario.productoId)
        marcarOperacion(ProductoOperacion.Tipo.Actualizar, id)
        actualizarProducto(id, formulario.nombre, formulario.precio, formulario.stock).fold(
            onSuccess = { finalizarMutacion("Producto actualizado correctamente.") },
            onFailure = ::manejarFallo,
        )
    }

    private fun finalizarMutacion(mensaje: String) {
        _uiState.update { it.copy(formulario = ProductoFormularioState(),
            operacion = ProductoOperacion.Inactiva, mensajeExito = mensaje) }
    }

    private fun marcarOperacion(tipo: ProductoOperacion.Tipo, id: Long? = null) {
        _uiState.update {
            it.copy(
                operacion = ProductoOperacion.EnCurso(tipo, id),
                mensajeExito = null,
                formulario = it.formulario.copy(
                    nombreError = null,
                    precioError = null,
                    stockError = null,
                ),
            )
        }
    }

    private fun manejarFallo(fallo: Throwable) {
        when (fallo) {
            is ValidacionProductoException -> aplicarErrorLocal(fallo)
            is ErrorApiException -> when (val error = fallo.error) {
                is ErrorApi.Validacion -> _uiState.update {
                    it.copy(
                        operacion = ProductoOperacion.Inactiva,
                        formulario = it.formulario.copy(
                            nombreError = error.porCampo["nombre"],
                            precioError = error.porCampo["precio"],
                            stockError = error.porCampo["stock"],
                        ),
                    )
                }
                else -> _uiState.update {
                    it.copy(operacion = ProductoOperacion.Fallida(mensajeDe(error)))
                }
            }
            else -> _uiState.update {
                it.copy(operacion = ProductoOperacion.Fallida(fallo.message ?: mensajeDe(null)))
            }
        }
    }

    private fun aplicarErrorLocal(error: ValidacionProductoException) {
        _uiState.update {
            val formulario = when (error.campo) {
                CampoProducto.NOMBRE -> it.formulario.copy(nombreError = error.message)
                CampoProducto.PRECIO -> it.formulario.copy(precioError = error.message)
                CampoProducto.STOCK -> it.formulario.copy(stockError = error.message)
            }
            it.copy(formulario = formulario, operacion = ProductoOperacion.Inactiva)
        }
    }

    private fun actualizarFormulario(cambio: ProductoFormularioState.() -> ProductoFormularioState) {
        _uiState.update { it.copy(formulario = it.formulario.cambio(), mensajeExito = null) }
    }

    private fun faseDe(productos: List<Producto>): ProductoFase =
        if (productos.isEmpty()) ProductoFase.SinProductos else ProductoFase.ConProductos(productos)
}
