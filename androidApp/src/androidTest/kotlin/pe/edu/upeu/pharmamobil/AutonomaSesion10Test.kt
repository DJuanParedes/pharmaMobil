package pe.edu.upeu.pharmamobil

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.EstadoSesion
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.presentation.sesion.SesionViewModel
import java.io.File
import java.time.Instant

/** Evidencias reales: las alteraciones se realizan exclusivamente desde instrumentación. */
@RunWith(AndroidJUnit4::class)
class AutonomaSesion10Test {
    private val inst get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = inst.targetContext
    private val device get() = UiDevice.getInstance(inst)
    private val repo get() = GlobalContext.get().get<AuthRepository>()
    private val client get() = GlobalContext.get().get<HttpClient>()
    private val store get() = GlobalContext.get().get<AlmacenSeguro>()
    private val state get() = GlobalContext.get().get<EstadoSesion>()
    private val dir get() = File(context.getExternalFilesDir(null), "autonoma10").apply { mkdirs() }
    private fun clave(user: String) = requireNotNull(InstrumentationRegistry.getArguments().getString(user+"Password"))
    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        inst.uiAutomation.executeShellCommand(command)).use { it.readBytes().toString(Charsets.UTF_8) }
    private fun export(name: String, text: String) {
        File(dir,name).writeText(text)
        shell("mkdir -p /sdcard/Download/S10AA")
        shell("cp ${dir.absolutePath}/$name /sdcard/Download/S10AA/$name")
    }
    private fun shot(name: String) {
        device.waitForIdle()
        assertTrue(device.takeScreenshot(File(dir,"$name.png")))
        shell("mkdir -p /sdcard/Download/S10AA")
        shell("cp ${dir.absolutePath}/$name.png /sdcard/Download/S10AA/$name.png")
    }
    private fun click(selector: BySelector) {
        repeat(3) { i ->
            device.waitForIdle()
            val obj=requireNotNull(device.wait(Until.findObject(selector),15_000))
            try {obj.click();return} catch(e: StaleObjectException){if(i==2) throw e}
        }
    }
    private suspend fun login(user: String="admin") {
        repo.cerrarSesion()
        repo.iniciarSesion(Credenciales(user,clave(user)))
    }
    private suspend fun request(prefix: String?=null, token: String?=null): Pair<Int,String> = try {
        val r=client.get("productos") {
            if(prefix!=null) {
                header("Authorization","$prefix $token")
                // Conserva el encabezado deliberadamente incorrecto; evita el reintento automático de Auth.
                attributes.put(AuthCircuitBreaker,Unit)
            }
        }
        r.status.value to r.bodyAsText()
    } catch(e: ResponseException){e.response.status.value to e.response.bodyAsText()}
    private fun opened(): ActivityScenario<MainActivity> {
        val s=ActivityScenario.launch(MainActivity::class.java)
        assertNotNull(device.wait(Until.findObject(By.desc("Abrir menú")),15_000))
        return s
    }
    private fun productos() {click(By.desc("Abrir menú"));click(By.text("Productos"));assertNotNull(device.wait(Until.findObject(By.text("Inventario offline-first")),15_000))}
    private fun loginVisible() {assertNotNull(device.wait(Until.findObject(By.text("Inicia sesión para gestionar tu farmacia")),15_000))}
    private fun registro(id:Int,name:String,start:String,r:Pair<Int,String>,visible:String):JSONObject {
        val body=JSONObject(r.second)
        return JSONObject().put("id",id).put("escenario",name).put("inicio",start).put("fin",Instant.now().toString())
            .put("plataforma","Android").put("http",r.first).put("message",body.opt("message")?:JSONObject.NULL)
            .put("respuesta",body).put("interfaz",visible).put("captura","escenario-$id.png")
    }
    @Test fun seisEscenariosRealesYArchivoCifrado():Unit=runBlocking {
        val rows=JSONArray()
        login()
        opened().use {
            productos()
            val inicio=Instant.now().toString();val r=request();assertEquals(200,r.first);shot("escenario-1")
            rows.put(registro(1,"Token válido",inicio,r,"Productos: Inventario offline-first"))
            val token=requireNotNull(store.leerToken())
            val raw=File(context.applicationInfo.dataDir,"shared_prefs/sesion_segura.xml").readText()
            assertFalse(raw.contains(token));assertFalse(raw.contains("Bearer "))
            export("almacen-cifrado.xml",raw)
            export("almacen-comprobacion.json",JSONObject().put("token_legible",false).put("archivo","shared_prefs/sesion_segura.xml").put("fecha",Instant.now().toString()).toString(2))
        }
        login()
        opened().use {
            productos();val inicio=Instant.now().toString()
            // Espera real del vencimiento. El host arranca este laboratorio con TTL de un minuto.
            delay(65_000)
            val r=request();assertEquals(401,r.first);assertEquals("El token ha expirado. Inicie sesión nuevamente",JSONObject(r.second).getString("message"))
            loginVisible();assertNull(store.leerToken());shot("escenario-2")
            rows.put(registro(2,"Token expirado tras 65 segundos",inicio,r,"Tu sesión expiró. Inicia sesión nuevamente."))
        }
        login()
        opened().use {
            productos();val inicio=Instant.now().toString();val original=requireNotNull(store.leerToken())
            val i=original.lastIndexOf('.')+1
            val alterado=original.substring(0,i)+(if(original[i]=='A')'B' else 'A')+original.substring(i+1)
            store.guardarToken(alterado);client.authProvider<BearerAuthProvider>()?.clearToken()
            val r=request();assertEquals(401,r.first);assertEquals("El token no es válido",JSONObject(r.second).getString("message"))
            loginVisible();shot("escenario-3");rows.put(registro(3,"Un carácter de la firma alterado",inicio,r,"Tu sesión expiró. Inicia sesión nuevamente."))
        }
        login();repo.cerrarSesion()
        ActivityScenario.launch(MainActivity::class.java).use {
            loginVisible();val inicio=Instant.now().toString();val r=request();assertEquals(401,r.first)
            assertEquals("Debe iniciar sesión para acceder a este recurso",JSONObject(r.second).getString("message"))
            shot("escenario-4");rows.put(registro(4,"Sin cabecera después de logout",inicio,r,"Formulario de login sin sesión"))
        }
        login()
        opened().use {
            productos();val inicio=Instant.now().toString();val token=store.leerToken()
            val r=request("bearer",token);assertEquals(401,r.first)
            assertEquals("Debe iniciar sesión para acceder a este recurso",JSONObject(r.second).getString("message"))
            loginVisible();shot("escenario-5");rows.put(registro(5,"Prefijo bearer en minúscula",inicio,r,"Tu sesión expiró. Inicia sesión nuevamente."))
        }
        login()
        opened().use {
            productos();val inicio=Instant.now().toString()
            export("deshabilitar.flag","admin")
            repeat(40){if(!shell("ls /sdcard/Download/S10AA/deshabilitado.flag 2>/dev/null").contains("deshabilitado.flag")) delay(500)}
            assertTrue("El host debe deshabilitar al usuario en MySQL",shell("ls /sdcard/Download/S10AA/deshabilitado.flag 2>/dev/null").contains("deshabilitado.flag"))
            try {
                val r=request();assertEquals(401,r.first);assertEquals("El usuario está deshabilitado",JSONObject(r.second).getString("message"))
                loginVisible();assertNull(store.leerToken());shot("escenario-6")
                rows.put(registro(6,"Usuario deshabilitado con sesión abierta",inicio,r,"Tu sesión expiró. Inicia sesión nuevamente."))
            } finally {export("reactivar.flag","admin")}
        }
        export("bitacora.json",rows.toString(2))
        repeat(40){if(!shell("ls /sdcard/Download/S10AA/reactivado.flag 2>/dev/null").contains("reactivado.flag")) delay(500)}
        login()
        opened().use {shot("antes-cierre-proceso")}
        export("persistencia-lista.flag","sesion_guardada")
    }
    @Test fun cierreCompleto():Unit=runBlocking {
        val token=store.leerToken()
        assertNotNull("La sesión debe sobrevivir al force-stop del host",token)
        assertNotNull(repo.sesionGuardada())
        opened().use {s ->
            shot("sesion-restaurada-proceso")
            productos();assertEquals(200,request().first)
            s.onActivity {a -> a.viewModelStore.keys().mapNotNull{a.viewModelStore[it] as? SesionViewModel}.first().cerrar()}
            loginVisible();assertNull(store.leerToken());assertNull(state.sesion.value)
            val raw=File(context.applicationInfo.dataDir,"shared_prefs/sesion_segura.xml").readText()
            export("almacen-despues-logout.xml",raw)
            shot("logout-limpio")
            val r=request();assertEquals(401,r.first)
            device.pressBack();device.pressHome()
            shell("am start -n pe.edu.upeu.pharmamobil/.MainActivity")
            loginVisible();assertFalse(device.hasObject(By.text("Inventario offline-first")))
            shot("atras-sin-sesion")
            export("logout.json",JSONObject().put("token_ausente",store.leerToken()==null).put("http_siguiente",r.first)
                .put("message",JSONObject(r.second).getString("message")).put("estado_limpio",state.sesion.value==null)
                .put("atras_no_abre_productos",true).put("persistio_force_stop",true).put("fecha",Instant.now().toString()).toString(2))
        }
    }
    @Test fun matrizDeInterfaz():Unit=runBlocking {
        val matrix=JSONArray()
        for(user in listOf("admin","vendedor","almacen")) {
            login(user)
            opened().use {
                shot("inicio-$user")
                val registrar=device.hasObject(By.text("Registrar un producto"))
                click(By.desc("Abrir menú"))
                val clientes=device.hasObject(By.text("Clientes"));val pedidos=device.hasObject(By.text("Pedidos"))
                shot("menu-$user");click(By.text("Productos"))
                assertNotNull(device.wait(Until.findObject(By.text("Inventario offline-first")),15_000))
                shot("productos-$user")
                val editar=device.hasObject(By.text("Editar"))
                assertEquals(user!="vendedor",registrar);assertEquals(user!="vendedor",editar)
                assertEquals(user!="almacen",clientes);assertEquals(user!="almacen",pedidos)
                matrix.put(JSONObject().put("usuario",user).put("listar_productos",true).put("registrar_producto",registrar)
                    .put("listar_clientes",clientes).put("pedidos_demo",pedidos).put("reportes",false)
                    .put("auth_me","Automático al restaurar").put("editar_eliminar",editar))
            }
        }
        export("interfaz-roles.json",matrix.toString(2));repo.cerrarSesion()
    }
}
