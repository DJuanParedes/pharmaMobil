# Producto 2 � Informe comparativo

## 1. Formato monetario y resultados observados

En mi proyecto, `Formato.android.kt` delega en `NumberFormat.getCurrencyInstance(Locale("es", "PE"))`, mientras `Formato.ios.kt` utiliza `NSNumberFormatterCurrencyStyle`, `NSLocale("es_PE")` y `NSNumber`. Ambos identificadores representan espa�ol del Per�, pero seleccionan bibliotecas diferentes: Java en Android y Foundation en iOS. El locale expresa una preferencia cultural, no obliga a que sus motores compartan la misma versi�n de datos regionales ni la misma representaci�n del espacio, s�mbolo y agrupaci�n. Por eso compar� el mismo producto, Paracetamol 500 mg, con precio num�rico 15.5 y stock 100, obtenido del backend oficial.

Android produjo literalmente �S/�15.50�. Su espacio entre s�mbolo y cifra es U+00A0, no separable. En iOS compilaron y pasaron las pruebas comunes, que normalizan ese espacio al verificar el texto. El resultado literal de su interfaz no est� confirmado: detuve la ejecuci�n visual cuando el estudiante indic� no abrir el simulador. Esta parte de la primera pregunta queda pendiente; no presentar� como observaci�n un formato calculado en otro sistema. Incluso si ambas salidas coincidieran visualmente, sus implementaciones seguir�an siendo diferentes. `Producto.toUi()` aplica el formato en presentaci�n y conserva el `Double` del dominio. El composable �nicamente muestra `ProductoUi.precio`.

## 2. Contexto Android y controlador iOS

`CompartidorAndroid` necesita un `Context` porque Android inicia una actividad mediante `Context.startActivity`. Koin entrega el contexto de aplicaci�n registrado por `MainApplication`, no una Activity concreta. Por ello agregu� `FLAG_ACTIVITY_NEW_TASK` al Intent creado por `Intent.createChooser`. El env�o utiliza `ACTION_SEND`, MIME `text/plain` y `EXTRA_TEXT`; el selector permiti� comprobar el nombre, precio y stock sin enviar el contenido a otra persona.

`CompartidorIos` no recibe ese objeto porque UIKit utiliza otra organizaci�n. Obtiene la escena activa de `UIApplication`, identifica la ventana principal y encuentra el controlador visible. Ese controlador presenta `UIActivityViewController`. Configur� adem�s el ancla del popover para iPad e import� expl�citamente la extensi�n correspondiente de UIKit. Koin construye aqu� `CompartidorIos()` sin par�metros Android. Ninguna de estas decisiones aparece en `DetalleProductoViewModel`: el ViewModel s�lo conoce la interfaz de dominio y el producto cargado.

## 3. Alternativa con interfaz e inyecci�n

Si hubiera definido una interfaz `FormateadorMoneda`, habr�a registrado una implementaci�n por plataforma en Koin e inyectado esa dependencia en los consumidores. Esto facilitar�a sustituir el formato por un doble en pruebas y permitir�a recibir configuraci�n mediante el constructor. Sin embargo, aumentar�a el n�mero de contratos, registros y argumentos para una funci�n sin estado. Adem�s, olvidar un registro provocar�a un fallo al resolver la dependencia, mientras una declaraci�n actual ausente se detecta al compilar el target correspondiente.

Eleg� expect/actual para `formatearSoles` y `InfoDispositivo`, porque sus implementaciones se seleccionan por source set y no requieren infraestructura adicional. Eleg� interfaz con inyecci�n para Compartidor porque Android necesita Context y las pruebas necesitan sustituir el selector. Las pruebas comunes usan un Compartidor que registra los textos, simula un fallo y verifica que el detalle conserve el producto al reintentar. El acoplamiento a UIKit y Android permanece aislado en sus source sets; la legibilidad del c�digo com�n mejora al expresar la capacidad, sin reproducir sus mecanismos internos.

## 4. Ausencia deliberada de un actual

Coment� temporalmente el contenido de `Formato.android.kt` y ejecut� `:shared:compileAndroidMain`. El compilador inform� exactamente: �Expected formatearSoles has no actual declaration in module <commonMain> for JVM�. El log registra `:shared:compileAndroidMain FAILED` y `BUILD FAILED in 11s`. La captura muestra ese diagn�stico real mediante un visor del registro, no una pantalla de un IDE ni un mensaje fabricado. Despu�s restaur� los bytes originales y repet� la compilaci�n, que finaliz� correctamente. Esto demuestra una garant�a por target: el actual iOS no puede satisfacer una compilaci�n JVM. Tambi�n deben coincidir el paquete, nombre, par�metros y retorno.

## 5. Capacidad que debe permanecer com�n

La validaci�n de registrar un producto no deber�a trasladarse a androidMain ni iosMain. En `RegistrarProductoUseCase.kt`, `validarProducto` normaliza el nombre, comprueba su longitud de 3 a 150 caracteres, exige precio m�nimo 0.01 y stock entero no negativo. Son reglas del inventario y del contrato REST, no funciones del sistema operativo. Duplicarlas permitir�a que una plataforma aceptara productos rechazados por la otra y obligar�a a corregir dos implementaciones.

Asimismo, `comoTextoParaCompartir()` conserva en commonMain la composici�n �nombre � precio � Stock: cantidad�. S�lo el formato local y la apertura del selector dependen de la plataforma. Mi criterio consiste en separar reglas que describen el negocio de mecanismos que requieren una API nativa. La implementaci�n de Acerca de confirma esa frontera: Compose muestra dos cadenas comunes y `InfoDispositivo` consulta `Build.VERSION.RELEASE` o `UIDevice`, seg�n el sistema donde se ejecuta.
