package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ProductoRepositoryEnMemoria(
    productosIniciales: List<Producto> = productosInicialesPharmaMobil(),
    private val latenciaMillis: Long = 500,
) : ProductoRepository {
    private val mutex = Mutex()
    private val productos = productosIniciales.toMutableList()
    private var siguienteId = (productosIniciales.maxOfOrNull(Producto::id) ?: 0L) + 1L

    init {
        require(latenciaMillis >= 0) { "La latencia no puede ser negativa" }
    }

    override suspend fun registrar(
        nombre: String,
        precio: Double,
        stock: Int,
    ): Producto {
        delay(latenciaMillis)
        return mutex.withLock {
            val producto = Producto(
                id = siguienteId++,
                nombre = nombre,
                precio = precio,
                stock = stock,
            )
            productos += producto
            producto
        }
    }

    override suspend fun listar(): List<Producto> {
        delay(latenciaMillis)
        return mutex.withLock { productos.toList() }
    }
}

fun productosInicialesPharmaMobil(): List<Producto> = listOf(
    Producto(id = 1L, nombre = "Paracetamol", precio = 15.50, stock = 100),
    Producto(id = 2L, nombre = "Ibuprofeno", precio = 18.90, stock = 50),
    Producto(id = 3L, nombre = "Amoxicilina", precio = 25.00, stock = 5),
    Producto(id = 4L, nombre = "Loratadina", precio = 12.50, stock = 0, activo = false),
    Producto(id = 5L, nombre = "Diclofenaco", precio = 20.00, stock = 3),
)
