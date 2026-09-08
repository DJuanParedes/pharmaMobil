package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RegistrarProductoUseCaseTest {
    private val repository = ProductoRepositoryPrueba()
    private val useCase = RegistrarProductoUseCase(repository)

    @Test
    fun registraProductoValidoYElRepositorioAsignaElId() = runTest {
        val resultado = useCase("  Paracetamol 500 mg  ", "8.50", "100")

        val producto = resultado.getOrThrow()
        assertEquals(1L, producto.id)
        assertEquals("Paracetamol 500 mg", producto.nombre)
        assertEquals(8.50, producto.precio)
        assertEquals(100, producto.stock)
        assertEquals(listOf(producto), repository.listar())
    }

    @Test
    fun rechazaNombreVacio() = runTest {
        verificarError("", "8.50", "100", CampoProducto.NOMBRE, "Nombre obligatorio")
    }

    @Test
    fun rechazaPrecioConTexto() = runTest {
        verificarError("Ibuprofeno", "abc", "50", CampoProducto.PRECIO, "Precio inválido")
    }

    @Test
    fun rechazaPrecioIgualACero() = runTest {
        verificarError(
            "Ibuprofeno",
            "0",
            "50",
            CampoProducto.PRECIO,
            "El precio debe ser mayor a 0",
        )
    }

    @Test
    fun rechazaStockConTexto() = runTest {
        verificarError(
            "Amoxicilina",
            "18.50",
            "abc",
            CampoProducto.STOCK,
            "Stock debe ser un número entero",
        )
    }

    @Test
    fun rechazaStockNegativo() = runTest {
        verificarError(
            "Amoxicilina",
            "18.50",
            "-5",
            CampoProducto.STOCK,
            "Stock no puede ser negativo",
        )
    }

    @Test
    fun permiteStockIgualACero() = runTest {
        val resultado = useCase("Loratadina", "10", "0")

        assertTrue(resultado.isSuccess)
        assertEquals(0, resultado.getOrThrow().stock)
    }

    private suspend fun verificarError(
        nombre: String,
        precio: String,
        stock: String,
        campoEsperado: CampoProducto,
        mensajeEsperado: String,
    ) {
        val resultado = useCase(nombre, precio, stock)
        val error = assertIs<ValidacionProductoException>(resultado.exceptionOrNull())

        assertEquals(campoEsperado, error.campo)
        assertEquals(mensajeEsperado, error.message)
    }
}

private class ProductoRepositoryPrueba : ProductoRepository {
    private val productos = mutableListOf<Producto>()
    private var siguienteId = 1L

    override suspend fun registrar(
        nombre: String,
        precio: Double,
        stock: Int,
    ): Producto = Producto(
        id = siguienteId++,
        nombre = nombre,
        precio = precio,
        stock = stock,
    ).also(productos::add)

    override suspend fun listar(): List<Producto> = productos.toList()
}
