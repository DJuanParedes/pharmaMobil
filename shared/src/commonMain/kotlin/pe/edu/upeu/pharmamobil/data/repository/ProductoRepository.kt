package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Producto

interface ProductoRepository {
    suspend fun obtenerProductos(): List<Producto>

    fun observarProductos(): Flow<List<Producto>>
}
