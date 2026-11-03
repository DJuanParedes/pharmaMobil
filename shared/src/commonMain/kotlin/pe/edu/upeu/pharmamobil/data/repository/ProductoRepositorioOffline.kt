package pe.edu.upeu.pharmamobil.data.repository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.ResumenSync
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarUseCase

class ProductoRepositorioOffline(
    private val local: ProductoLocal,
    private val remoto: ProductoRepository,
    private val sincronizacion: SincronizarUseCase = SincronizarUseCase(local, EnvioRepositorioRest(remoto)),
) : ProductoRepository {
    private val mutex = Mutex()
    override fun observar() = local.observar()
    override suspend fun listar() = local.listar()
    override suspend fun obtener(id: Long) = local.obtener(id)
        ?: throw ErrorApiException(ErrorApi.NoEncontrado)
    override suspend fun registrar(nombre: String, precio: Double, stock: Int): Producto {
        val guardado = mutex.withLock { local.guardarPendiente(Producto(-1, nombre, precio, stock)) }
        try { sincronizar() } catch (e: ErrorApiException) {
            // El producto ya está guardado: un fallo transitorio de red o del servidor no se relanza.
            if (e.error !in listOf(ErrorApi.SinConexion, ErrorApi.TiempoAgotado, ErrorApi.Servidor)) throw e
        }
        return guardado
    }
    /** Sesión 13: delega en el caso de uso con intentos por fila y espera creciente. */
    suspend fun sincronizarConResumen(): ResumenSync = mutex.withLock { sincronizacion() }

    override suspend fun sincronizar() {
        // Una descarga fallida conserva la última lista y las altas ya confirmadas.
        sincronizarConResumen().errorDescarga?.let { throw ErrorApiException(it) }
    }
    override suspend fun actualizar(producto: Producto): Producto = mutex.withLock {
        if (producto.id < 0) producto.copy(pendiente = true).also(local::guardar)
        else remoto.actualizar(producto).also(local::guardar)
    }
    override suspend fun eliminar(id: Long) = mutex.withLock {
        if (id > 0) remoto.eliminar(id)
        local.eliminar(id); Unit
    }
}

