# Sesión 13 Proyecto integrador: sincronización robusta, permisos y Firebase

Juan Paredes Cárdenas. Desarrollo de Aplicaciones Móviles, ciclo VI, semestre 2026-2.
Rama: `feature/integrador-13`, creada desde `feature/offline-first-paredes` (sesión 11). Cada paso de la guía tiene su propio commit.

## Paso 1. Recorrido del flujo y tabla de huecos

El recorrido fue: iniciar sesión, ver el inventario, registrar un producto con red y sin red, reconectar, sincronizar, cerrar sesión y volver a entrar con otra cuenta. Junto con el recorrido se revisó el código de `ProductoRepositorioOffline`, `SesionRepositorio` y las pantallas.

| # | Hueco encontrado | Severidad | Decisión |
|---|---|---|---|
| 1 | Una fila pendiente que el servidor rechaza (409, 400) se reenvía en cada sincronización para siempre y bloquea la cola. | Alta | **Hoy** (pasos 2 y 3): columnas `intentos` y `ultimoError`, y fallos permanentes marcados. |
| 2 | Un 500 o 503 se trataba igual que un 409: no había distinción entre fallos que se reintentan y fallos que no. | Alta | **Hoy** (paso 3): `ResultadoEnvio` Exito / Transitorio / Permanente. |
| 3 | Reintentos inmediatos y sin límite cuando el servidor está caído. | Media | **Hoy** (paso 3): máximo 5 intentos y espera de 1, 2, 4, 8 y 16 s. |
| 4 | La pantalla solo mostraba «N pendientes». No decía si hay red, si algo falló ni qué tan viejos son los datos. | Alta | **Hoy** (paso 4): barra de estado con antigüedad y acciones. |
| 5 | El usuario no se entera si una fila quedó fallida con la app en segundo plano. | Media | **Hoy** (pasos 5 y 6): notificación local, con permiso y sus 3 estados. |
| 6 | Los errores en producción no se ven: no hay telemetría. | Media | **Hoy** (paso 6): Crashlytics detrás de la interfaz `Telemetria`. |
| 7 | Al cerrar sesión la base local queda con los datos del usuario anterior. | Alta | **Sesión 14**: limpiar `PharmaDb` en el cierre de sesión y avisar si quedan pendientes. |
| 8 | Si el token vence a mitad de un formulario, la app vuelve al login y se pierde lo escrito. | Media | **Sesión 14**: guardar el borrador antes de redirigir. |
| 9 | Reintentar una creación puede duplicar el producto si la respuesta del POST se perdió. | Alta | **Sesión 14**: clave de idempotencia (`Idempotency-Key`) acordada con el backend. Hoy queda mitigado: el 409 por nombre duplicado se marca como fallo permanente visible. |
| 10 | Un error 500 en `registrar` relanzaba la excepción aunque el producto ya estaba guardado localmente. | Baja | **Hoy**: el producto queda pendiente y la sincronización lo trata como transitorio. |
| 11 | Permisos y telemetría en iOS. | Media | **Sesión 14**: `GestorPermisosIos` está declarado como stub y `TelemetriaNula` se usa en iOS. |
| 12 | Pedir el permiso de cámara para escanear códigos de barras. | Baja | **Descartado** por ahora: no hay pantalla de escaneo en el alcance del curso. |

Lo que ya funcionaba bien se dejó igual. El login distingue «credenciales inválidas» de «sin conexión», y la lista vacía tiene su propio texto.

## Pasos 2 a 7, qué se hizo

- **Paso 2. Esquema.** `producto` suma `intentos INTEGER NOT NULL DEFAULT 0` y `ultimoError TEXT`. La tabla `sync_meta` guarda la hora de la última sincronización. `1.sqm` migra las instalaciones existentes sin borrar datos. Las nuevas consultas son `pendientesPorEnviar`, `listarFallidas`, `contarPendientes`, `contarFallidas`, `sumarIntento`, `marcarFallida`, `reintentarFallidas`, `guardarMeta` y `leerMeta`.
- **Paso 3. Caso de uso.** `EnvioRepositorioRest` traduce cada `ErrorApi`:
  - Sin conexión, tiempo agotado o error del servidor → `Transitorio`.
  - 400, 403, 404, 409 o respuesta inválida → `Permanente`.

  `SincronizarUseCase` sube primero y baja después. Un transitorio suma un intento, espera 1, 2, 4, 8 o 16 s y corta la ronda. Al quinto intento la fila queda fallida. Un permanente la marca fallida al momento, la reporta a `Telemetria` y avisa por `Notificador`.
- **Paso 4. Interfaz.** `EstadoSincronizacionBarra` muestra «Sin conexión · 3 cambios por enviar» y «Actualizado hace 15 min». Si hay fallidas, avisa con los botones *Reintentar* y *Revisar*. También tiene *Sincronizar ahora*.
- **Paso 5. Permisos.** `GestorPermisos` (común) tiene tres estados: Concedido, Denegado y DenegadoParaSiempre. `GestorPermisosAndroid` usa `PuentePermisos` (`ActivityResultContracts.RequestPermission` + `CompletableDeferred`) y una marca `pedido_` para distinguir «nunca pedido» de «denegado para siempre». La tarjeta de Inicio explica, pide o lleva a Ajustes según el estado, y vuelve a leer en `ON_RESUME`. El manifiesto declara `POST_NOTIFICATIONS`. `Info.plist` declara cámara y ubicación.
- **Paso 6. Firebase.** GitLive 2.7.0 (`firebase-crashlytics`, `firebase-messaging`) se usa detrás de `Telemetria`. Solo se envían operación, tipo y motivo: nunca tokens ni datos personales.
- **Paso 7. Pruebas** (`./gradlew :shared:testDebugUnitTest`):
  - `SincronizacionTest`: 503 transitorio, 409 permanente, espera creciente, 5 intentos, orden POST→GET y reintento de fallidas.
  - `PermisoNotificacionesTest`: gestor falso con los tres estados.
  - `SincronizacionTextoTest`: textos de estado y antigüedad.
  - `ProductoRepositorioOfflineTest`: actualizado.

## Desviaciones documentadas

1. Las dependencias de Firebase están en `androidMain`, no en `commonMain`. Así el enlace de iOS de la sesión 11 no se rompe mientras no exista `GoogleService-Info.plist`. La interfaz `Telemetria` sigue en común.
2. iOS usa `TelemetriaNula` y un `GestorPermisosIos` de relleno. Quedan para la sesión 14 (hueco 11).
3. El plugin `google-services` se aplica solo si existe `androidApp/google-services.json`. Sin ese archivo la app compila, y `TelemetriaFirebase` no falla porque usa `runCatching`.

## Activar Firebase

1. En la consola de Firebase, crear el proyecto y registrar la app Android `pe.edu.upeu.pharmamobil`.
2. Descargar `google-services.json` y colocarlo en `androidApp/`. Está en `.gitignore`: no se sube.
3. Sincronizar Gradle y ejecutar. Crashlytics recibe los errores permanentes de sincronización.

## Lista de cotejo (14 ítems)

| # | Ítem | Estado |
|---|---|---|
| 1 | Rama `feature/integrador-13` con un commit por paso | ✔ |
| 2 | Tabla de huecos con severidad y decisión | ✔ |
| 3 | Columnas `intentos` y `ultimoError` | ✔ |
| 4 | Migración `1.sqm` | ✔ |
| 5 | `ResultadoEnvio` Exito/Transitorio/Permanente | ✔ |
| 6 | Máximo 5 intentos con espera 1/2/4/8/16 s | ✔ |
| 7 | Subir antes de bajar | ✔ |
| 8 | Pantalla con pendientes, fallidas, antigüedad y acciones | ✔ |
| 9 | `GestorPermisos` común con 3 estados | ✔ |
| 10 | Implementación Android con `PuentePermisos` | ✔ |
| 11 | Manifiesto e `Info.plist` | ✔ |
| 12 | Firebase detrás de `Telemetria` | ✔ (falta `google-services.json` propio) |
| 13 | Pruebas en commonTest con MockEngine y driver en memoria | ✔ |
| 14 | Capturas: 3 estados del permiso y estado de sincronización | Pendiente de tomar en el emulador |
