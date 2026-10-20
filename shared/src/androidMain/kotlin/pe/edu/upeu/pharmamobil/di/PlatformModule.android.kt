package pe.edu.upeu.pharmamobil.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid

actual val platformModule: Module = module {
    single<pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver> { pe.edu.upeu.pharmamobil.data.local.CreadorDeDriverAndroid(androidContext()) }
    single<pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro> {
        pe.edu.upeu.pharmamobil.data.local.AlmacenSeguroAndroid(androidContext())
    }
    single<Compartidor> { CompartidorAndroid(androidContext()) }
}
