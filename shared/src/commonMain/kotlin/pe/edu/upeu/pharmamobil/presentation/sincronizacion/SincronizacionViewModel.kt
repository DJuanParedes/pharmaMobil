package pe.edu.upeu.pharmamobil.presentation.sincronizacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioOffline
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.repository.ProductoLocalRepository
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarUseCase.Companion.MAX_INTENTOS

/**
 * El estado se arma con consultas observables de la base local, nunca consultando la red:
 * pendientes, fallidas y fecha de la última sincronización.
 */
class SincronizacionViewModel(
    private val local: ProductoLocalRepository,
    private val repositorio: ProductoRepositorioOffline,
) : ViewModel() {
    private val enCurso = MutableStateFlow(false)
    private val sinConexion = MutableStateFlow(false)

    val estado: StateFlow<SincronizacionUiState> = combine(
        local.observarPendientes(MAX_INTENTOS),
        local.observarFallidas(MAX_INTENTOS),
        local.observarUltimaSincronizacion(),
        enCurso,
        sinConexion,
    ) { pend, fall, ultima, curso, offline ->
        SincronizacionUiState(
            enCurso = curso,
            pendientes = pend,
            fallidas = fall,
            ultimaSync = ultima,
            sinConexion = offline,
            detalleFallidas = if (fall == 0) emptyList() else local.fallidas(MAX_INTENTOS)
                .map { "${it.producto.nombre}: ${it.ultimoError ?: "sin detalle"}" },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SincronizacionUiState())

    fun sincronizar() {
        if (enCurso.value) return
        viewModelScope.launch {
            enCurso.value = true
            try {
                val resumen = repositorio.sincronizarConResumen()
                sinConexion.value = resumen.errorDescarga == ErrorApi.SinConexion ||
                    resumen.errorDescarga == ErrorApi.TiempoAgotado
            } finally {
                enCurso.value = false
            }
        }
    }

    /** Devuelve las filas fallidas a la cola y vuelve a intentar ahora. */
    fun reintentarFallidas() {
        local.reintentarFallidas(MAX_INTENTOS)
        sincronizar()
    }
}
