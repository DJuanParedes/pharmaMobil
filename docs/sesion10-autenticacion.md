# Sesión 10 Autenticación JWT

## Alcance

Práctica individual de Juan Paredes Cárdenas. Continúa las sesiones 7, 8 y 9 en `feature/auth-jwt-paredes`, creada desde `develop` e integrada con la rama propia de sesión 9 mediante un merge explícito. Los commits usan etiquetas `feat`, `fix`, `test`, `docs` y `merge`; su fecha corresponde al desarrollo real, sin fechas inventadas.

El backend es la versión oficial `dreyna/pharmaSoft` en `fff44f9`. No se reimplementa su seguridad. Para ejecutar el laboratorio local sin modificar bases existentes se usa H2 en modo Oracle, con las migraciones Flyway V1, V2 y V3. Solo se adaptan los índices SQL sobre `upper(...)` a índices de columna para H2, en una copia temporal. Esa adaptación no sustituye la validación de Oracle en producción.

## Arquitectura

- Dominio: Credenciales, Sesion, ErrorSesion, AuthRepository y casos de uso; no importa HTTP ni clases de plataforma.
- Datos: DTO del login, restauración mediante `/auth/me`, almacén seguro y cliente Ktor.
- Android: EncryptedSharedPreferences, AES256 y MasterKey en Android Keystore. Se conserva esta API solicitada por la guía aunque AndroidX la deprecó en 1.1. Se desactiva el respaldo de la app para evitar restaurar preferencias cifradas sin su llave.
- iOS: Keychain Generic Password, cuenta `jwt`, servicio de la aplicación y accesibilidad AfterFirstUnlockThisDeviceOnly. Se comprueban los estados de Security y se liberan las consultas Core Foundation.
- Presentación: SesionViewModel, login validado, restauración, cierre y navegación filtrada por permisos.

## Flujo

1. `POST /api/v1/auth/login` recibe username y password.
2. Se guarda exclusivamente el JWT en el almacén cifrado; no se persiste la contraseña.
3. Se limpia la caché Bearer de Ktor después del login y del logout.
4. Auth adjunta el token a las peticiones del backend; ninguna operación CRUD construye Authorization manualmente.
5. Al reabrir, se lee el token y `/auth/me` reconstruye la identidad y los permisos desde el servidor.
6. Un 401 de una operación protegida borra el token y lleva al login con aviso. Un 401 del login significa credenciales incorrectas.
7. Un 403 conserva el token y muestra falta de permiso, sin enviar al login.
8. Logout borra el token y el estado de navegación. Si no hay conexión al restaurar, se conserva el token y se ofrece reintentar.

Logging usa HEADERS solo en depuración, sanea Authorization y no registra cuerpos de login ni respuestas con JWT. En release el nivel es NONE. No hay renovación porque PharmaSoft no proporciona refresh tokens.

## Usuarios de laboratorio

| Usuario | Contraseña de prueba | Rol |
|---|---|---|
| admin | Consultar guía del curso | ADMIN |
| vendedor | Consultar guía del curso | VENDEDOR |
| almacen | Consultar guía del curso | ALMACENERO |

Las contraseñas se consultan en la guía del docente y no se publican en este repositorio.

## Verificación de roles

Antes de ejecutar la matriz, define PHARMA_ADMIN_PASSWORD, PHARMA_VENDEDOR_PASSWORD y PHARMA_ALMACEN_PASSWORD con los valores de la guía docente. Usa una base de laboratorio recién iniciada para evitar duplicar los datos sintéticos.

Las pruebas de dispositivo reciben adminPassword y vendedorPassword como parámetros de instrumentación mediante esas variables locales:

    .\gradlew.bat :androidApp:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.adminPassword=$env:PHARMA_ADMIN_PASSWORD" "-Pandroid.testInstrumentationRunnerArguments.vendedorPassword=$env:PHARMA_VENDEDOR_PASSWORD"

Ejecuta `python ci/verificar_auth_s10.py evidencias/sesion10/matriz-roles.json` con el backend local de laboratorio iniciado. El script crea datos sintéticos en esa base y verifica cada código sin guardar tokens.

| Operación | admin | vendedor | almacen |
|---|---|---|---|
| Listar productos | 200 | 200 | 200 |
| Registrar producto | 201 | 403 | 201 |
| Listar clientes | 200 | 200 | 403 |
| Registrar venta | 201 | 201 | 403 |
| Ver reportes | 200 | 403 | 403 |
| Consultar auth/me | 200 | 200 | 200 |

La prueba sin token devuelve 401. Esta matriz se ejecutó contra la API oficial real, no contra un servidor simulado.

## Reproducir Android

Requisitos: JDK 21, Android SDK 36 y emulador operativo. El emulador usa `http://10.0.2.2:8080/api/v1/`; no debe apuntar a localhost.

```powershell
.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug --console=plain
```

Instala la APK en el emulador. Inicia con cada usuario, entra a Productos y comprueba los botones. Admin y almacén pueden escribir catálogo. Vendedor solo puede consultarlo. Almacén no ve Clientes ni Pedidos. Cierra completamente y reabre: la sesión se restaura por el almacén seguro. Usa Cerrar sesión para comprobar su borrado.

Las pruebas MockEngine cubren cambio de usuario y caché, token ausente en login, restauración, logout, 401, 403, logs saneados y logs apagados en release. Son verificaciones de integración del cliente; la matriz HTTP complementa esas pruebas con el servidor real.

## iOS y evidencias

La compilación del framework para dispositivo se realiza en GitHub Actions con `linkDebugFrameworkIosArm64`. No inicia un simulador. La ejecución y las capturas del CRUD iOS siguen pendientes por la indicación del usuario de no abrir simuladores iOS; no se presentan capturas Android como si fueran iOS.

Las pantallas Clientes y Pedidos conservan el comportamiento de las sesiones anteriores; sus permisos se filtran en la navegación. La matriz de clientes y ventas prueba los endpoints reales directamente. El CRUD REST de productos es el flujo integrado y autenticado en la app.

El punto de control de Swagger se documenta con las capturas reales 401, 200 y 403 en evidencias/sesion10/swagger. El JWT del curl se redactó antes de capturar; no se alteraron códigos ni cuerpos de respuesta. La captura del registro corresponde al archivo real obtenido de Logcat y visualizado en un navegador, no a una ventana de Android Studio.

La prueba de dispositivo provoca deliberadamente una eliminación con vendedor mediante el ViewModel, aunque la interfaz oculta ese botón. El servidor devuelve 403, la pantalla muestra el mensaje y se verifica que la sesión continúa. Una segunda prueba introduce un JWT inválido exclusivamente en el emulador para provocar un 401 real y comprobar el borrado y el regreso al login. Esta última prueba valida la reacción al 401; no representa una espera de 3600 segundos.

La compilación iOS sin simulador pasó en https://github.com/DJuanParedes/pharmaMobil/actions/runs/38452598406. Se entrega el registro correspondiente. La ejecución y capturas iOS son la evidencia de plataforma que no se realizó.

## Preparar el backend de laboratorio

Con una copia del backend oficial indicada arriba, JDK 21 y Python:

    python ci/preparar_backend_s10.py RUTA_PHARMASOFT RUTA_TEMPORAL
    RUTA_PHARMASOFT\mvnw.cmd -f RUTA_TEMPORAL\pom.xml -DskipTests package
    java -jar RUTA_TEMPORAL\target\PharmaBackend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev,s10

Sustituye las rutas y mantén el directorio temporal fuera del repositorio. El script genera un secreto JWT local que no debe publicarse. H2 es una base de laboratorio en memoria: al detener el backend se pierden los datos ingresados. Flyway vuelve a cargar las cuentas del curso en el siguiente arranque.

## Referencias

- Guía práctica 10 del curso, 13 de octubre de 2026.
- https://ktor.io/docs/client-bearer-auth.html
- https://ktor.io/docs/client-logging.html
- https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences
- https://github.com/dreyna/pharmaSoft
