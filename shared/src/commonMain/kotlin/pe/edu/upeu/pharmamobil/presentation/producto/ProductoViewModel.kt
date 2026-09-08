package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ValidacionProductoException

class ProductoViewModel(
    private val registrarProducto: RegistrarProductoUseCase,
    private val repository: ProductoRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    init {
        cargarProductos()
    }

    fun cargarProductos() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    fase = ProductoFase.Cargando,
                    formulario = it.formulario.copy(enProceso = false),
                )
            }

            runCatching { repository.listar() }
                .onSuccess(::mostrarProductos)
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            fase = ProductoFase.Error(
                                error.message ?: "No se pudo cargar el inventario.",
                            ),
                        )
                    }
                }
        }
    }

    fun cambiarFiltro(filtro: FiltroInventario) {
        _uiState.update { it.copy(filtro = filtro) }
    }

    fun cambiarNombre(nombre: String) = actualizarFormulario { copy(nombre = nombre) }

    fun cambiarPrecio(precio: String) = actualizarFormulario { copy(precio = precio) }

    fun cambiarStock(stock: String) = actualizarFormulario { copy(stock = stock) }

    fun registrar() {
        val formularioActual = _uiState.value.formulario
        val faseAnterior = _uiState.value.fase

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    fase = ProductoFase.Cargando,
                    formulario = formularioActual.copy(
                        campoConError = null,
                        mensaje = null,
                        registroExitoso = false,
                        ultimoProductoRegistrado = null,
                        enProceso = true,
                    ),
                )
            }

            registrarProducto(
                nombre = formularioActual.nombre,
                precio = formularioActual.precio,
                stock = formularioActual.stock,
            ).fold(
                onSuccess = { producto ->
                    val productos = runCatching { repository.listar() }
                        .getOrElse { error ->
                            mostrarError(error)
                            return@fold
                        }

                    _uiState.update {
                        it.copy(
                            fase = faseDe(productos),
                            filtro = if (producto.requiereReposicion()) {
                                FiltroInventario.BAJO_STOCK
                            } else {
                                FiltroInventario.ACTIVOS
                            },
                            formulario = ProductoFormularioState(
                                mensaje = "Producto registrado correctamente.",
                                registroExitoso = true,
                                ultimoProductoRegistrado = producto,
                            ),
                        )
                    }
                },
                onFailure = { error ->
                    if (error is ValidacionProductoException) {
                        _uiState.update {
                            it.copy(
                                fase = faseAnterior,
                                formulario = formularioActual.copy(
                                    campoConError = error.campo,
                                    mensaje = error.message,
                                    enProceso = false,
                                ),
                            )
                        }
                    } else {
                        mostrarError(error)
                    }
                },
            )
        }
    }

    private fun actualizarFormulario(
        cambio: ProductoFormularioState.() -> ProductoFormularioState,
    ) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.cambio().copy(
                    campoConError = null,
                    mensaje = null,
                    registroExitoso = false,
                    ultimoProductoRegistrado = null,
                ),
            )
        }
    }

    private fun mostrarProductos(productos: List<Producto>) {
        _uiState.update { it.copy(fase = faseDe(productos)) }
    }

    private fun mostrarError(error: Throwable) {
        _uiState.update {
            it.copy(
                fase = ProductoFase.Error(
                    error.message ?: "No se pudo completar la operación.",
                ),
                formulario = it.formulario.copy(enProceso = false),
            )
        }
    }

    private fun faseDe(productos: List<Producto>): ProductoFase =
        if (productos.isEmpty()) {
            ProductoFase.SinProductos
        } else {
            ProductoFase.ConProductos(productos)
        }
}
