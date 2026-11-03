package pe.edu.upeu.pharmamobil.permiso

import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso

/**
 * La guía pide la implementación Android en esta sesión. En iOS la tarjeta se muestra
 * como "Denegado" hasta implementar UNUserNotificationCenter en la sesión 14; las claves
 * de uso ya están en Info.plist para que la app no se cierre al pedir cámara o ubicación.
 */
class GestorPermisosIos : GestorPermisos {
    override suspend fun estado(permiso: Permiso): EstadoPermiso = EstadoPermiso.Denegado
    override suspend fun solicitar(permiso: Permiso): EstadoPermiso = EstadoPermiso.Denegado
    override fun abrirAjustes() = Unit
}
