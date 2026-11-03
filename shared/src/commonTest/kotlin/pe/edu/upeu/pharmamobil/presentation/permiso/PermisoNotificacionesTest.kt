package pe.edu.upeu.pharmamobil.presentation.permiso

import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Al declararlo como interfaz, el gestor se reemplaza por uno falso en commonTest. */
class PermisoNotificacionesTest {
    private class GestorFalso(var actual: EstadoPermiso, val respuesta: EstadoPermiso) : GestorPermisos {
        val pedidos = mutableListOf<Permiso>()
        var ajustesAbiertos = 0
        override suspend fun estado(permiso: Permiso) = actual
        override suspend fun solicitar(permiso: Permiso): EstadoPermiso {
            pedidos += permiso; actual = respuesta; return respuesta
        }
        override fun abrirAjustes() { ajustesAbiertos++ }
    }

    @Test
    fun antesDePedirSeExplicaYSeOfrecePedir() = runTest {
        val modelo = PermisoNotificacionesModelo(GestorFalso(EstadoPermiso.Denegado, EstadoPermiso.Concedido))
        val estado = modelo.cargar()
        assertEquals(AccionPermiso.EXPLICAR_Y_PEDIR, accionPara(estado))
        assertTrue(textoPermiso(estado).contains("avisarte"))
    }

    @Test
    fun alConcederNoQuedaNingunaAccion() = runTest {
        val gestor = GestorFalso(EstadoPermiso.Denegado, EstadoPermiso.Concedido)
        val modelo = PermisoNotificacionesModelo(gestor)
        modelo.cargar()
        assertEquals(EstadoPermiso.Concedido, modelo.pedir())
        assertEquals(listOf(Permiso.NOTIFICACIONES), gestor.pedidos)
        assertEquals(AccionPermiso.NINGUNA, accionPara(modelo.estado))
    }

    @Test
    fun denegadoParaSiempreSoloOfreceAjustes() = runTest {
        val gestor = GestorFalso(EstadoPermiso.Denegado, EstadoPermiso.DenegadoParaSiempre)
        val modelo = PermisoNotificacionesModelo(gestor)
        modelo.pedir()
        assertEquals(AccionPermiso.ABRIR_AJUSTES, accionPara(modelo.estado))
        modelo.abrirAjustes()
        assertEquals(1, gestor.ajustesAbiertos)
    }
}
