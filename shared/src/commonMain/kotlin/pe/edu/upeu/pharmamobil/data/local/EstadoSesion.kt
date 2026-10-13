package pe.edu.upeu.pharmamobil.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.edu.upeu.pharmamobil.domain.model.Sesion

class EstadoSesion {
    private val mutable = MutableStateFlow<Sesion?>(null)
    val sesion = mutable.asStateFlow()
    private val avisoMutable = MutableStateFlow<String?>(null)
    val aviso = avisoMutable.asStateFlow()
    fun actualizar(sesion: Sesion?) { mutable.value = sesion }
    fun expirar() {
        mutable.value = null
        avisoMutable.value = "Tu sesión expiró. Inicia sesión nuevamente."
    }
    fun limpiarAviso() { avisoMutable.value = null }
}
