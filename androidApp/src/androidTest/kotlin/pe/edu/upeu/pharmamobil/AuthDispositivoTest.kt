package pe.edu.upeu.pharmamobil

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import java.io.File
import android.os.ParcelFileDescriptor
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro

@RunWith(AndroidJUnit4::class)
class AuthDispositivoTest {
    private fun clave(nombre: String): String =
        requireNotNull(InstrumentationRegistry.getArguments().getString(nombre)) { "Falta parámetro de prueba: $nombre" }
    @Test fun permisoDenegadoConservaSesionYTokenEstaCifrado(): Unit = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val repo = GlobalContext.get().get<AuthRepository>()
        repo.cerrarSesion()
        val sesion = repo.iniciarSesion(Credenciales("vendedor", clave("vendedorPassword")))
        val valores = contexto.getSharedPreferences("sesion_segura", Context.MODE_PRIVATE).all
        assertTrue("El archivo no debe contener el JWT en texto plano",
            valores.values.none { it.toString().contains(sesion.token) })
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            device.wait(Until.findObject(By.desc("Abrir menú")), 15_000).click()
            device.wait(Until.findObject(By.text("Productos")), 5_000).click()
            assertNotNull(device.wait(Until.findObject(By.text("Inventario conectado")), 10_000))
            // Provocación deliberada por instrumentación: la UI oculta esta acción al vendedor.
            escenario.onActivity { actividad ->
                val store = actividad.viewModelStore
                val vm = store.keys().mapNotNull {
                    store[it] as? ProductoViewModel
                }.first()
                vm.eliminar(1L)
            }
            assertNotNull(device.wait(Until.findObject(By.text("No tienes permisos para esta operación.")), 15_000))
            assertNotNull(repo.sesionGuardada())
            val directorio = contexto.getExternalFilesDir("s10")!!
            directorio.mkdirs()
            assertTrue(device.takeScreenshot(File(directorio, "403-mensaje-usuario.png")))
            val copia = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "cp " + directorio.absolutePath + "/403-mensaje-usuario.png /sdcard/Download/S10-403.png")
            ParcelFileDescriptor.AutoCloseInputStream(copia).use { it.readBytes() }
        }
    }
    @Test fun un401ProtegidoLimpiaSesionYLlevaAlLogin(): Unit = runBlocking {
        val repo = GlobalContext.get().get<AuthRepository>()
        repo.cerrarSesion()
        repo.iniciarSesion(Credenciales("admin", clave("adminPassword")))
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            device.wait(Until.findObject(By.desc("Abrir menú")), 15_000).click()
            device.wait(Until.findObject(By.text("Productos")), 5_000).click()
            assertNotNull(device.wait(Until.findObject(By.text("Inventario conectado")), 10_000))
            // JWT inválido deliberado: provoca un 401 real de una ruta protegida.
            GlobalContext.get().get<AlmacenSeguro>().guardarToken("jwt-invalido-del-laboratorio")
            GlobalContext.get().get<HttpClient>().authProvider<BearerAuthProvider>()?.clearToken()
            escenario.onActivity { actividad ->
                val store = actividad.viewModelStore
                store.keys().mapNotNull { store[it] as? ProductoViewModel }.first().cargarProductos()
            }
            assertNotNull(device.wait(Until.findObject(By.text("Tu sesión expiró. Inicia sesión nuevamente.")), 15_000))
            assertNull(GlobalContext.get().get<AlmacenSeguro>().leerToken())
            val contexto = InstrumentationRegistry.getInstrumentation().targetContext
            val archivo = File(contexto.getExternalFilesDir("s10"), "401-login.png")
            archivo.parentFile!!.mkdirs()
            assertTrue(device.takeScreenshot(archivo))
            val copia = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "cp " + archivo.absolutePath + " /sdcard/Download/S10-401.png")
            ParcelFileDescriptor.AutoCloseInputStream(copia).use { it.readBytes() }
        }
    }
}
