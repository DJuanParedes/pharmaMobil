package pe.edu.upeu.pharmamobil

import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import java.io.File

@RunWith(AndroidJUnit4::class)
class OfflineSesion11Test {
    private val inst get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(inst)
    private val koin get() = GlobalContext.get()
    private fun argumento(nombre: String) = requireNotNull(InstrumentationRegistry.getArguments().getString(nombre))
    private fun abrirProductos() {
        requireNotNull(device.wait(Until.findObject(By.desc("Abrir menú")), 45_000)).click()
        requireNotNull(device.wait(Until.findObject(By.text("Productos")), 10_000)).click()
        assertNotNull(device.wait(Until.findObject(By.text("Inventario offline-first")), 15_000))
        device.waitForIdle()
    }
    private fun captura(nombre: String) {
        val archivo = File(inst.targetContext.getExternalFilesDir("s11"), "$nombre.png")
        archivo.parentFile!!.mkdirs()
        assertTrue(device.takeScreenshot(archivo))
        val fd = inst.uiAutomation.executeShellCommand("mkdir -p /sdcard/Download/S11; cp ${archivo.absolutePath} /sdcard/Download/S11/$nombre.png")
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes() }
    }
    @Test fun prepararConRed(): Unit = runBlocking {
        val auth = koin.get<AuthRepository>()
        auth.iniciarSesion(Credenciales("admin", argumento("adminPassword")))
        val repo = koin.get<ProductoRepository>()
        repo.sincronizar()
        assertTrue(repo.listar().isNotEmpty())
        ActivityScenario.launch(MainActivity::class.java).use {
            abrirProductos(); captura("01-listado-con-red")
        }
    }
    @Test fun abrirSinRedYRegistrar(): Unit = runBlocking {
        assertNotNull(koin.get<AuthRepository>().sesionGuardada())
        val repo = koin.get<ProductoRepository>()
        assertTrue(repo.listar().isNotEmpty())
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            abrirProductos()
            assertNotNull(device.wait(Until.findObject(By.text("Sin conexión: mostrando datos guardados")), 60_000))
            captura("02-reapertura-modo-avion")
            escenario.onActivity { actividad ->
                val store = actividad.viewModelStore
                val vm = store.keys().mapNotNull { store[it] as? ProductoViewModel }.first()
                vm.cambiarNombre(argumento("productoNombre")); vm.cambiarPrecio("4.50"); vm.cambiarStock("20"); vm.guardar()
            }
            assertNotNull(device.wait(Until.findObject(By.textContains("Pendientes de enviar:")), 60_000))
            val pendiente = koin.get<ProductoLocal>().pendientes().single { it.nombre == argumento("productoNombre") }
            assertTrue(pendiente.id < 0)
            captura("03-producto-pendiente")
        }
    }
    @Test fun reconectarYVerificarServidor(): Unit = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            abrirProductos()
            koin.get<ProductoRepository>().sincronizar()
            val local = koin.get<ProductoLocal>()
            assertTrue(local.pendientes().isEmpty())
            val producto = local.listar().single { it.nombre == argumento("productoNombre") }
            assertTrue(producto.id > 0)
            val remoto = koin.get<ProductoApi>().obtener(producto.id)
            assertEquals(producto.nombre, remoto.nombre)
            device.wait(Until.gone(By.textContains("Pendientes de enviar:")), 20_000)
            captura("04-reconexion-confirmada")
            println("VERIFICADO: id=${producto.id}; nombre=${producto.nombre}; pendiente=false; GET servidor=200")
        }
    }
}
