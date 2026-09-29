package pe.edu.upeu.pharmamobil.domain.error

sealed interface ErrorApi {
    data class Validacion(val porCampo: Map<String, String>) : ErrorApi
    data object NoEncontrado : ErrorApi
    data class Conflicto(val mensaje: String) : ErrorApi
    data object NoAutorizado : ErrorApi
    data object Servidor : ErrorApi
    data object SinConexion : ErrorApi
    data object TiempoAgotado : ErrorApi
    data object RespuestaInvalida : ErrorApi
}

class ErrorApiException(val error: ErrorApi) : Exception()

fun mensajeDe(error: ErrorApi?): String = when (error) {
    is ErrorApi.Validacion -> "Revisa los datos ingresados."
    ErrorApi.NoEncontrado -> "El producto ya no existe. Actualiza la lista."
    is ErrorApi.Conflicto -> error.mensaje
    ErrorApi.NoAutorizado -> "No tienes autorización para completar esta operación."
    ErrorApi.Servidor -> "El servidor no pudo completar la solicitud. Intenta nuevamente."
    ErrorApi.SinConexion -> "No se pudo conectar con PharmaSoft. Verifica la red y el backend."
    ErrorApi.TiempoAgotado -> "La operación tardó demasiado. Intenta nuevamente."
    ErrorApi.RespuestaInvalida -> "El servidor devolvió una respuesta inesperada."
    null -> "No se pudo completar la operación."
}
