package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ActualizarProductoUseCase(private val repository: ProductoRepository) {
    suspend operator fun invoke(
        id: Long,
        nombre: String,
        precio: String,
        stock: String,
        activo: Boolean = true,
    ): Result<Producto> {
        val datos = validarProducto(nombre, precio, stock)
            .getOrElse { return Result.failure(it) }
        return resultadoDe {
            repository.actualizar(
                Producto(
                    id = id,
                    nombre = datos.nombre,
                    precio = datos.precio,
                    stock = datos.stock,
                    activo = activo,
                ),
            )
        }
    }
}
