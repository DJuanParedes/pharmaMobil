# Producto 2 — Informe comparativo

## 1. Formato monetario y resultados observados

En mi proyecto, `Formato.android.kt` delega en `NumberFormat.getCurrencyInstance(Locale("es", "PE"))`, mientras `Formato.ios.kt` utiliza `NSNumberFormatterCurrencyStyle`, `NSLocale("es_PE")` y `NSNumber`. Ambos identificadores representan español del Perú, pero seleccionan bibliotecas diferentes: Java en Android y Foundation en iOS. El locale expresa una preferencia cultural, no obliga a que sus motores compartan la misma versión de datos regionales ni la misma representación del espacio, símbolo y agrupación. Por eso elegí el mismo producto, Paracetamol 500 mg, con precio numérico 15.5 y stock 100, obtenido del backend oficial.

Android produjo literalmente «S/ 15.50». Su espacio entre símbolo y cifra es U+00A0, no separable. En iOS compilaron y pasaron las pruebas comunes, que normalizan ese espacio al verificar el texto. El resultado literal de su interfaz no está confirmado: detuve la ejecución visual cuando el estudiante indicó no abrir el simulador. Esta parte de la primera pregunta queda pendiente; no presentaré como observación un formato calculado en otro sistema. Incluso si ambas salidas coincidieran visualmente, sus implementaciones seguirían siendo diferentes. `Producto.toUi()` aplica el formato en presentación y conserva el `Double` del dominio. El composable únicamente muestra `ProductoUi.precio`.

## 2. Contexto Android y controlador iOS

`CompartidorAndroid` necesita un `Context` porque Android inicia una actividad mediante `Context.startActivity`. Koin entrega el contexto de aplicación registrado por `MainApplication`, no una Activity concreta. Por ello agregué `FLAG_ACTIVITY_NEW_TASK` al Intent creado por `Intent.createChooser`. El envío utiliza `ACTION_SEND`, MIME `text/plain` y `EXTRA_TEXT`; el selector permitió comprobar el nombre, precio y stock sin enviar el contenido a otra persona.

`CompartidorIos` no recibe ese objeto porque UIKit utiliza otra organización. Obtiene la escena activa de `UIApplication`, identifica la ventana principal y encuentra el controlador visible. Ese controlador presenta `UIActivityViewController`. Configuré además el ancla del popover para iPad e importé explícitamente la extensión correspondiente de UIKit. Koin construye aquí `CompartidorIos()` sin parámetros Android. Ninguna de estas decisiones aparece en `DetalleProductoViewModel`: el ViewModel sólo conoce la interfaz de dominio y el producto cargado.

## 3. Alternativa con interfaz e inyección

Si hubiera definido una interfaz `FormateadorMoneda`, habría registrado una implementación por plataforma en Koin e inyectado esa dependencia en los consumidores. Esto facilitaría sustituir el formato por un doble en pruebas y permitiría recibir configuración mediante el constructor. Sin embargo, aumentaría el número de contratos, registros y argumentos para una función sin estado. Además, olvidar un registro provocaría un fallo al resolver la dependencia, mientras una declaración actual ausente se detecta al compilar el target correspondiente.

Elegí expect/actual para `formatearSoles` y `InfoDispositivo`, porque sus implementaciones se seleccionan por source set y no requieren infraestructura adicional. Elegí interfaz con inyección para Compartidor porque Android necesita Context y las pruebas necesitan sustituir el selector. Las pruebas comunes usan un Compartidor que registra los textos, simula un fallo y verifica que el detalle conserve el producto al reintentar. El acoplamiento a UIKit y Android permanece aislado en sus source sets; la legibilidad del código común mejora al expresar la capacidad, sin reproducir sus mecanismos internos.

## 4. Ausencia deliberada de un actual

Comenté temporalmente el contenido de `Formato.android.kt` y ejecuté `:shared:compileAndroidMain`. El compilador informó exactamente: «Expected formatearSoles has no actual declaration in module <commonMain> for JVM». El log registra `:shared:compileAndroidMain FAILED` y `BUILD FAILED in 11s`. La captura muestra ese diagnóstico real mediante un visor del registro, no una pantalla de un IDE ni un mensaje fabricado. Después restauré los bytes originales y repetí la compilación, que finalizó correctamente. Esto demuestra una garantía por target: el actual iOS no puede satisfacer una compilación JVM. También deben coincidir el paquete, nombre, parámetros y retorno.

## 5. Capacidad que debe permanecer común

La validación de registrar un producto no debería trasladarse a androidMain ni iosMain. En `RegistrarProductoUseCase.kt`, `validarProducto` normaliza el nombre, comprueba su longitud de 3 a 150 caracteres, exige precio mínimo 0.01 y stock entero no negativo. Son reglas del inventario y del contrato REST, no funciones del sistema operativo. Duplicarlas permitiría que una plataforma aceptara productos rechazados por la otra y obligaría a corregir dos implementaciones.

Asimismo, `comoTextoParaCompartir()` conserva en commonMain la composición «nombre — precio · Stock: cantidad». Sólo el formato local y la apertura del selector dependen de la plataforma. Mi criterio consiste en separar reglas que describen el negocio de mecanismos que requieren una API nativa. La implementación de Acerca de confirma esa frontera: Compose muestra dos cadenas comunes y `InfoDispositivo` consulta `Build.VERSION.RELEASE` o `UIDevice`, según el sistema donde se ejecuta.
