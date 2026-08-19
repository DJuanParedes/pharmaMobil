package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pe.edu.upeu.pharmamobil.domain.model.Producto

class ProductoRepositoryMemoria(
    productosRemotos: List<Producto>,
    private val latenciaMillis: Long = 1_000
) : ProductoRepository {
    private val productosRemotos = productosRemotos.toList()

    init {
        require(latenciaMillis >= 0) { "La latencia no puede ser negativa" }
    }

    override suspend fun obtenerProductos(): List<Producto> {
        delay(latenciaMillis)
        return productosRemotos.toList()
    }

    override fun observarProductos(): Flow<List<Producto>> = flow {
        emit(emptyList())
        emit(obtenerProductos())
    }
}
