package pe.edu.upeu.pharmamobil.domain.model

sealed interface ErrorSesion {
    data object CredencialesInvalidas : ErrorSesion
    data object SesionExpirada : ErrorSesion
    data object SinPermiso : ErrorSesion
    data object SinConexion : ErrorSesion
    data object Servidor : ErrorSesion
    data object Almacenamiento : ErrorSesion
}

class SesionException(val error: ErrorSesion) : Exception(error.toString())
