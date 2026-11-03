package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.error.mensajeDe
import pe.edu.upeu.pharmamobil.domain.model.FilaPendiente
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio
import pe.edu.upeu.pharmamobil.domain.repository.ProductoEnvioRepository
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Traduce cada error de la API a Transitorio o Permanente. Es la capa de datos,
 * donde ya vive la traducción de errores de la sesión 8.
 */
class EnvioRepositorioRest(private val rest: ProductoRepository) : ProductoEnvioRepository {
    override suspend fun enviar(fila: FilaPendiente): ResultadoEnvio = try {
        val p = fila.producto
        ResultadoEnvio.Exito(rest.registrar(p.nombre, p.precio, p.stock))
    } catch (e: ErrorApiException) {
        e.error.aResultado()
    }

    override suspend fun listar(): List<Producto> = rest.listar()
}

internal fun ErrorApi.aResultado(): ResultadoEnvio = when (this) {
    ErrorApi.SinConexion, ErrorApi.TiempoAgotado, ErrorApi.Servidor ->
        ResultadoEnvio.Transitorio(mensajeDe(this))
    is ErrorApi.Validacion -> ResultadoEnvio.Permanente(
        porCampo.entries.joinToString("; ") { "${it.key}: ${it.value}" }.ifBlank { mensajeDe(this) })
    ErrorApi.NoEncontrado, is ErrorApi.Conflicto, ErrorApi.SinPermiso, ErrorApi.NoAutorizado,
    ErrorApi.RespuestaInvalida -> ResultadoEnvio.Permanente(mensajeDe(this))
    ErrorApi.SesionExpirada -> ResultadoEnvio.Permanente("Sesión vencida")
}
