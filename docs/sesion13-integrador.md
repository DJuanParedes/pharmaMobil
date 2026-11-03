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
