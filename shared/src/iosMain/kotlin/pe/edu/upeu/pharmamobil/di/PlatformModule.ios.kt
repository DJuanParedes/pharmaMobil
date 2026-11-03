package pe.edu.upeu.pharmamobil.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorIos

actual val platformModule: Module = module {
    single<pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver> { pe.edu.upeu.pharmamobil.data.local.CreadorDeDriverIos() }
    single<pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro> {
        pe.edu.upeu.pharmamobil.data.local.AlmacenSeguroIos()
    }
    single<Compartidor> { CompartidorIos() }
    // Sesión 13: en iOS la telemetría y las notificaciones quedan pendientes (sesión 14).
    single<pe.edu.upeu.pharmamobil.domain.port.Telemetria> { pe.edu.upeu.pharmamobil.domain.port.TelemetriaNula }
    single<pe.edu.upeu.pharmamobil.domain.port.Notificador> { pe.edu.upeu.pharmamobil.domain.port.NotificadorNulo }
    single<pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos> { pe.edu.upeu.pharmamobil.permiso.GestorPermisosIos() }
}
