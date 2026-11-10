package pe.edu.upeu.pharmamobil.data.local
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.FilaPendiente
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoLocalRepository

class ProductoLocal(base: PharmaDb) : ProductoLocalRepository {
    private val consultas = base.productoQueries
    fun observar(): Flow<List<Producto>> = consultas.listarTodos().asFlow()
        .mapToList(Dispatchers.Default).map { filas -> filas.map { it.aDominio() } }
    fun listar(): List<Producto> = consultas.listarTodos().executeAsList().map { it.aDominio() }
    fun pendientes(): List<Producto> = consultas.listarPendientes().executeAsList().map { it.aDominio() }
    fun obtener(id: Long): Producto? = consultas.obtener(id).executeAsOneOrNull()?.aDominio()
    fun guardarPendiente(producto: Producto): Producto = consultas.transactionWithResult {
        val id = minOf(consultas.siguienteIdLocal().executeAsOne(), 0L) - 1L
        producto.copy(id = id, pendiente = true).also(::guardar)
    }
    fun guardar(p: Producto) = consultas.insertar(p.id, p.nombre, p.precio, p.stock.toLong(),
        if (p.pendiente) 1L else 0L, if (p.activo) 1L else 0L, 0L, null)
    fun eliminar(id: Long) = consultas.eliminar(id)
    override fun confirmar(idLocal: Long, remoto: Producto) = consultas.transaction {
        require(remoto.id > 0) { "El servidor debe asignar un id positivo" }
        consultas.eliminar(idLocal)
        guardar(remoto.copy(pendiente = false))
    }
    fun reemplazarSincronizados(productos: List<Producto>) = consultas.transaction {
        consultas.borrarSincronizados()
        productos.forEach { guardar(it.copy(pendiente = false)) }
    }

    // --- Sesión 13: cola de envío con intentos y último error ---
    override fun pendientes(maxIntentos: Int): List<FilaPendiente> =
        consultas.pendientesPorEnviar(maxIntentos.toLong()).executeAsList().map { it.aFila() }
    override fun fallidas(maxIntentos: Int): List<FilaPendiente> =
        consultas.listarFallidas(maxIntentos.toLong()).executeAsList().map { it.aFila() }
    override fun sumarIntento(id: Long, motivo: String) { consultas.sumarIntento(motivo, id) }
    override fun marcarFallida(id: Long, motivo: String, maxIntentos: Int) {
        consultas.marcarFallida(maxIntentos.toLong(), motivo, id)
    }
    override fun reintentarFallidas(maxIntentos: Int) { consultas.reintentarFallidas(maxIntentos.toLong()) }
    override fun conciliar(productos: List<Producto>) = reemplazarSincronizados(productos)
    override fun contarPendientes(maxIntentos: Int): Int =
        consultas.contarPendientes(maxIntentos.toLong()).executeAsOne().toInt()
    override fun observarPendientes(maxIntentos: Int): Flow<Int> =
        consultas.contarPendientes(maxIntentos.toLong()).asFlow().mapToOne(Dispatchers.Default).map { it.toInt() }
    override fun observarFallidas(maxIntentos: Int): Flow<Int> =
        consultas.contarFallidas(maxIntentos.toLong()).asFlow().mapToOne(Dispatchers.Default).map { it.toInt() }
    override fun registrarSincronizacion(epochMillis: Long) { consultas.guardarMeta(CLAVE_ULTIMA_SYNC, epochMillis) }
    override fun observarUltimaSincronizacion(): Flow<Long?> =
        consultas.leerMeta(CLAVE_ULTIMA_SYNC).asFlow().mapToOneOrNull(Dispatchers.Default)

    /** Sesión 14 (hueco 7): borra inventario, cola y fecha de sincronización del usuario saliente. */
    fun limpiar() = consultas.transaction {
        consultas.borrarTodo()
        consultas.borrarMeta()
    }

    private fun pe.edu.upeu.pharmamobil.db.Producto.aDominio() =
        Producto(id, nombre, precio, stock.toInt(), activo != 0L, pendiente != 0L)
    private fun pe.edu.upeu.pharmamobil.db.Producto.aFila() =
        FilaPendiente(aDominio(), intentos.toInt(), ultimoError)

    private companion object { const val CLAVE_ULTIMA_SYNC = "ultima_sincronizacion" }
}
