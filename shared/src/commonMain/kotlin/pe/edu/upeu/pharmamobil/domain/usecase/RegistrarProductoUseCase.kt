package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

enum class CampoProducto {
    NOMBRE,
    PRECIO,
    STOCK,
}

class ValidacionProductoException(
    val campo: CampoProducto,
    override val message: String,
) : IllegalArgumentException(message)

class RegistrarProductoUseCase(
    private val repository: ProductoRepository,
) {
    suspend operator fun invoke(
        nombre: String,
        precio: String,
        stock: String,
    ): Result<Producto> {
        val nombreNormalizado = nombre.trim()
        val precioNumerico = precio.toDoubleOrNull()
        val stockNumerico = stock.toIntOrNull()

        val error = when {
            nombreNormalizado.isBlank() -> ValidacionProductoException(
                CampoProducto.NOMBRE,
                "Nombre obligatorio",
            )

            precioNumerico == null -> ValidacionProductoException(
                CampoProducto.PRECIO,
                "Precio inválido",
            )

            precioNumerico <= 0.0 -> ValidacionProductoException(
                CampoProducto.PRECIO,
                "El precio debe ser mayor a 0",
            )

            stockNumerico == null -> ValidacionProductoException(
                CampoProducto.STOCK,
                "Stock debe ser un número entero",
            )

            stockNumerico < 0 -> ValidacionProductoException(
                CampoProducto.STOCK,
                "Stock no puede ser negativo",
            )

            else -> null
        }

        if (error != null) return Result.failure(error)

        return runCatching {
            repository.registrar(
                nombre = nombreNormalizado,
                precio = checkNotNull(precioNumerico),
                stock = checkNotNull(stockNumerico),
            )
        }
    }
}
