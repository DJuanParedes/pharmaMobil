package pe.edu.upeu.pharmamobil.presentation.detalle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositoryEnMemoria
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleProductoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val producto = Producto(1, "Paracetamol 500 mg", 15.5, 100)
    private val textos = mutableListOf<String>()
    private var falloNativo = false
    private val compartidor = object : Compartidor {
        override fun compartir(texto: String) {
            if (falloNativo) error("Selector no disponible")
            textos.add(texto)
        }
    }
    private fun viewModel() = DetalleProductoViewModel(
        ProductoRepositoryEnMemoria(listOf(producto)), compartidor,
    )
    @BeforeTest fun preparar() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun limpiar() { Dispatchers.resetMain() }

    @Test fun cargaDetalleConPrecioPresentableSinModificarDominio() = runTest(dispatcher) {
        val vm = viewModel()
        vm.cargar(1)
        advanceUntilIdle()
        val estado = assertIs<DetalleProductoUiState.ConProducto>(vm.uiState.value)
        assertEquals(producto, estado.producto)
        assertEquals(15.5, estado.producto.precio)
        assertTrue(estado.productoUi.precio.contains("S/"))
        assertTrue(estado.productoUi.precio.contains("15.50"))
    }

    @Test fun comparteElContratoDeTextoConSeparadoresExactos() = runTest(dispatcher) {
        val vm = viewModel()
        vm.cargar(1)
        advanceUntilIdle()
        vm.compartir()
        assertEquals(1, textos.size)
        val texto = textos.single().replace('\u00a0', ' ')
        assertEquals("Paracetamol 500 mg — S/ 15.50 · Stock: 100", texto)
    }

    @Test fun noComparteDuranteCarga() = runTest(dispatcher) {
        val vm = viewModel()
        vm.compartir()
        assertTrue(textos.isEmpty())
    }

    @Test fun falloNativoConservaProductoYSeLimpiaAlReintentar() = runTest(dispatcher) {
        val vm = viewModel()
        vm.cargar(1)
        advanceUntilIdle()
        falloNativo = true
        vm.compartir()
        val estado = assertIs<DetalleProductoUiState.ConProducto>(vm.uiState.value)
        assertEquals(producto, estado.producto)
        assertEquals("Selector no disponible", estado.errorCompartir)
        falloNativo = false
        vm.compartir()
        assertNull(assertIs<DetalleProductoUiState.ConProducto>(vm.uiState.value).errorCompartir)
        assertEquals(1, textos.size)
    }

    @Test fun productoInexistenteMuestraErrorYNoComparte() = runTest(dispatcher) {
        val vm = viewModel()
        vm.cargar(999)
        advanceUntilIdle()
        assertIs<DetalleProductoUiState.Error>(vm.uiState.value)
        vm.compartir()
        assertTrue(textos.isEmpty())
    }
}
