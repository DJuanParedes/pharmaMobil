package pe.edu.upeu.pharmamobil.domain.repository

import pe.edu.upeu.pharmamobil.domain.model.Producto

interface ProductoRepository {
    suspend fun registrar(
        nombre: String,
        precio: Double,
        stock: Int,
    ): Producto

    suspend fun listar(): List<Producto>
}
