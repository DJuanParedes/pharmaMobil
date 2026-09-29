package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobil.data.remote.dto.toDomain
import pe.edu.upeu.pharmamobil.data.remote.dto.toRequest
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ProductoRepositorioRest(
    private val api: ProductoApi,
    private val categoriaPorDefecto: Long,
) : ProductoRepository {
    override suspend fun listar(): List<Producto> =
        ejecutarLlamada { api.listar().contenido.map { it.toDomain() } }.getOrThrow()

    override suspend fun obtener(id: Long): Producto =
        ejecutarLlamada { api.obtener(id).toDomain() }.getOrThrow()

    override suspend fun registrar(nombre: String, precio: Double, stock: Int): Producto =
        ejecutarLlamada {
            val nuevo = Producto(id = 1L, nombre = nombre, precio = precio, stock = stock)
            api.crear(nuevo.toRequest(categoriaPorDefecto)).toDomain()
        }.getOrThrow()

    override suspend fun actualizar(producto: Producto): Producto =
        ejecutarLlamada {
            api.actualizar(producto.id, producto.toRequest(categoriaPorDefecto)).toDomain()
        }.getOrThrow()

    override suspend fun eliminar(id: Long) {
        ejecutarLlamada { api.eliminar(id) }.getOrThrow()
    }
}
