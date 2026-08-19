package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.model.Producto

class ObservarProductosUseCase(
    private val repository: ProductoRepository
) {
    operator fun invoke(): Flow<List<Producto>> = repository.observarProductos()
}
