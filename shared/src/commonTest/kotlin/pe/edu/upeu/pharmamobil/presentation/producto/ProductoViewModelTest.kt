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
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

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
    fun muestraLaValidacionDebajoDelCampoNombre() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.cambiarNombre("A")
        viewModel.cambiarPrecio("12.50")
        viewModel.cambiarStock("4")
        viewModel.guardar()
        advanceUntilIdle()

        val formulario = viewModel.uiState.value.formulario
        assertEquals("El nombre debe tener entre 3 y 150 caracteres", formulario.nombreError)
        assertNull(formulario.precioError)
        assertIs<ProductoOperacion.Inactiva>(viewModel.uiState.value.operacion)
    }

    @Test
    fun creaProductoYRecargaElListado() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.cambiarNombre("Naproxeno")
        viewModel.cambiarPrecio("12.90")
        viewModel.cambiarStock("4")
        viewModel.guardar()
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        val fase = assertIs<ProductoFase.ConProductos>(estado.fase)
        assertEquals(6, fase.productos.size)
        assertEquals("Naproxeno", fase.productos.last().nombre)
        assertEquals("Producto creado correctamente.", estado.mensajeExito)
        assertIs<ProductoOperacion.Inactiva>(estado.operacion)
    }

    @Test
    fun actualizaProductoYRecargaElListado() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        val original = productosDe(viewModel).first()

        viewModel.iniciarEdicion(original)
        viewModel.cambiarNombre("Paracetamol Forte")
        viewModel.cambiarPrecio("19.90")
        viewModel.guardar()
        advanceUntilIdle()

        val actualizado = productosDe(viewModel).first { it.id == original.id }
        assertEquals("Paracetamol Forte", actualizado.nombre)
        assertEquals(19.90, actualizado.precio)
        assertEquals("Producto actualizado correctamente.", viewModel.uiState.value.mensajeExito)
    }

    @Test
    fun eliminaProductoYRecargaElListado() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        val id = productosDe(viewModel).first().id

        viewModel.eliminar(id)
        advanceUntilIdle()

        val productos = productosDe(viewModel)
        assertEquals(4, productos.size)
        assertEquals(false, productos.any { it.id == id })
        assertEquals("Producto eliminado correctamente.", viewModel.uiState.value.mensajeExito)
    }

    private fun productosDe(viewModel: ProductoViewModel) =
        assertIs<ProductoFase.ConProductos>(viewModel.uiState.value.fase).productos

    private fun crearViewModel(): ProductoViewModel {
        val repository = ProductoRepositoryEnMemoria(
            productosIniciales = productosInicialesPharmaMobil(),
            latenciaMillis = 0,
        )
        return ProductoViewModel(
            listarProductos = ListarProductosUseCase(repository),
            registrarProducto = RegistrarProductoUseCase(repository),
            actualizarProducto = ActualizarProductoUseCase(repository),
            eliminarProducto = EliminarProductoUseCase(repository),
        )
    }
}
