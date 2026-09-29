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
        val datos = validarProducto(nombre, precio, stock)
            .getOrElse { return Result.failure(it) }
        return resultadoDe {
            repository.registrar(
                nombre = datos.nombre,
                precio = datos.precio,
                stock = datos.stock,
            )
        }
    }
}

internal data class DatosProductoValidos(
    val nombre: String,
    val precio: Double,
    val stock: Int,
)

internal fun validarProducto(nombre: String, precio: String, stock: String): Result<DatosProductoValidos> {
    val nombreNormalizado = nombre.trim()
    val precioNumerico = precio.toDoubleOrNull()
    val stockNumerico = stock.toIntOrNull()
    val error = when {
        nombreNormalizado.length !in 3..150 -> ValidacionProductoException(
            CampoProducto.NOMBRE,
            "El nombre debe tener entre 3 y 150 caracteres",
        )
        precioNumerico == null || precioNumerico < 0.01 -> ValidacionProductoException(
            CampoProducto.PRECIO,
            "El precio debe ser mayor o igual a 0.01",
        )
        stockNumerico == null || stockNumerico < 0 -> ValidacionProductoException(
            CampoProducto.STOCK,
            "El stock debe ser un número entero no negativo",
        )
        else -> null
    }
    return if (error != null) {
        Result.failure(error)
    } else {
        Result.success(
            DatosProductoValidos(
                nombre = nombreNormalizado,
                precio = checkNotNull(precioNumerico),
                stock = checkNotNull(stockNumerico),
            ),
        )
    }
}
