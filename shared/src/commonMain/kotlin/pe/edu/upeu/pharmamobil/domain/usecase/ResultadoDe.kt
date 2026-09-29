package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.CancellationException

suspend inline fun <T> resultadoDe(crossinline bloque: suspend () -> T): Result<T> = try {
    Result.success(bloque())
} catch (cancelacion: CancellationException) {
    throw cancelacion
} catch (error: Throwable) {
    Result.failure(error)
}
