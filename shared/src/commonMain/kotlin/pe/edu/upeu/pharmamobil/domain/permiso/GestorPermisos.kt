package pe.edu.upeu.pharmamobil.domain.permiso

enum class Permiso { CAMARA, UBICACION, NOTIFICACIONES }

sealed interface EstadoPermiso {
    data object Concedido : EstadoPermiso
    /** Se puede volver a pedir. */
    data object Denegado : EstadoPermiso
    /** El sistema ya no muestra el diálogo: solo queda Ajustes. */
    data object DenegadoParaSiempre : EstadoPermiso
}

/**
 * Declarado en el código común porque la implementación de Android necesita la Activity
 * para lanzar el diálogo del sistema y el código común no puede nombrarla.
 */
interface GestorPermisos {
    suspend fun estado(permiso: Permiso): EstadoPermiso
    suspend fun solicitar(permiso: Permiso): EstadoPermiso
    fun abrirAjustes()
}
