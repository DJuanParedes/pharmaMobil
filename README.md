# PharmaMobil - Clean Architecture, MVVM y Koin

Proyecto Kotlin Multiplatform para Android e iOS, adaptado a la estructura desarrollada en clase.

## Capacidades nativas

La sesión 9 continúa la rama `actividad-autonoma-sesion-8`, conservando el CRUD REST y sus
errores. Se creó `develop` desde ese trabajo y después `feature/expect-actual-paredes`.
La función común declara qué necesita la aplicación; cada plataforma implementa cómo
formatea soles. Compartir utiliza una interfaz de dominio, inyectada con Koin.

| Responsabilidad | Código común | Android | iOS |
| --- | --- | --- | --- |
| Formato PEN | `platform/Formato.kt`: `expect fun formatearSoles(valor: Double): String` | `platform/Formato.android.kt`: `NumberFormat`, `Locale("es", "PE")` | `platform/Formato.ios.kt`: `NSNumberFormatter`, `NSLocale("es_PE")` |
| Contrato para compartir | `domain/platform/Compartidor.kt` | `platform/CompartidorAndroid.kt`: `ACTION_SEND`, MIME `text/plain`, `EXTRA_TEXT` | `platform/CompartidorIos.kt`: `UIActivityViewController` |
| Texto compartido | `domain/usecase/TextoParaCompartir.kt` | Consume el mismo texto común | Consume el mismo texto común |
| Inyección | `di/AppModule.kt` declara el módulo esperado | `di/PlatformModule.android.kt` registra `CompartidorAndroid(androidContext())` | `di/PlatformModule.ios.kt` registra `CompartidorIos()` |
| Presentación | `presentation/producto/ProductoUi.kt`, `presentation/detalle/DetalleProductoViewModel.kt` y `DetalleProductoScreen.kt` | Pantallas Compose comunes | Pantallas Compose comunes |

Las rutas de esta tabla son relativas a
`shared/src/{commonMain,androidMain,iosMain}/kotlin/pe/edu/upeu/pharmamobil/`.
`Producto.precio` permanece como `Double`. El mapeo `Producto.toUi()` convierte el precio
a `String` en presentación; los composables muestran el resultado sin formatearlo.
El detalle consulta `ProductoRepository.obtener(id)` y el botón **Compartir** invoca el
ViewModel. El contrato del texto es `nombre — precio en soles · Stock: cantidad`.
El icono utiliza `material-icons-core` y ninguna clase de presentación importa
`android.*` ni `platform.UIKit.*`.

Android recibe el contexto de aplicación registrado en `MainApplication`; el selector
incluye `FLAG_ACTIVITY_NEW_TASK`. iOS obtiene la ventana de una escena activa y el
controlador visible; también configura el ancla del popover para iPad. Esta adaptación
evita depender de `UIApplication.keyWindow`, obsoleto. Los espacios monetarios pueden
ser espacios no separables y la apariencia del selector depende del sistema operativo.

### Kotlin y Swift

`iosApp/iosApp/iOSApp.swift` importa `Shared` e inicializa Koin con
`KoinIosKt.doInitKoinIos()`. El nombre procede del archivo `di/KoinIos.kt`; el exportador
evita interpretar `initKoinIos` como un inicializador de Swift.
`ContentView.swift` llama `MainViewControllerKt.MainViewController()` para integrar
Compose en SwiftUI. La inicialización de Koin precede a la creación de la interfaz.

Las funciones suspend se exportan con un completion handler que Swift puede importar
como `async`. `@Throws` define los errores que pueden cruzar la frontera; no debe
asumirse que cualquier excepción Kotlin se convierte automáticamente en un error Swift.
Las jerarquías sealed no ofrecen la exhaustividad de un enum Swift; se requiere una
alternativa en el `switch`. Las data classes se usan como clases exportadas: no ofrecen
la sintaxis de desestructuración Kotlin ni parámetros predeterminados de `copy`; algunos
métodos generados pueden aparecer en la cabecera, según el exportador. Los tipos
anulables llegan como opcionales; los primitivos anulables pueden utilizar envoltorios
Kotlin. Por ello los estados y decisiones de la aplicación permanecen en Kotlin.
Referencia: [interoperabilidad oficial Kotlin/Objective-C y Swift](https://kotlinlang.org/docs/native-objc-interop.html).

### Código específico de plataforma

El [inventario completo](docs/sesion9-inventario.md) registra todos los expect del
proyecto y el contrato Compartidor, con firmas y rutas exactas. Además del formato y
compartir, la actividad autónoma incorpora `InfoDispositivo`: Android utiliza
`Build.VERSION.RELEASE` e iOS `UIDevice.currentDevice.systemVersion`. La pantalla
**Acerca de**, accesible desde el menú, muestra el sistema operativo y la versión real.
La tercera capacidad tiene un expect class y dos actual class con el mismo paquete.

### Verificación y entregables de la sesión 9

Consulta [la actividad autónoma y la trazabilidad](docs/sesion9-actividad-autonoma.md)
para los diez criterios, capturas, procedencia del backend y estado de entrega.
El [PDF de sesión 9](output/pdf/S09_ActividadAutonoma_Paredes_Cardenas.pdf) y el
[informe de 734 palabras](docs/sesion9-informe-comparativo.md) reúnen los cuatro productos.
La entrega es individual: Juan Paredes Cárdenas.
En Windows se verificaron `:shared:testAndroidHostTest` y `:androidApp:assembleDebug`.
La comprobación macOS se encuentra en
[GitHub Actions](https://github.com/DJuanParedes/pharmaMobil/actions/workflows/sesion09-ios.yml):
está preparada para compilar Kotlin/Native y Swift y exportar evidencias XCTest.
Se desactivaron sus arranques automáticos: sólo tiene `workflow_dispatch` con la opción
`ejecutar_simulador` desactivada por defecto. No abrir iOS Simulator hasta que el
estudiante lo solicite. Android pasó 59 pruebas y sus tres capacidades se observaron
en ejecución. Las 59 pruebas Kotlin iOS previas también pasaron, pero Swift y el flujo
visual iOS siguen pendientes; no hay capturas iOS ni resultado monetario literal inventado.
Consulta [el resultado iOS](evidencias/sesion9/ios/resultado.json).

En Android Studio abre la carpeta raíz del proyecto, espera la sincronización y ejecuta
`androidApp`. Inicia antes PharmaSoft en el puerto 8080. El emulador consume
`http://10.0.2.2:8080/api/v1/`; iOS Simulator utiliza `http://localhost:8080/api/v1/`.
En macOS abre `iosApp/iosApp.xcodeproj` y selecciona un simulador ARM64. Para usar un
iPhone físico configura tu equipo de firma y una URL del backend accesible desde él.

```powershell
.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug
```

```sh
./gradlew :shared:iosSimulatorArm64Test
```

El proyecto conserva la selección de JDK 21 Azul en `gradle/gradle-daemon-jvm.properties`.
La verificación final de Windows pasó con Zulu JDK 21.0.11, ya disponible en la caché
de Gradle, y el wrapper Gradle 9.1, sin cambiar los criterios del proyecto. En las
comprobaciones iniciales se utilizó temporalmente JBR 25; ese ajuste se restauró.

## Sesión 8 - CRUD REST de productos

La rama `feature/crud-productos-paredes` conecta el módulo de Productos con el backend
[PharmaSoft](https://github.com/dreyna/pharmaSoft) mediante Ktor Client:

- listado paginado con `GET /api/v1/productos`;
- consulta por identificador con `GET /api/v1/productos/{id}`;
- creación con `POST`, actualización con `PUT` y eliminación con `DELETE`;
- DTO genérico `PaginaResponseDto<T>` y mapeo entre datos y dominio;
- implementación REST de `ProductoRepository` registrada en Koin;
- traducción centralizada de errores HTTP, conexión, tiempo de espera y serialización;
- estados independientes para la fase de la pantalla y la operación en curso;
- validaciones por campo para nombre, precio y stock;
- recarga del listado después de crear, actualizar o eliminar;
- pruebas con `MockEngine` para los cinco endpoints y el error HTTP 400.

El emulador Android consume `http://10.0.2.2:8080/api/v1/`. La aplicación iOS usa
`http://localhost:8080/api/v1/` y debe ejecutarse en macOS con Xcode. Las capturas reales
de la verificación Android se encuentran en [`evidencias/sesion8/android`](./evidencias/sesion8/android).

Para verificar la entrega en Windows:

```powershell
.\gradlew.bat :shared:testAndroidHostTest
.\gradlew.bat :androidApp:assembleDebug
```

## Manejo de errores

La actividad autónoma de la sesión 8 comprueba que los fallos de red y del servidor se
traduzcan a tipos de dominio antes de llegar a la interfaz:

| Situación | Representación | Comportamiento visible |
| --- | --- | --- |
| HTTP 400 | `ErrorApi.Validacion` | El mensaje aparece debajo de `nombre`, `precio` o `stock`. |
| HTTP 404 | `ErrorApi.NoEncontrado` | Se informa que el producto ya no existe. |
| HTTP 409 | `ErrorApi.Conflicto` | Se conserva y muestra el mensaje enviado por PharmaSoft. |
| HTTP 5xx | `ErrorApi.Servidor` | Se permite reintentar sin cerrar la pantalla. |
| Backend detenido | `ErrorApi.SinConexion` | Se pide verificar la red y el servicio. |
| Tiempo de espera | `ErrorApi.TiempoAgotado` | Se informa que la operación tardó demasiado. |
| JSON no válido | `ErrorApi.RespuestaInvalida` | Se muestra una respuesta inesperada del servidor. |
| Cancelación de corrutina | No se convierte en `ErrorApi` | La cancelación se propaga y no genera un mensaje falso. |

Las pruebas de la actividad están en `commonTest`. Incluyen carga con datos, listado vacío,
validación por campo, eliminación con estado `EnCurso`, recarga del listado, códigos 404 y
409, desconexión, timeout y cancelación. En Windows se ejecutan con:

```powershell
.\gradlew.bat :shared:testAndroidHostTest
```

La entrega se desarrolla en la rama `actividad-autonoma-sesion-8`. Las capturas del
emulador, el resultado de `commonTest` y el extracto de Logcat se encuentran en
[`evidencias/sesion8-autonoma/android`](./evidencias/sesion8-autonoma/android).
El informe final con la matriz CRUD, la bitácora de ocho escenarios, las pruebas y las
conclusiones está disponible en
[`S08_ActividadAutonoma_Paredes_Cardenas.pdf`](./output/pdf/S08_ActividadAutonoma_Paredes_Cardenas.pdf).
La evidencia iOS queda identificada como pendiente porque requiere una ejecución real en
macOS con Xcode; no se incluyen capturas simuladas.

## Sesión 5 - Módulo de Productos por capas

La rama `feature/clean-mvvm` reorganiza el flujo completo de Productos con Clean Architecture y MVVM:

- `ProductoRepository` se declara como contrato de dominio;
- `ProductoRepositoryEnMemoria` implementa operaciones `suspend`, latencia visible y asignación interna de ID;
- `RegistrarProductoUseCase` concentra las reglas de nombre, precio y stock y devuelve `Result<Producto>`;
- `ProductoViewModel` expone un `StateFlow` de solo lectura y ejecuta operaciones con `viewModelScope`;
- `ProductoUiState` representa de forma exclusiva carga, lista vacía, productos o error;
- `ProductoScreen` es declarativa: recibe estado y eventos, sin validaciones ni datos de negocio con `remember`;
- Koin enlaza repositorio, caso de uso y ViewModel para Android e iOS;
- Android inicia Koin desde `MainApplication` e iOS desde `initKoinIos()`;
- la regla `Producto.requiereReposicion()` usa `STOCK_MINIMO = 5`;
- el formulario conserva mensajes por campo y confirma el producto con el ID asignado.

Las evidencias, el procedimiento y el resultado de las pruebas están en
[`output/pdf/evidencias-s5.pdf`](./output/pdf/evidencias-s5.pdf). Las capturas originales de Android se conservan en
[`evidencias/s5/android`](./evidencias/s5/android).

Estructura principal del flujo:

```text
presentation/producto/ProductoScreen
              │ eventos / UiState
              ▼
presentation/producto/ProductoViewModel
              │
              ▼
domain/usecase/RegistrarProductoUseCase
              │
              ▼
domain/repository/ProductoRepository
              ▲
              │ implementación
data/repository/ProductoRepositoryEnMemoria
```

## Sesión 4 - Navegación y estructura visual

La aplicación integra los requisitos del Reto 01 de la Sesión 4:

- cuatro destinos tipados: Inicio, Productos, Clientes y Pedidos;
- `Scaffold` y `TopAppBar` con título dinámico;
- `ModalNavigationDrawer` en teléfonos, `NavigationRail` en tablets y
  `PermanentNavigationDrawer` en pantallas amplias;
- reutilización directa de `ProductoScreen()` sin duplicar el formulario;
- conservación del estado escrito al cambiar temporalmente de destino;
- tema corporativo Material 3 con cambio dinámico entre modo claro y oscuro;
- mensajes de validación alineados con los ocho casos de la guía;
- pruebas de los destinos, identificadores y reglas del formulario.

La implementación de la Sesión 4 se encuentra en la rama `sesion-4`.

El procedimiento completo, las pruebas realizadas y las capturas de cada paso están en
[`documentacion/Documentacion_Sesion_4_PharmaMobil.docx`](./documentacion/Documentacion_Sesion_4_PharmaMobil.docx).
Las imágenes originales se conservan en
[`evidencias/sesion4`](./evidencias/sesion4).

## Sesión 3 - Registro de productos con Compose

La pantalla principal implementa la actividad práctica de la guía:

- `ProductoScreen()` en el paquete `presentation.producto`;
- campos Nombre, Precio y Stock mediante `OutlinedTextField`;
- estados locales con `remember` y `mutableStateOf`;
- botón Registrar y mensaje de resultado;
- validación secuencial de nombre, conversión y rango de precio, y conversión y rango de stock;
- creación de `Producto(id = 1L, nombre, precio, stock)` cuando los datos son válidos;
- retroalimentación visual con `isError` y mensajes junto a cada campo;
- limpieza automática del formulario después de un registro correcto;
- siete pruebas automatizadas que cubren exactamente los casos de la guía.

La entrega de la actividad autónoma está documentada en
[`ACTIVIDAD_AUTONOMA_SESION_3.md`](./ACTIVIDAD_AUTONOMA_SESION_3.md). Sus cinco capturas obligatorias se encuentran en
[`evidencias/actividad-autonoma-sesion3`](./evidencias/actividad-autonoma-sesion3). Las respuestas de reflexión están en
[`RESPUESTAS_REFLEXION_SESION_3.md`](./RESPUESTAS_REFLEXION_SESION_3.md).

## Base de dominio implementada

- `Cliente` con null-safety y `obtenerTelefono()` usando el operador Elvis.
- `Producto` inmutable y actualización de stock mediante `copy()`.
- Consultas de colecciones con `filter`, `map` y `find`.
- `Pedido`, `DetallePedido` y estados modelados con `sealed class`.
- Repositorio de Productos con funciones `suspend` y contrato ubicado en dominio.
- Caso de uso de registro sin acoplar la UI al repositorio concreto.
- Pruebas del dominio, repositorio, caso de uso, ViewModel, navegación, clientes y pedidos en `commonTest`.

## Actividad autónoma

Se añadió el procesamiento reactivo de pedidos e inventario como ampliación autónoma de la sesión:

- validación de existencia y disponibilidad de productos;
- suma de cantidades repetidas antes de descontar stock;
- resultados de negocio exhaustivos mediante `sealed class`;
- actualización inmutable y segura del inventario con `Mutex`;
- observación del inventario mediante `StateFlow`;
- seis pruebas adicionales de reglas de negocio y flujo reactivo.

La explicación, las reglas y los casos comprobados están en
[ACTIVIDAD_AUTONOMA.md](./ACTIVIDAD_AUTONOMA.md).

## Estructura original del proyecto

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

En Windows, valida toda la actividad compartida con:

```powershell
.\gradlew.bat :shared:testAndroidHostTest
.\gradlew.bat :androidApp:assembleDebug
```

El APK de depuración se genera en:

`androidApp/build/outputs/apk/debug/androidApp-debug.apk`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
