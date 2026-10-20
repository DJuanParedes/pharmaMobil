# Práctica 11: persistencia local y sincronización básica

**Estudiante:** Juan Paredes. **Fecha:** 20/10/2026. **Proyecto:** PharmaMobil / PharmaSoft.

## Correspondencia con los seis pasos de la guía

1. SQLDelight 2.1.0 está declarado en el catálogo Gradle. `Producto.sq` contiene la tabla,
   sus consultas y `pendiente`. Se conserva `activo` para no perder los filtros del CRUD previo.
2. `CreadorDeDriver` es una interfaz común; los drivers Android e iOS se registran en
   `platformModule`. La base se crea al iniciar Koin mediante `createdAtStart = true`.
3. `ProductoLocal` traduce filas a dominio, observa la tabla y escribe dentro de transacciones.
   No importa Ktor ni DTO. Los IDs negativos se asignan en una transacción.
4. `ProductoRepositorioOffline` expone `observar()` y `sincronizar()`. Mantiene el contrato
   CRUD para los casos de uso anteriores, pero el listado de presentación observa la base.
5. `ProductoViewModel` recoge el Flow una vez; guardar, editar y eliminar no vuelven a pedir
   el listado. La sincronización actualiza la base y su error se muestra como aviso independiente.
6. La prueba instrumentada cubre sincronización inicial, cierre del proceso, modo avión,
   reapertura, alta pendiente y reconexión. La comprobación final consulta el producto en la API.

## Ajustes necesarios respecto a los fragmentos de clase

- Las diapositivas exigen confirmar cada alta con su ID real. Se agregó `confirmar()` para
  eliminar la fila negativa e insertar la respuesta del POST en una misma transacción.
  Sin ese paso, el fragmento de la guía vuelve a enviar el mismo pendiente en cada sincronización.
- `JdbcSqliteDriver` se implementa en `androidHostTest`; `commonTest` usa una fábrica expect/actual.
  iOS usa su driver nativo. Cada prueba crea y cierra su propia base.
- La sesión 10 consultaba `/auth/me` antes de permitir abrir la app. Se conserva un perfil
  validado en EncryptedSharedPreferences/Keychain para restaurarlo cuando no hay conexión.
- `encodeDefaults = true` envía `estado=true`, obligatorio para PharmaSoft. La prueba de Ktor
  comprueba expresamente el cuerpo de POST y PUT.
- La descarga recorre todas las páginas antes de reemplazar la base, para no truncar el inventario.

## Pruebas automatizadas

Comando: `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:assembleDebugAndroidTest`.
Resultado local: **73 pruebas, 0 fallos, 0 errores**, más APK y APK de instrumentación compilados.

Ocho casos nuevos en `commonTest/data/repository/ProductoRepositorioOfflineTest.kt`:

| Caso | Comprobación |
|---|---|
| Con red | La respuesta se guarda y el Flow la muestra. |
| Sin red | El alta queda visible, pendiente y con ID negativo. |
| Reconexión | POST precede a GET; el reintento no duplica el alta confirmada. |
| Reemplazo local | Descargar sincronizados conserva las altas pendientes. |
| GET fallido | Un POST confirmado no se reenvía tras fallar la descarga. |
| Conflicto | El pendiente permanece y no se continúa con la descarga. |
| IDs locales | Dos altas tienen IDs distintos y no colisionan con positivos. |
| Backend apagado | El inventario anterior permanece disponible. |

XML originales y salida Gradle: `evidencias/sesion11/pruebas/`.

## Cómo reproducir el ciclo Android

1. Iniciar PharmaSoft con sus credenciales locales y entrar a PharmaMobil como admin.
2. Abrir Productos, sincronizar y cerrar el proceso completamente.
3. Activar modo avión y desactivar wifi/datos; abrir de nuevo la app.
4. Verificar el listado y el aviso de desconexión. Registrar un nombre único.
5. Comprobar `pendiente = 1` e ID negativo. Desactivar modo avión y pulsar Sincronizar.
6. En Swagger, autorizar con JWT y consultar `GET /api/v1/productos/{id}`. Debe responder
   200 con un ID positivo y el mismo nombre, precio y stock.

La instrumentación opera el ViewModel real de la pantalla para llenar y guardar el formulario;
las capturas son del emulador real. No son pantallas simuladas. Los registros de cada fase
se conservan en `evidencias/sesion11/android/`.

## Alcance y límites

El reintento de una alta cuyo POST respondió correctamente está cubierto. Si el servidor
confirma internamente un POST pero su respuesta se pierde antes de llegar al dispositivo,
la garantía de exactamente una creación requiere una clave de idempotencia en PharmaSoft;
no se afirma que esta sincronización básica resuelva ese caso distribuido.

La práctica usa un botón de sincronización, como la guía; no requiere monitor automático
ConnectivityManager/NWPathMonitor. Las ampliaciones de la actividad autónoma (antigüedad,
bitácora de cinco situaciones y guion de sustentación) no se confunden con esta entrega práctica.

## Resultado real Android

La ejecución final del 20/10/2026 aprobó las tres pruebas instrumentadas.
Se activó modo avión (`airplane_mode_on=1`), se cerró el proceso y se reabrió la app.
El producto **AA S11 Paredes 141745**, creado sin red, llegó a PharmaSoft con
**ID 8**, precio **S/ 4.50** y stock **20**. Swagger respondió **200** al consultar ese ID.
Las cuatro capturas Android y la captura Swagger corresponden a esa misma ejecución.
El reloj del emulador está en UTC; el backend local registra la hora de America/Lima.

Para repetir la automatización con el backend activo, define `PHARMA_ADMIN_PASSWORD`
en el entorno y ejecuta `python ci/ejecutar_offline_s11.py`. La contraseña no se guarda
en el código ni en los registros. El script restaura la conectividad del emulador incluso
si falla la fase offline.

## Estado de evidencias iOS

La ejecución se realiza en macOS mediante GitHub Actions. Se han preparado pruebas de SQLite
nativo y una prueba XCTest que abre con red, apaga el backend de laboratorio y vuelve a abrir
el listado persistente. El backend de CI usa H2 en modo Oracle, Flyway y credenciales efímeras;
Android se verifica contra el backend local MySQL. Hasta obtener el resultado y las capturas
no se considera acreditada la ejecución de iOS.

La fábrica iOS usa `inMemoryDriver(PharmaDb.Schema)`: el nombre literal `:memory:` del
constructor nativo no garantiza aislamiento y fue corregido al ejecutar las pruebas reales.
Ejecución corregida: https://github.com/DJuanParedes/pharmaMobil/actions/runs/39482764173
