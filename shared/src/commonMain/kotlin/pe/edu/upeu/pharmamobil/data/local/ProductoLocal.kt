package pe.edu.upeu.pharmamobil.data.local
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto

class ProductoLocal(base: PharmaDb) {
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
        if (p.pendiente) 1L else 0L, if (p.activo) 1L else 0L)
    fun eliminar(id: Long) = consultas.eliminar(id)
    fun confirmar(idLocal: Long, remoto: Producto) = consultas.transaction {
        require(remoto.id > 0) { "El servidor debe asignar un id positivo" }
        consultas.eliminar(idLocal)
        guardar(remoto.copy(pendiente = false))
    }
    fun reemplazarSincronizados(productos: List<Producto>) = consultas.transaction {
        consultas.borrarSincronizados()
        productos.forEach { guardar(it.copy(pendiente = false)) }
    }
    private fun pe.edu.upeu.pharmamobil.db.Producto.aDominio() =
        Producto(id, nombre, precio, stock.toInt(), activo != 0L, pendiente != 0L)
}
