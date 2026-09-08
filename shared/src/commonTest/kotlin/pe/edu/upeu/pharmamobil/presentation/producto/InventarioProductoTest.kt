package pe.edu.upeu.pharmamobil.presentation.producto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import pe.edu.upeu.pharmamobil.domain.model.Producto

class InventarioProductoTest {

    @Test
    fun datosSimuladosCoincidenConLaGuia() {
        assertEquals(5, productosSimulados.size)
        assertEquals(
            listOf("Paracetamol", "Ibuprofeno", "Amoxicilina", "Loratadina", "Diclofenaco"),
            productosSimulados.map { it.nombre },
        )
    }

    @Test
    fun filtraProductosActivosEInactivos() {
        val activos = filtrarInventario(productosSimulados, FiltroInventario.ACTIVOS)
        val inactivos = filtrarInventario(productosSimulados, FiltroInventario.INACTIVOS)

        assertEquals(4, activos.size)
        assertEquals(listOf("Loratadina"), inactivos.map { it.nombre })
    }

    @Test
    fun bajoStockIncluyeCincoPeroNoUnInactivoConStockCero() {
        val bajoStock = filtrarInventario(productosSimulados, FiltroInventario.BAJO_STOCK)

        assertEquals(listOf("Amoxicilina", "Diclofenaco"), bajoStock.map { it.nombre })
        assertTrue(bajoStock.all { it.stock <= Producto.STOCK_MINIMO })
        assertFalse(bajoStock.any { it.nombre == "Loratadina" })
    }
}
