# Producto 1 — Inventario de capacidades nativas

Rutas relativas a la raíz del repositorio. Prefijo de paquete:
`pe.edu.upeu.pharmamobil`. La búsqueda `rg '\bexpect\b' shared/src/commonMain`
identifica cinco declaraciones expect propias. Compartidor se incluye además como
contrato de dominio, tal como pide la ficha; no es un expect.

## 1. Plataforma original

- Firma: `expect fun getPlatform(): Platform`.
- commonMain: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/Platform.kt`.
- Android actual: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/Platform.android.kt`.
  API: `android.os.Build.VERSION.SDK_INT`; construye `AndroidPlatform`.
- iOS actual: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/Platform.ios.kt`.
  API: `platform.UIKit.UIDevice.currentDevice.systemName()` y `systemVersion`;
  construye `IOSPlatform`.

## 2. Cliente HTTP

- Firma: `expect fun crearHttpClient(): HttpClient`.
- commonMain: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/data/remote/HttpClientFactory.kt`.
- Android actual: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/data/remote/HttpClientFactory.android.kt`.
  API/biblioteca: motor Ktor CIO sobre JVM; base `http://10.0.2.2:8080/api/v1/`.
- iOS actual: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/data/remote/HttpClientFactory.ios.kt`.
  API/biblioteca: motor Ktor Darwin sobre NSURLSession de Foundation;
  base `http://localhost:8080/api/v1/`.
- El pipeline, serialización, traducción de errores y repositorio REST son comunes.

## 3. Módulo de inyección

- Firma: `expect val platformModule: Module`.
- commonMain: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/di/AppModule.kt`.
- Android actual: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/di/PlatformModule.android.kt`.
  Koin `module`, `single<Compartidor>`, `androidContext()` y `CompartidorAndroid`.
- iOS actual: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/di/PlatformModule.ios.kt`.
  Koin `module`, `single<Compartidor>` y `CompartidorIos`, sin Context Android.
- Es infraestructura de inyección; Koin es una biblioteca, no una API del sistema.

## 4. Formato de moneda peruana

- Firma: `expect fun formatearSoles(valor: Double): String`.
- commonMain: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/platform/Formato.kt`.
- Android actual: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/platform/Formato.android.kt`.
  API JVM: `java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "PE"))`.
- iOS actual: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/platform/Formato.ios.kt`.
  API Foundation: `NSNumberFormatterCurrencyStyle`, `NSNumberFormatter`, `NSLocale("es_PE")`,
  `NSNumber(double = valor)` y `stringFromNumber`.
- Consumidores: mapeo `Producto.toUi()` y texto común para compartir.

## 5. Información del dispositivo — tercera capacidad

- Firma: `expect class InfoDispositivo() { val sistema: String; val version: String }`.
- commonMain: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/platform/InfoDispositivo.kt`.
- Android actual: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/platform/InfoDispositivo.android.kt`.
  API: `android.os.Build.VERSION.RELEASE`; sistema `Android`.
- iOS actual: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/platform/InfoDispositivo.ios.kt`.
  API: `platform.UIKit.UIDevice.currentDevice.systemName()` y `systemVersion`.
- Interfaz conectada: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/presentation/acerca/AcercaDeScreen.kt`.
  Destino Acerca de en `navigation/Screen.kt`; integrado por `App.kt`.
- La capacidad original getPlatform utiliza SDK_INT en Android; la nueva utiliza la
  versión comercial RELEASE y campos separados, como pide la opción de la ficha.

## 6. Compartir producto — interfaz con inyección

- Contrato: `interface Compartidor { fun compartir(texto: String) }`.
- commonMain: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/domain/platform/Compartidor.kt`.
- Android implementación: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/platform/CompartidorAndroid.kt`.
  API: `Intent.ACTION_SEND`, `Intent.EXTRA_TEXT`, MIME text/plain,
  `Intent.createChooser`, `FLAG_ACTIVITY_NEW_TASK`, `Context.startActivity`.
- iOS implementación: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/platform/CompartidorIos.kt`.
  API: `UIActivityViewController`, escenas activas de UIApplication, UIWindowScene,
  UIViewController y extensión UIKit popoverPresentationController.
- Texto común: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/domain/usecase/TextoParaCompartir.kt`.
- No existen actual para Compartidor: sus implementaciones cumplen la interfaz y se
  seleccionan mediante los actual de platformModule. Esta distinción evita atribuir
  al compilador la resolución de una dependencia que realiza Koin.
