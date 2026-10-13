# Sesión 9 — expect/actual y capacidades nativas

**Integrante:** Juan Paredes Cárdenas · **Cuenta:** DJuanParedes  
**Fuente:** Guía Práctica N.º 09, 12 páginas, proporcionada por el estudiante.  
**Fecha de actividad autónoma indicada:** 12 de octubre de 2026, 23:59.

## Continuidad y commits propios

Repositorio: [DJuanParedes/pharmaMobil](https://github.com/DJuanParedes/pharmaMobil).
Rama: [feature/expect-actual-paredes](https://github.com/DJuanParedes/pharmaMobil/tree/feature/expect-actual-paredes).
Base: `actividad-autonoma-sesion-8`, commit `dd6a61ce62b9c0b6fc8401dce0ac34997841a870`.
Como no existía `develop`, se creó desde esa base; la funcionalidad previa se conserva.

| Commit publicado | Trabajo |
| --- | --- |
| [322f922](https://github.com/DJuanParedes/pharmaMobil/commit/322f9225a56d62685ff7ae65db0fd8107f2dfc3a) | Declaración expect y punto de control sin actual |
| [7563f2d](https://github.com/DJuanParedes/pharmaMobil/commit/7563f2de34ce5015c5a0a3533f2a2a37875c9fd8) | Actual Android/iOS y mapeo a ProductoUi |
| [113d424](https://github.com/DJuanParedes/pharmaMobil/commit/113d4242a745c69d718faf09fabc047bbb695b21) | Contrato, implementaciones, Koin, detalle y botón Compartir |
| [7505bde](https://github.com/DJuanParedes/pharmaMobil/commit/7505bde2f9bea95c08d33cb55fceb7b0c3cebc01) | Verificación macOS Kotlin/Swift y capturas XCTest |

Los commits publicados pertenecen a la cuenta autenticada DJuanParedes. La entrega es
individual, según la indicación explícita del estudiante: incluye su nombre, su rama y
sus commits. El proyecto local se sincroniza con la historia publicada. Para el aula
virtual se adjunta el PDF y el enlace de esta rama.

## Diferencias de plataforma

| Aspecto | Android | iOS | Decisión común |
| --- | --- | --- | --- |
| Moneda | Java NumberFormat y locale es-PE | Foundation NSNumberFormatter y locale es_PE | expect/actual con firma y paquete idénticos |
| Compartir | Intent y selector de actividades | UIActivityViewController | Compartidor.compartir(texto) |
| Contexto | Aplicación; NEW_TASK en el chooser | Escena activa y controlador visible; popover en iPad | ViewModel no conoce el sistema operativo |
| Dependencias | Koin obtiene androidContext | Koin construye CompartidorIos | Koin resuelve la misma interfaz |
| Acceso al backend | 10.0.2.2 desde emulador | localhost desde simulador macOS | Mismo repositorio Ktor y endpoints reales |
| Compilación | Android SDK, Gradle y JDK | macOS, Xcode, Kotlin/Native | commonMain contiene reglas y pantallas |

El formato se calcula al mapear a `ProductoUi`, antes de mostrarlo. El dominio mantiene
el precio numérico. El texto para compartir se construye una sola vez en código común
con el guion largo `—` y el separador `·`; cada implementación sólo abre el selector.
El error de compartir se muestra conservando el detalle y se limpia si un reintento funciona.

## Lista de cotejo y puntos de control

| N.º | Criterio de la guía | Implementación / evidencia |
| --- | --- | --- |
| 1 | expect y dos actual | Tres archivos Formato del mismo paquete |
| 2 | Paquete y firma idénticos | pe.edu.upeu.pharmamobil.platform; formatearSoles(Double): String |
| 3 | Precio en ambas plataformas | Captura Android; implementación iOS y pruebas Kotlin aprobadas; precio literal visual iOS pendiente |
| 4 | Formato en presentación | Producto.toUi(); ProductoUi.precio es String |
| 5 | Interfaz en domain sin plataformas | domain/platform/Compartidor.kt |
| 6 | Dos implementaciones nativas | CompartidorAndroid y CompartidorIos |
| 7 | Koin en ambos módulos | single<Compartidor> en ambos PlatformModule; resolución Android ejecutada; recorrido visual iOS pendiente |
| 8 | Botón y texto común | DetalleProductoScreen → ViewModel → comoTextoParaCompartir → Compartidor; selector Android observado, hoja iOS pendiente |
| 9 | Sin imports nativos en presentation | Búsqueda de imports android.* y platform.UIKit.* sin coincidencias |
| 10 | Capturas Android/iOS | Android capturado localmente; capturas iOS pendientes por indicación de no abrir el simulador |

El estudiante indicó no abrir el simulador de iOS. Se detuvo su ejecución visual y el
workflow ahora sólo permite una ejecución manual con opción explícita, desactivada
por defecto. Antes de esa indicación pasaron 59 pruebas Kotlin iOS en macOS; esto no
acredita la compilación Swift ni reemplaza las capturas de su interfaz.

**Punto de control 1.** Se compiló después del primer commit, antes de crear actual:
`Expected formatearSoles has no actual declaration in module <commonMain> for JVM`.
Se guardó el registro completo y una captura del mensaje real. La captura corresponde
a Chrome del emulador mostrando un visor del log Gradle; no es una captura de Android
Studio. El mensaje no fue recreado ni editado.

**Punto de control 2.** Tras los actual se compiló Android y se mostró el listado con
`S/ 15.50`. Ambos archivos actual conservan la firma y el paquete del expect.

**Punto de control 3.** `Compartidor` y `comoTextoParaCompartir()` se compilan como código
común, sin importar APIs Android ni iOS.

**Punto de control 4.** Android resolvió el Compartidor registrado en Koin al ejecutar el
botón del detalle. En iOS, XCTest está preparado para comprobar esa resolución, pero el recorrido visual queda pendiente.
`MainApplication` conserva el registro de `androidContext()`.

**Punto de control 5.** La búsqueda de imports nativos en todo presentation no produjo
coincidencias. Ambas pantallas comunes se conectan exclusivamente al ViewModel.

**Verificación del paso 6.** Se ejecutó el detalle y el selector Android. El árbol de interfaz
capturado contiene `Paracetamol 500 mg — S/ 15.50 · Stock: 100`, con espacio no separable
introducido por el formateador. La prueba iOS está preparada para ese recorrido y para copiar el texto
de la hoja; no se da por ejecutada ni se inventa su contenido.

## Evidencias Android

![Error esperado sin actual](../evidencias/sesion9/control1/error-sin-actual.png)
![Listado Android con soles](../evidencias/sesion9/android/listado-precio-soles.png)
![Detalle Android](../evidencias/sesion9/android/detalle-producto.png)
![Selector nativo Android](../evidencias/sesion9/android/selector-compartir.png)

Registros: [punto de control 1](../evidencias/sesion9/control1/compilacion-sin-actual.txt),
[compilación con formato](../evidencias/sesion9/android/compilacion-formato.txt),
[compilación y pruebas](../evidencias/sesion9/android/compilacion-y-pruebas.txt).
Las imágenes proceden de `adb exec-out screencap -p` en un emulador API 37 ejecutando
el APK compilado; no son maquetas. La jerarquía del selector está en `selector-compartir.xml`.

## Entorno reproducible y actividad autónoma

Se utilizó el backend oficial [dreyna/pharmaSoft](https://github.com/dreyna/pharmaSoft),
commit `6bc94e321f993c194789e9ce9d008a48813e0bf2`, con sus controladores y servicios
originales. Un POM temporal añade H2 en modo Oracle; se deshabilita Flyway y se usa
Hibernate create-drop exclusivamente para el laboratorio. No se cambió el código del
backend ni se simuló la respuesta HTTP. Se sembraron mediante POST reales una categoría
Medicamentos y los productos Paracetamol 500 mg (15.50, stock 100) y Vitamina C 1 g
(1234.56, stock 4). Son datos sintéticos, no un inventario de producción.

`ci/preparar_backend.py` y `ci/seed_backend.py` reproducen ese entorno. La verificación
macOS puede compilar el framework y la aplicación Swift, ejecutar XCTest y exportar
sus adjuntos cuando el estudiante solicite esa verificación. La comprobación visual
iOS se detuvo por su indicación; las pruebas Kotlin previas están conservadas en
`evidencias/sesion9/ios`, con un resumen que diferencia los resultados pendientes.

Las pruebas comunes del detalle cubren carga, mantenimiento del precio numérico,
texto con separadores exactos, ausencia de compartir durante carga, producto inexistente
y recuperación ante fallo nativo. Se ejecutan junto a las pruebas CRUD preexistentes.

Para inspeccionar interoperabilidad, revisar los archivos Swift de iosApp. La cabecera
`Shared.h` podrá comprobarse al construir la aplicación iOS; esa verificación está pendiente. Las decisiones de estado permanecen en Kotlin;
Swift se limita a inicializar Koin y alojar el controlador Compose.

Se localizó y revisó también la ficha `sesion09_actividad_autonoma_diferencias_qkoyb3gpig.pdf`
(8 páginas) en Descargas. Su alcance adicional incluye el inventario completo, un informe
de 600 a 900 palabras, una tercera capacidad conectada a la interfaz y un único PDF.
La tercera capacidad elegida es información del dispositivo en la pantalla Acerca de.

## Entrega individual y estado final

- [Inventario completo](sesion9-inventario.md).
- [Informe comparativo: 734 palabras](sesion9-informe-comparativo.md).
- [PDF de entrega](../output/pdf/S09_ActividadAutonoma_Paredes_Cardenas.pdf).
- [Acerca de en Android](../evidencias/sesion9/android/acerca-dispositivo.png).
- [Actual comentado: error real](../evidencias/sesion9/control-autonomo/error-actual-comentado.png).
- [59 pruebas Kotlin iOS: resumen y procedencia](../evidencias/sesion9/ios/resultado.json).

Android: las tres capacidades ejecutadas y capturadas; 59 pruebas, cero fallos y errores.
iOS: fuentes comunes y actual compilados y 59 pruebas aprobadas; compilación Swift,
hoja de compartir, Acerca de y capturas visuales pendientes por indicación del estudiante.
El resultado literal iOS de la primera pregunta tampoco se declara observado.
La entrega no se ha enviado al aula virtual y no se presenta como rúbrica completamente
satisfecha mientras falten esas evidencias.
