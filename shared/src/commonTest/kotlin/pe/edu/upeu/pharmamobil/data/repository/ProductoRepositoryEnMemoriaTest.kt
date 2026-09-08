package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductoRepositoryEnMemoriaTest {
    @Test
    fun asignaIdConsecutivoDentroDelRepositorio() = runTest {
        val repository = ProductoRepositoryEnMemoria(
            productosIniciales = listOf(
                Producto(id = 8L, nombre = "Inicial", precio = 10.0, stock = 2),
            ),
            latenciaMillis = 0,
        )

        val registrado = repository.registrar(
            nombre = "Naproxeno",
            precio = 12.90,
            stock = 4,
        )

        assertEquals(9L, registrado.id)
        assertEquals(listOf(8L, 9L), repository.listar().map(Producto::id))
    }

    @Test
    fun listarEntregaUnaCopiaDelInventario() = runTest {
        val repository = ProductoRepositoryEnMemoria(
            productosIniciales = productosInicialesPharmaMobil(),
            latenciaMillis = 0,
        )

        val primeraLectura = repository.listar()
        repository.registrar("Vitamina C", 9.50, 20)

        assertEquals(5, primeraLectura.size)
        assertEquals(6, repository.listar().size)
    }
}
