package pe.edu.upeu.pharmamobil

import android.content.Context
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
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.koin.core.context.GlobalContext
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarUseCase.Companion.MAX_INTENTOS
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

/**
 * Actividad autónoma 13: mapa del flujo completo en diez pasos contra PharmaSoft real.
 * Cada paso deja una captura y una línea con el estado del sistema en evidencias/estado.txt.
 * La clave efímera del backend de CI y el token vencido llegan como argumentos de instrumentación.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AutonomaSesion13Test {
    private val inst get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(inst)
    private val koin get() = GlobalContext.get()
    private val local get() = koin.get<ProductoLocal>()
    private val auth get() = koin.get<AuthRepository>()
    private val almacen get() = koin.get<AlmacenSeguro>()
    private fun argumento(nombre: String) = requireNotNull(InstrumentationRegistry.getArguments().getString(nombre))
    private val clave get() = argumento("adminPassword")

    @get:Rule val diagnostico = object : TestWatcher() {
        override fun failed(e: Throwable?, description: Description) {
            runCatching { captura("error-" + description.methodName) }
            runCatching { estado("ERROR " + description.methodName, e?.message ?: "") }
        }
    }

    private fun shell(comando: String): String {
        val fd = inst.uiAutomation.executeShellCommand(comando)
        return ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes().decodeToString() }
    }

    private fun cerrarDialogoDelSistema() {
        if (device.hasObject(By.textContains("isn't responding")))
            device.findObject(By.text(Pattern.compile("(?i)^wait$")))?.click()
    }

    private fun esperar(selector: BySelector, ms: Long = 30_000): UiObject2 {
        val limite = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < limite) {
            device.wait(Until.findObject(selector), 3_000)?.let { return it }
            cerrarDialogoDelSistema()
        }
        error("No apareció: $selector")
    }

    private fun pulsar(selector: BySelector) {
        repeat(4) { intento ->
            device.waitForIdle()
            try { esperar(selector).click(); return } catch (e: StaleObjectException) { if (intento == 3) throw e }
        }
    }

    private fun desplazarHasta(selector: BySelector) {
        if (device.wait(Until.findObject(selector), 3_000) == null)
            device.findObject(By.scrollable(true))?.scrollUntil(Direction.DOWN, Until.findObject(selector))
    }

    private fun carpeta() = inst.targetContext.getExternalFilesDir("s13a")!!.also { it.mkdirs() }

    private fun captura(nombre: String) {
        device.waitForIdle(); Thread.sleep(800)
        val archivo = File(carpeta(), "$nombre.png")
        assertTrue(device.takeScreenshot(archivo))
        shell("cp ${archivo.absolutePath} /sdcard/Download/S13A-$nombre.png")
    }

    /** Una línea por paso con el estado del sistema; el script la copia a evidencias. */
    private fun estado(paso: String, texto: String) {
        val archivo = File(carpeta(), "estado.txt")
        archivo.appendText("$paso | $texto\n")
        shell("cp ${archivo.absolutePath} /sdcard/Download/S13A-estado.txt")
        println("ESTADO $paso | $texto")
    }

    private fun hora(epoch: Long?) = epoch?.let {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("America/Lima") }.format(Date(it)) + " (Lima)"
    } ?: "sin registro"

    private fun modoAvion(activo: Boolean) {
        shell("cmd connectivity airplane-mode " + if (activo) "enable" else "disable")
        Thread.sleep(if (activo) 4_000 else 12_000)
    }

    private fun tokenEnTextoPlano(token: String): Boolean =
        inst.targetContext.getSharedPreferences("sesion_segura", Context.MODE_PRIVATE).all.values.any { it.toString().contains(token) }

    private fun abrirProductos() {
        pulsar(By.desc("Abrir menú"))
        pulsar(By.text("Productos"))
        esperar(By.text("Inventario offline-first"))
    }

    private fun conViewModel(escenario: ActivityScenario<MainActivity>, accion: ProductoViewModel.() -> Unit) {
        escenario.onActivity { actividad ->
            val store = actividad.viewModelStore
            store.keys().mapNotNull { store[it] as? ProductoViewModel }.first().accion()
        }
    }

    private fun registrarEnFormulario(escenario: ActivityScenario<MainActivity>, nombre: String, precio: String, stock: String) =
        conViewModel(escenario) { cambiarNombre(nombre); cambiarPrecio(precio); cambiarStock(stock); guardar() }

    private suspend fun entrarComo(usuario: String) {
        auth.cerrarSesion()
        auth.iniciarSesion(Credenciales(usuario, clave))
    }

    @Test fun p01_primeraAperturaSinSesion(): Unit = runBlocking {
        assertNull(almacen.leerToken())
        assertEquals(0, local.listar().size)
        ActivityScenario.launch(MainActivity::class.java).use {
            esperar(By.text("Inicia sesión para gestionar tu farmacia"), 60_000)
            captura("01-primera-apertura")
        }
        estado("1", "Pantalla de inicio de sesión. Base local: 0 filas. Token guardado: no.")
    }

    @Test fun p02_inicioDeSesionCorrecto(): Unit = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            esperar(By.clazz("android.widget.EditText"))
            val campos = device.findObjects(By.clazz("android.widget.EditText"))
            campos[0].text = "admin"
            campos[1].text = clave
            captura("02a-credenciales")
            pulsar(By.text("Iniciar sesión"))
            esperar(By.desc("Abrir menú"), 60_000)
            captura("02-sesion-iniciada")
        }
        val token = requireNotNull(almacen.leerToken())
        val sesion = requireNotNull(auth.sesionGuardada())
        assertTrue("El JWT no debe estar en texto plano", !tokenEnTextoPlano(token))
        estado("2", "Token JWT (${token.length} caracteres) en EncryptedSharedPreferences «sesion_segura» con AES-256-GCM y llave del Android Keystore; no aparece en texto plano. Usuario ${sesion.usuario} (${sesion.nombreCompleto}), rol ADMIN, privilegios: ${sesion.permisos.sorted().joinToString(", ")}.")
    }

    @Test fun p03_listadoTrasPrimeraSincronizacion(): Unit = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            abrirProductos()
            esperar(By.textContains("En línea · todo enviado"), 60_000)
            esperar(By.textContains("Actualizado hace"))
            captura("03-listado-sincronizado")
        }
        val ultima = local.observarUltimaSincronizacion().first()
        estado("3", "Filas descargadas: ${local.listar().size} (${local.listar().joinToString { it.nombre }}). Última sincronización: ${hora(ultima)}.")
    }

    @Test fun p04_altaConConexion(): Unit = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            abrirProductos()
            registrarEnFormulario(escenario, "AA S13 Clotrimazol 1%", "7.80", "25")
            esperar(By.text("AA S13 Clotrimazol 1%"), 60_000)
            repeat(20) { if (local.listar().any { it.nombre == "AA S13 Clotrimazol 1%" && it.pendiente }) Thread.sleep(1_000) }
            val producto = local.listar().single { it.nombre == "AA S13 Clotrimazol 1%" }
            assertTrue(producto.id > 0 && !producto.pendiente)
            device.findObject(By.scrollable(true))?.scroll(Direction.UP, 1f)
            captura("04-alta-con-conexion")
            estado("4", "«${producto.nombre}» creado con red. ID asignado por PharmaSoft: ${producto.id}. Pendiente: no.")
        }
    }

    @Test fun p05_altaEnModoAvion(): Unit = runBlocking {
        modoAvion(true)
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            abrirProductos()
            registrarEnFormulario(escenario, "AA S13 Fluconazol 150", "12.50", "8")
            esperar(By.textContains("AA S13 Fluconazol 150: pendiente"), 60_000)
            pulsar(By.text("Sincronizar ahora"))
            esperar(By.textContains("Sin conexión · 1 cambio por enviar"), 60_000)
            esperar(By.text("Sincronizar ahora"), 60_000)
            val producto = local.listar().single { it.nombre == "AA S13 Fluconazol 150" }
            assertTrue(producto.id < 0 && producto.pendiente)
            captura("05-alta-sin-red")
            estado("5", "Modo avión: airplane_mode_on=${shell("settings get global airplane_mode_on").trim()}. «${producto.nombre}» con ID local provisional ${producto.id} y marca pendiente=1. Cambios por enviar: ${local.contarPendientes(MAX_INTENTOS)}.")
        }
    }

    @Test fun p06_sincronizacionAlRecuperarSenal(): Unit = runBlocking {
        val antes = local.contarPendientes(MAX_INTENTOS)
        val idLocal = local.listar().single { it.nombre == "AA S13 Fluconazol 150" }.id
        modoAvion(false)
        ActivityScenario.launch(MainActivity::class.java).use {
            abrirProductos()
            repeat(6) {
                if (local.contarPendientes(MAX_INTENTOS) > 0) {
                    pulsar(By.text("Sincronizar ahora")); esperar(By.text("Sincronizar ahora"), 60_000)
                }
            }
            esperar(By.textContains("En línea · todo enviado"), 60_000)
            val producto = local.listar().single { it.nombre == "AA S13 Fluconazol 150" }
            assertTrue(producto.id > 0 && !producto.pendiente)
            captura("06-sincronizado-al-volver")
            estado("6", "Pendientes antes: $antes; después: ${local.contarPendientes(MAX_INTENTOS)}. ID provisional $idLocal reemplazado por el definitivo ${producto.id}. Última sincronización: ${hora(local.observarUltimaSincronizacion().first())}.")
        }
    }

    @Test fun p07_errorDeValidacion(): Unit = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            abrirProductos()
            registrarEnFormulario(escenario, "Ab", "4.50", "10")
            val mensaje = "El nombre debe tener entre 3 y 150 caracteres"
            desplazarHasta(By.textContains(mensaje))
            esperar(By.textContains(mensaje))
            captura("07-error-validacion")
        }
        // La app valida antes de enviar con la misma regla que PharmaSoft; se comprueba la respuesta del servicio.
        val respuesta = try {
            koin.get<ProductoApi>().crear(ProductoRequestDto("Ab", 4.5, 10, true, 1)); "sin error"
        } catch (e: ClientRequestException) { "HTTP ${e.response.status.value}: ${e.response.bodyAsText().replace(Regex("\"timestamp\":\"[^\"]*\","), "")}" }
        assertTrue(respuesta.startsWith("HTTP 400"))
        estado("7", "Nombre «Ab». El mensaje aparece bajo el campo Nombre del formulario. Respuesta de PharmaSoft al mismo dato: $respuesta")
    }

    @Test fun p08_operacionSinPrivilegio(): Unit = runBlocking {
        entrarComo("vendedor")
        val sesion = requireNotNull(auth.sesionGuardada())
        ActivityScenario.launch(MainActivity::class.java).use {
            esperar(By.desc("Abrir menú"), 60_000)
            assertNull(device.wait(Until.findObject(By.text("Registrar un producto")), 3_000))
            abrirProductos()
            assertNull(device.wait(Until.findObject(By.text("Eliminar")), 3_000))
            captura("08-vendedor-sin-privilegio")
        }
        val respuesta = try {
            koin.get<ProductoApi>().crear(ProductoRequestDto("S13 Prueba vendedor", 3.0, 5, true, 1)); "sin error"
        } catch (e: ClientRequestException) { "HTTP ${e.response.status.value}" }
        assertEquals("HTTP 403", respuesta)
        estado("8", "Usuario vendedor, privilegios: ${sesion.permisos.sorted().joinToString(", ")}. La interfaz oculta «Registrar un producto», Editar y Eliminar. POST /productos con su token: $respuesta (Forbidden). Base local: ${local.listar().size} filas visibles, descargadas por la sesión anterior.")
    }

    @Test fun p09_tokenVencido(): Unit = runBlocking {
        entrarComo("admin")
        val vencido = argumento("tokenVencido")
        val perfil = """{"token":"$vencido","usuario":"admin","nombreCompleto":"Carlos Ramirez Torres","permisos":["CATALOGO_LEER"]}"""
        almacen.guardarToken(vencido); almacen.guardarPerfil(perfil)
        koin.get<HttpClient>().authProvider<BearerAuthProvider>()?.clearToken()
        ActivityScenario.launch(MainActivity::class.java).use {
            esperar(By.text("Inicia sesión para gestionar tu farmacia"), 60_000)
            val aviso = device.wait(Until.findObject(By.textContains("Tu sesión expiró")), 10_000)
            captura("09-token-vencido")
            estado("9", "JWT firmado por PharmaSoft con vencimiento una hora en el pasado. Al abrir, GET /auth/me responde 401 y la app borra el token (guardado: ${if (almacen.leerToken() == null) "no" else "sí"}). El usuario queda en Iniciar sesión ${if (aviso != null) "con el aviso «Tu sesión expiró. Inicia sesión nuevamente.»" else "sin aviso visible"}.")
        }
    }

    @Test fun p10_cierreDeSesion(): Unit = runBlocking {
        auth.iniciarSesion(Credenciales("admin", clave))
        ActivityScenario.launch(MainActivity::class.java).use {
            pulsar(By.text("Cerrar sesión"))
            esperar(By.text("Inicia sesión para gestionar tu farmacia"), 30_000)
            captura("10-sesion-cerrada")
        }
        assertNull(almacen.leerToken())
        val filas = local.listar()
        estado("10", "Token y perfil borrados del almacenamiento seguro. En la base local quedan ${filas.size} filas (${filas.joinToString { it.nombre }}), visibles para el siguiente usuario: hueco 7, cierre en la sesión 14.")
    }

    /** Se ejecuta aparte, después de desinstalar y reinstalar la app. */
    @Test fun p11_reinstalacionSinToken(): Unit = runBlocking {
        assertNull(almacen.leerToken())
        ActivityScenario.launch(MainActivity::class.java).use {
            esperar(By.text("Inicia sesión para gestionar tu farmacia"), 60_000)
            captura("11-reinstalacion")
        }
        estado("11", "Tras desinstalar y reinstalar: token guardado: ${almacen.leerToken() ?: "no"}; base local: ${local.listar().size} filas. android:allowBackup=false, por eso no se restaura.")
    }
}
