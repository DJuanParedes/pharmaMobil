package pe.edu.upeu.pharmamobil

import android.Manifest
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.koin.core.context.GlobalContext
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioOffline
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarUseCase.Companion.MAX_INTENTOS
import java.io.File
import java.util.regex.Pattern

/**
 * Evidencias de la sesión 13 en el emulador, contra PharmaSoft real (H2 y credenciales efímeras de CI).
 * `ci/sesion13/ejecutar_android_s13.sh` instala la app limpia y revoca POST_NOTIFICATIONS antes de esta clase.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class IntegradorSesion13Test {
    private val inst get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(inst)
    private val koin get() = GlobalContext.get()
    /** Si una prueba falla, guarda la pantalla y el árbol de vistas para diagnosticar. */
    @get:Rule val diagnostico = object : TestWatcher() {
        override fun failed(e: Throwable?, description: Description) {
            runCatching { captura("error-" + description.methodName) }
            runCatching { shell("uiautomator dump /sdcard/Download/S13-error-" + description.methodName + ".xml") }
        }
    }
    private fun argumento(nombre: String) = requireNotNull(InstrumentationRegistry.getArguments().getString(nombre))

    private fun shell(comando: String): String {
        val fd = inst.uiAutomation.executeShellCommand(comando)
        return ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes().decodeToString() }
    }

    private fun esperar(selector: BySelector, ms: Long = 30_000): UiObject2 =
        requireNotNull(device.wait(Until.findObject(selector), ms)) { "No apareció: $selector" }

    private fun pulsar(selector: BySelector) {
        repeat(4) { intento ->
            device.waitForIdle()
            try { esperar(selector).click(); return } catch (e: StaleObjectException) {
                if (intento == 3) throw e
            }
        }
    }

    private fun captura(nombre: String) {
        device.waitForIdle()
        Thread.sleep(800)
        val archivo = File(inst.targetContext.getExternalFilesDir("s13"), "$nombre.png")
        archivo.parentFile!!.mkdirs()
        assertTrue(device.takeScreenshot(archivo))
        shell("cp ${archivo.absolutePath} /sdcard/Download/S13-$nombre.png")
        println("CAPTURA: S13-$nombre.png")
    }

    private suspend fun iniciarSesion() {
        val auth = koin.get<AuthRepository>()
        if (auth.sesionGuardada() == null) auth.iniciarSesion(Credenciales("admin", argumento("adminPassword")))
    }

    private fun verTarjetaPermiso(texto: String) {
        val selector = By.textContains(texto)
        if (device.wait(Until.findObject(selector), 5_000) == null) {
            device.findObject(By.scrollable(true))?.scrollUntil(Direction.DOWN, Until.findObject(selector))
        }
        val tarjeta = esperar(selector)
        device.findObject(By.scrollable(true))?.let { lista ->
            if (tarjeta.visibleBounds.bottom > lista.visibleBounds.bottom - 300) lista.scroll(Direction.DOWN, 0.3f)
        }
    }

    private fun responderDialogo(permitir: Boolean) {
        val id = if (permitir) "permission_allow_button" else "permission_deny_button"
        val boton = device.wait(Until.findObject(By.res(Pattern.compile(".*:id/$id"))), 15_000)
            ?: esperar(By.text(Pattern.compile(if (permitir) "(?i)(allow|permitir)" else "(?i)(don.t allow|no permitir)")))
        boton.click()
        device.waitForIdle()
    }

    private fun modoAvion(activo: Boolean) {
        shell("cmd connectivity airplane-mode " + if (activo) "enable" else "disable")
        Thread.sleep(if (activo) 4_000 else 12_000)
        println("MODO AVION: " + shell("settings get global airplane_mode_on").trim())
    }

    private val gestor get() = koin.get<GestorPermisos>()

    @Test fun a_permisoEnSusTresEstados(): Unit = runBlocking {
        iniciarSesion()
        ActivityScenario.launch(MainActivity::class.java).use {
            esperar(By.desc("Abrir menú"), 60_000)

            // Estado 1: nunca pedido. La tarjeta explica para qué sirve antes de pedirlo.
            verTarjetaPermiso("puede avisarte")
            assertEquals(EstadoPermiso.Denegado, gestor.estado(Permiso.NOTIFICACIONES))
            captura("01-permiso-explicacion")

            pulsar(By.text("Permitir notificaciones"))
            esperar(By.res(Pattern.compile(".*:id/permission_deny_button")))
            captura("02-dialogo-del-sistema")
            responderDialogo(permitir = false)

            // Segunda negativa: Android deja de mostrar el diálogo y solo queda ir a Ajustes.
            verTarjetaPermiso("puede avisarte")
            pulsar(By.text("Permitir notificaciones"))
            responderDialogo(permitir = false)
            verTarjetaPermiso("bloqueadas")
            esperar(By.text("Abrir Ajustes"))
            assertEquals(EstadoPermiso.DenegadoParaSiempre, gestor.estado(Permiso.NOTIFICACIONES))
            captura("03-permiso-denegado-para-siempre")
        }
        // El usuario lo activa en Ajustes; al volver, ON_RESUME vuelve a leer el estado.
        inst.uiAutomation.grantRuntimePermission(inst.targetContext.packageName, Manifest.permission.POST_NOTIFICATIONS)
        ActivityScenario.launch(MainActivity::class.java).use {
            esperar(By.desc("Abrir menú"), 60_000)
            verTarjetaPermiso("Notificaciones activadas")
            assertEquals(EstadoPermiso.Concedido, gestor.estado(Permiso.NOTIFICACIONES))
            captura("04-permiso-concedido")
        }
        println("VERIFICADO: permiso Denegado -> DenegadoParaSiempre -> Concedido")
    }

    @Test fun b_estadoDeSincronizacion(): Unit = runBlocking {
        iniciarSesion()
        val local = koin.get<ProductoLocal>()
        val repo = koin.get<ProductoRepositorioOffline>()
        repo.sincronizar()
        ActivityScenario.launch(MainActivity::class.java).use {
            pulsar(By.desc("Abrir menú"))
            pulsar(By.text("Productos"))
            esperar(By.text("Inventario offline-first"))
            esperar(By.textContains("En línea · todo enviado"))
            captura("05-sync-en-linea")

            // Sin red: tres altas quedan en la cola. «S13 Amoxicilina 500» ya existe en PharmaSoft.
            modoAvion(true)
            val sufijo = InstrumentationRegistry.getArguments().getString("sufijo").orEmpty()
            listOf("S13 Ibuprofeno 400 $sufijo" to 20, "S13 Amoxicilina 500" to 12, "S13 Loratadina 10 $sufijo" to 30).forEach { (nombre, stock) ->
                repo.registrar(nombre, 4.5, stock)
            }
            pulsar(By.text("Sincronizar ahora"))
            esperar(By.textContains("Sin conexión · 3 cambios por enviar"), 60_000)
            esperar(By.text("Sincronizar ahora"), 60_000)
            captura("06-sync-sin-conexion-pendientes")

            // Con red: dos altas se confirman y el servidor rechaza el nombre duplicado con 409.
            modoAvion(false)
            var intentos = 0
            while (device.wait(Until.findObject(By.textContains("no se pudo enviar")), 3_000) == null && intentos++ < 6) {
                pulsar(By.text("Sincronizar ahora"))
                esperar(By.text("Sincronizar ahora"), 60_000)
            }
            esperar(By.textContains("no se pudo enviar"))
            // La barra muestra la fila fallida a mitad de la ronda: se espera a que termine de enviar las demás.
            val confirmadas = { local.listar().filter { it.nombre != "S13 Amoxicilina 500" && it.nombre.startsWith("S13") } }
            repeat(30) { if (local.contarPendientes(MAX_INTENTOS) > 0 || confirmadas().any { it.id < 0 }) Thread.sleep(1_000) }
            esperar(By.text("Sincronizar ahora"), 60_000)
            val fallida = local.fallidas(MAX_INTENTOS).single()
            assertEquals("S13 Amoxicilina 500", fallida.producto.nombre)
            assertTrue(confirmadas().all { it.id > 0 })
            pulsar(By.text("Revisar"))
            esperar(By.textContains(fallida.ultimoError!!.take(20)))
            captura("07-sync-rechazo-409-visible")

            device.openNotification()
            assertNotNull(device.wait(Until.findObject(By.textContains("no se pudo enviar al servidor")), 20_000))
            captura("08-notificacion-fila-fallida")
            device.pressBack()
            println("VERIFICADO: enviadas=2; fallida=${fallida.producto.nombre}; motivo=${fallida.ultimoError}; intentos=${fallida.intentos}")
        }
    }
}
