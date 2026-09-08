package pe.edu.upeu.pharmamobil.presentation.producto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositoryEnMemoria
import pe.edu.upeu.pharmamobil.data.repository.productosInicialesPharmaMobil
import pe.edu.upeu.pharmamobil.domain.usecase.CampoProducto
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
    }

    @Test
    fun cargaInventarioYExponeStateFlowDeSoloLectura() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        val fase = assertIs<ProductoFase.ConProductos>(viewModel.uiState.value.fase)
        assertEquals(5, fase.productos.size)
    }

    @Test
    fun muestraErrorDeCampoSinValidarEnLaPantalla() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.cambiarPrecio("12.50")
        viewModel.cambiarStock("4")
        viewModel.registrar()
        advanceUntilIdle()

        val formulario = viewModel.uiState.value.formulario
        assertEquals(CampoProducto.NOMBRE, formulario.campoConError)
        assertEquals("Nombre obligatorio", formulario.mensaje)
    }

    @Test
    fun registraProductoYSeleccionaFiltroBajoStock() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.cambiarNombre("Naproxeno")
        viewModel.cambiarPrecio("12.90")
        viewModel.cambiarStock("4")
        viewModel.registrar()
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        val fase = assertIs<ProductoFase.ConProductos>(estado.fase)
        assertEquals(6, fase.productos.size)
        assertEquals(FiltroInventario.BAJO_STOCK, estado.filtro)
        assertTrue(estado.formulario.registroExitoso)
        assertEquals(6L, estado.formulario.ultimoProductoRegistrado?.id)
    }

    private fun crearViewModel(): ProductoViewModel {
        val repository = ProductoRepositoryEnMemoria(
            productosIniciales = productosInicialesPharmaMobil(),
            latenciaMillis = 0,
        )
        return ProductoViewModel(
            registrarProducto = RegistrarProductoUseCase(repository),
            repository = repository,
        )
    }
}
