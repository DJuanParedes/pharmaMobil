package pe.edu.upeu.pharmamobil.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pe.edu.upeu.pharmamobil.domain.model.*
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.data.local.EstadoSesion

data class SesionUiState(
    val usuario: String = "", val clave: String = "", val cargando: Boolean = false,
    val restaurando: Boolean = true, val error: String? = null, val sesion: Sesion? = null,
)
class SesionViewModel(private val repo: AuthRepository, private val estado: EstadoSesion) : ViewModel() {
    private val mutable = MutableStateFlow(SesionUiState())
    val uiState = mutable.asStateFlow()
    init {
        viewModelScope.launch { estado.sesion.collect { sesion ->
            mutable.update { it.copy(sesion = sesion, clave = if (sesion == null) it.clave else "") }
        } }
        viewModelScope.launch { estado.aviso.collect { aviso ->
            if (aviso != null) mutable.update { it.copy(error = aviso, clave = "") }
        } }
        restaurar()
    }
    fun usuario(valor: String) { mutable.update { it.copy(usuario = valor, error = null) } }
    fun clave(valor: String) { mutable.update { it.copy(clave = valor, error = null) } }
    fun restaurar() {
        viewModelScope.launch {
            mutable.update { it.copy(restaurando = true) }
            try { repo.sesionGuardada() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(error = mensaje(e)) } }
            finally { mutable.update { it.copy(restaurando = false) } }
        }
    }
    fun iniciar() {
        val actual = mutable.value
        if (actual.cargando) return
        if (actual.usuario.isBlank() || actual.clave.isBlank()) {
            mutable.update { it.copy(error = "Ingresa usuario y contraseña.") }; return
        }
        mutable.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try { repo.iniciarSesion(Credenciales(actual.usuario, actual.clave)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(error = mensaje(e)) } }
            finally { mutable.update { it.copy(cargando = false, clave = "") } }
        }
    }
    fun cerrar() {
        viewModelScope.launch {
            try { repo.cerrarSesion(); mutable.value = SesionUiState(restaurando = false) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(error = mensaje(e)) } }
        }
    }
    private fun mensaje(e: Exception): String = when ((e as? SesionException)?.error) {
        ErrorSesion.CredencialesInvalidas -> "Usuario o contraseña incorrectos."
        ErrorSesion.SesionExpirada -> "Tu sesión expiró. Inicia sesión nuevamente."
        ErrorSesion.SinPermiso -> "No tienes permisos para esta operación."
        ErrorSesion.SinConexion -> "Sin conexión con PharmaSoft. Intenta nuevamente."
        ErrorSesion.Almacenamiento -> "No se pudo acceder al almacén seguro."
        else -> "No se pudo completar la solicitud."
    }
}
