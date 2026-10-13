package pe.edu.upeu.pharmamobil.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.error.mensajeDe
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.comoTextoParaCompartir
import pe.edu.upeu.pharmamobil.presentation.producto.toUi

class DetalleProductoViewModel(
    private val repository: ProductoRepository,
    private val compartidor: Compartidor,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DetalleProductoUiState>(DetalleProductoUiState.Cargando)
    val uiState = _uiState.asStateFlow()
    private var carga: Job? = null

    fun cargar(id: Long) {
        carga?.cancel()
        _uiState.value = DetalleProductoUiState.Cargando
        carga = viewModelScope.launch {
            try {
                val producto = repository.obtener(id)
                _uiState.value = DetalleProductoUiState.ConProducto(producto, producto.toUi())
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (fallo: Exception) {
                _uiState.value = DetalleProductoUiState.Error(
                    mensajeDe((fallo as? ErrorApiException)?.error),
                )
            }
        }
    }

    fun compartir() {
        val estado = _uiState.value as? DetalleProductoUiState.ConProducto ?: return
        compartir(estado.producto)
    }

    fun compartir(producto: Producto) {
        try {
            compartidor.compartir(producto.comoTextoParaCompartir())
            val estado = _uiState.value as? DetalleProductoUiState.ConProducto
            if (estado != null) _uiState.value = estado.copy(errorCompartir = null)
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (fallo: Exception) {
            val estado = _uiState.value as? DetalleProductoUiState.ConProducto ?: return
            _uiState.value = estado.copy(
                errorCompartir = fallo.message ?: "No se pudo abrir el selector de compartir.",
            )
        }
    }
}
