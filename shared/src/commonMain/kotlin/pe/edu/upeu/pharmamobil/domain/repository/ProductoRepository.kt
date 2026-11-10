package pe.edu.upeu.pharmamobil.domain.repository

import pe.edu.upeu.pharmamobil.domain.model.Producto

interface ProductoRepository {
    // Compatibilidad con los repositorios de las sesiones anteriores.
    fun observar(): kotlinx.coroutines.flow.Flow<List<Producto>> = kotlinx.coroutines.flow.flow { emit(listar()) }
    suspend fun sincronizar() { listar() }

    suspend fun registrar(
        nombre: String,
        precio: Double,
        stock: Int,
    ): Producto

    suspend fun listar(): List<Producto>

    suspend fun obtener(id: Long): Producto

    suspend fun actualizar(producto: Producto): Producto

    suspend fun eliminar(id: Long)
}
