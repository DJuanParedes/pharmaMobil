package pe.edu.upeu.pharmamobil.domain.service

import pe.edu.upeu.pharmamobil.domain.model.Producto

class ProductoService {
    fun productosConStock(productos: List<Producto>): List<Producto> =
        productos.filter { it.stock > 0 }

    fun nombresDeProductos(productos: List<Producto>): List<String> =
        productos.map { it.nombre }

    fun buscarPorId(productos: List<Producto>, id: Long): Producto? =
        productos.find { it.id == id }
}
