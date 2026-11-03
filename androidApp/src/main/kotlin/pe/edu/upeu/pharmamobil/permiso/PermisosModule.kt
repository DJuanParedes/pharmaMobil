package pe.edu.upeu.pharmamobil.permiso

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.port.Notificador

/** Lo que solo la aplicación Android puede aportar: necesita la Activity o el sistema de avisos. */
val permisosModule = module {
    single { GestorPermisosAndroid(androidContext(), androidContext().getSharedPreferences("permisos", Context.MODE_PRIVATE)) }
    single<GestorPermisos> { get<GestorPermisosAndroid>() }
    single<Notificador> { NotificadorAndroid(androidContext()) }
}
