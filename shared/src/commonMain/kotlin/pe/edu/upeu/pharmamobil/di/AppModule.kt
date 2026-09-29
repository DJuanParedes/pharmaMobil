package pe.edu.upeu.pharmamobil.di

import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioRest
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel

private const val CATEGORIA_POR_DEFECTO = 1L

val dataModule = module {
    single { crearHttpClient() }
    single { ProductoApi(get()) }
    single<ProductoRepository> { ProductoRepositorioRest(get(), CATEGORIA_POR_DEFECTO) }
}

val domainModule = module {
    factory { RegistrarProductoUseCase(get()) }
    factory { ListarProductosUseCase(get()) }
    factory { ActualizarProductoUseCase(get()) }
    factory { EliminarProductoUseCase(get()) }
}

val presentationModule = module {
    viewModelOf(::ProductoViewModel)
}

expect val platformModule: Module

fun initKoin(
    appDeclaration: KoinAppDeclaration = {},
): KoinApplication = startKoin {
    appDeclaration()
    modules(dataModule, domainModule, presentationModule, platformModule)
}
