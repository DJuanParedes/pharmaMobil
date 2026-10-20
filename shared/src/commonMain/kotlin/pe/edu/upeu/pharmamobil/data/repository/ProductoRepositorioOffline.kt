package pe.edu.upeu.pharmamobil.data.repository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException

class ProductoRepositorioOffline(
    private val local: ProductoLocal,
    private val remoto: ProductoRepository,
) : ProductoRepository {
    private val mutex = Mutex()
    override fun observar() = local.observar()
    override suspend fun listar() = local.listar()
    override suspend fun obtener(id: Long) = local.obtener(id)
        ?: throw ErrorApiException(ErrorApi.NoEncontrado)
    override suspend fun registrar(nombre: String, precio: Double, stock: Int): Producto {
        val guardado = mutex.withLock { local.guardarPendiente(Producto(-1, nombre, precio, stock)) }
        try { sincronizar() } catch (e: ErrorApiException) {
            if (e.error != ErrorApi.SinConexion && e.error != ErrorApi.TiempoAgotado) throw e
        }
        return guardado
    }
    override suspend fun sincronizar() = mutex.withLock {
        local.pendientes().forEach { pendiente ->
            val confirmado = remoto.registrar(pendiente.nombre, pendiente.precio, pendiente.stock)
            local.confirmar(pendiente.id, confirmado)
        }
        // Una descarga fallida conserva la última lista y las altas ya confirmadas.
        val productos = remoto.listar()
        local.reemplazarSincronizados(productos)
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

