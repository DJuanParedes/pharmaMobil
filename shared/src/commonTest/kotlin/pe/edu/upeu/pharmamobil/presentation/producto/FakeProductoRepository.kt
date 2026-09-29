package pe.edu.upeu.pharmamobil.presentation.producto

import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class FakeProductoRepository(
    productos: List<Producto> = emptyList(),
    var latenciaMillis: Long = 0,
) : ProductoRepository {
    private val datos = productos.toMutableList()

    var errorAlListar: ErrorApi? = null
    var errorAlRegistrar: ErrorApi? = null
    var errorAlActualizar: ErrorApi? = null
    var errorAlEliminar: ErrorApi? = null
    var llamadasListar: Int = 0
        private set

    override suspend fun registrar(nombre: String, precio: Double, stock: Int): Producto {
        esperar(errorAlRegistrar)
        val producto = Producto(
            id = (datos.maxOfOrNull(Producto::id) ?: 0) + 1,
            nombre = nombre,
            precio = precio,
            stock = stock,
        )
        datos += producto
        return producto
    }

    override suspend fun listar(): List<Producto> {
        llamadasListar += 1
        esperar(errorAlListar)
        return datos.toList()
    }

    override suspend fun obtener(id: Long): Producto {
        esperar(null)
        return datos.firstOrNull { it.id == id }
            ?: throw ErrorApiException(ErrorApi.NoEncontrado)
    }

    override suspend fun actualizar(producto: Producto): Producto {
        esperar(errorAlActualizar)
        val indice = datos.indexOfFirst { it.id == producto.id }
        if (indice < 0) throw ErrorApiException(ErrorApi.NoEncontrado)
        datos[indice] = producto
        return producto
    }

    override suspend fun eliminar(id: Long) {
        esperar(errorAlEliminar)
        val eliminado = datos.removeAll { it.id == id }
        if (!eliminado) throw ErrorApiException(ErrorApi.NoEncontrado)
    }

    private suspend fun esperar(error: ErrorApi?) {
        if (latenciaMillis > 0) delay(latenciaMillis)
        if (error != null) throw ErrorApiException(error)
    }
}
