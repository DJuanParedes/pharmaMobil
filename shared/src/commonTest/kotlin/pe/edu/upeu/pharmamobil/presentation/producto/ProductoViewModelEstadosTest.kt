package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.model.Producto
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelEstadosTest {
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
    fun cargaExitosaPasaDeCargandoAConProductos() = runTest(dispatcher) {
        val repositorio = FakeProductoRepository(listOf(producto()), latenciaMillis = 1_000)
        val viewModel = crearViewModel(repositorio)

        runCurrent()
        assertIs<ProductoFase.Cargando>(viewModel.uiState.value.fase)

        advanceUntilIdle()
        val fase = assertIs<ProductoFase.ConProductos>(viewModel.uiState.value.fase)
        assertEquals(listOf("Paracetamol"), fase.productos.map(Producto::nombre))
    }

    @Test
    fun listadoVacioTerminaEnSinProductos() = runTest(dispatcher) {
        val viewModel = crearViewModel(FakeProductoRepository())

        advanceUntilIdle()

        assertIs<ProductoFase.SinProductos>(viewModel.uiState.value.fase)
    }

    @Test
    fun validacionDelServidorQuedaBajoElCampoSinCambiarLaFase() = runTest(dispatcher) {
        val repositorio = FakeProductoRepository(listOf(producto())).apply {
            errorAlRegistrar = ErrorApi.Validacion(
                mapOf("nombre" to "Ya existe un producto con este nombre"),
            )
        }
        val viewModel = crearViewModel(repositorio)
        advanceUntilIdle()

        viewModel.cambiarNombre("Paracetamol nuevo")
        viewModel.cambiarPrecio("8.50")
        viewModel.cambiarStock("10")
        viewModel.guardar()
        advanceUntilIdle()

        assertIs<ProductoFase.ConProductos>(viewModel.uiState.value.fase)
        assertEquals("Ya existe un producto con este nombre", viewModel.uiState.value.formulario.nombreError)
        assertIs<ProductoOperacion.Inactiva>(viewModel.uiState.value.operacion)
    }

    @Test
    fun eliminacionPasaPorEnCursoVuelveAInactivaYRecarga() = runTest(dispatcher) {
        val repositorio = FakeProductoRepository(listOf(producto()))
        val viewModel = crearViewModel(repositorio)
        advanceUntilIdle()
        repositorio.latenciaMillis = 1_000

        viewModel.eliminar(1)
        runCurrent()

        val operacion = assertIs<ProductoOperacion.EnCurso>(viewModel.uiState.value.operacion)
        assertEquals(ProductoOperacion.Tipo.Eliminar, operacion.tipo)
        assertEquals(1, operacion.productoId)

        advanceUntilIdle()
        assertIs<ProductoOperacion.Inactiva>(viewModel.uiState.value.operacion)
        assertIs<ProductoFase.SinProductos>(viewModel.uiState.value.fase)
        assertEquals(2, repositorio.llamadasListar)
    }

    @Test
    fun cancelarElViewModelNoConvierteCancellationExceptionEnError() = runTest(dispatcher) {
        val repositorio = FakeProductoRepository(listOf(producto()))
        val viewModel = crearViewModel(repositorio)
        advanceUntilIdle()
        repositorio.latenciaMillis = 10_000

        viewModel.eliminar(1)
        runCurrent()
        assertIs<ProductoOperacion.EnCurso>(viewModel.uiState.value.operacion)

        viewModel.viewModelScope.cancel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.operacion !is ProductoOperacion.Fallida)
        assertNull(viewModel.uiState.value.mensajeExito)
    }

    private fun crearViewModel(repositorio: FakeProductoRepository) = ProductoViewModel(
        listarProductos = ListarProductosUseCase(repositorio),
        registrarProducto = RegistrarProductoUseCase(repositorio),
        actualizarProducto = ActualizarProductoUseCase(repositorio),
        eliminarProducto = EliminarProductoUseCase(repositorio),
    )

    private fun producto() = Producto(
        id = 1,
        nombre = "Paracetamol",
        precio = 8.50,
        stock = 20,
    )
}
