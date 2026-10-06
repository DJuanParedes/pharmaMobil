package pe.edu.upeu.pharmamobil.presentation.producto

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.platform.formatearSoles

data class ProductoUi(
    val id: Long,
    val nombre: String,
    val precio: String,
    val stock: Int,
    val activo: Boolean,
    val requiereReposicion: Boolean,
)

fun Producto.toUi(): ProductoUi = ProductoUi(
    id = id,
    nombre = nombre,
    precio = formatearSoles(precio),
    stock = stock,
    activo = activo,
    requiereReposicion = requiereReposicion(),
)
