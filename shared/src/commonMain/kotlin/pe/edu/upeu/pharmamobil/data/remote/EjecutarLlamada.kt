package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import pe.edu.upeu.pharmamobil.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException

suspend fun <T> ejecutarLlamada(bloque: suspend () -> T): Result<T> = try {
    Result.success(bloque())
} catch (cancelacion: CancellationException) {
    throw cancelacion
} catch (error: ClientRequestException) {
    Result.failure(ErrorApiException(traducirCliente(error)))
} catch (_: ServerResponseException) {
    Result.failure(ErrorApiException(ErrorApi.Servidor))
} catch (_: HttpRequestTimeoutException) {
    Result.failure(ErrorApiException(ErrorApi.TiempoAgotado))
} catch (_: SerializationException) {
    Result.failure(ErrorApiException(ErrorApi.RespuestaInvalida))
} catch (_: IOException) {
    Result.failure(ErrorApiException(ErrorApi.SinConexion))
}

private suspend fun traducirCliente(error: ClientRequestException): ErrorApi {
    val cuerpo = runCatching { error.response.body<ErrorResponseDto>() }.getOrNull()
    return when (error.response.status.value) {
        400 -> ErrorApi.Validacion(cuerpo?.validationErrors.orEmpty())
        401, 403 -> ErrorApi.NoAutorizado
        404 -> ErrorApi.NoEncontrado
        409 -> ErrorApi.Conflicto(cuerpo?.message?.takeIf(String::isNotBlank) ?: "Operación no permitida")
        else -> ErrorApi.Servidor
    }
}
