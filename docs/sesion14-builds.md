# Sesión 14 · Mejoras finales, firma y builds AAB e IPA

Juan Paredes Cárdenas · código 202311735 · grupo 2 (Equipo 2).
Desarrollo de Aplicaciones Móviles, ciclo VI, semestre 2026-2. Fecha de la sesión: 10 de noviembre de 2026.
Rama `feature/builds-14`, creada desde `develop` después de fusionar `feature/integrador-13` (commit `f3e836f`).
Cada paso de la guía tiene su propio commit.

## Paso 1. Ficha de integración cerrada y alcance congelado

Se revisó la tabla de huecos de la sesión 13 ([docs/sesion13-integrador.md](sesion13-integrador.md)) y se
recorrió el código antes de firmar. Cada fila queda en una sola columna.

| # | Hueco (sesión 13) | Severidad | Decisión final | Qué se hizo / motivo |
|---|---|---|---|---|
| 1 | Fila rechazada (409/400) bloqueaba la cola | Alta | **Cerrado** (S13) | Columnas `intentos` y `ultimoError`; fallos permanentes marcados. |
| 2 | 500/503 tratado igual que 409 | Alta | **Cerrado** (S13) | `ResultadoEnvio` Exito / Transitorio / Permanente. |
| 3 | Reintentos sin límite | Media | **Cerrado** (S13) | 5 intentos con espera 1, 2, 4, 8 y 16 s. |
| 4 | Pantalla solo con «N pendientes» | Alta | **Cerrado** (S13) | Barra de estado con red, fallidas y antigüedad. |
| 5 | Fila fallida sin aviso en segundo plano | Media | **Cerrado** (S13) | Notificación local con permiso de tres estados. |
| 6 | Sin telemetría | Media | **Cerrado** (S13) | Crashlytics detrás de `Telemetria` (activa con `google-services.json` propio). |
| 7 | Al cerrar sesión la base local conserva los datos del usuario anterior | Alta | **Se cierra hoy** → cerrado | `ProductoLocal.limpiar()` borra inventario, cola y fecha de sincronización; `AuthRepositorioRest` lo invoca en `cerrarSesion()`. Prueba `CierreSesionLimpiezaTest`. Rompía el paso 8 del recorrido («equipo limpio para otro usuario»). |
| 7b | Aviso antes de cerrar sesión si quedan pendientes | Media | **Se declara** (L-1) | Requiere un diálogo nuevo en la interfaz (rediseño de pantalla, prohibido tras congelar). Para cerrarla: diálogo de confirmación que muestre `contarPendientes()` y ofrezca «Sincronizar y salir». |
| 8 | Token vencido a mitad de formulario pierde lo escrito | Media | **Se declara** (L-2) | Funcionalidad nueva (borradores persistentes). Para cerrarla: guardar el estado del formulario en `SavedStateHandle`/base local antes de redirigir al login. |
| 9 | Reintento de POST puede duplicar si se pierde la respuesta | Alta | **Se declara** (L-3) | Necesita cambio en PharmaSoft (cabecera `Idempotency-Key`), fuera del alcance del cliente. Mitigado: el 409 por nombre duplicado queda como fallo permanente visible. |
| 10 | 500 en `registrar` relanzaba la excepción | Baja | **Cerrado** (S13) | El producto queda pendiente y se trata como transitorio. |
| 11 | Permisos y telemetría en iOS | Media | **Se declara** (L-4) | `GestorPermisosIos` es stub y `TelemetriaNula` se usa en iOS. Para cerrarla: implementar `UNUserNotificationCenter` y GitLive en `iosMain` con `GoogleService-Info.plist`. |
| 12 | Permiso de cámara para escanear códigos | Baja | **Se descarta** | Nunca estuvo en el alcance comprometido: no existe pantalla de escaneo. Trabajo futuro. |
| 13 | URL del servicio fija en `10.0.2.2` (solo emulador) | Alta | **Se cierra hoy** → cerrado | Hallado hoy. La URL llega del manifiesto (`meta-data pharma.api.url`), que la toma de `-PpharmaApiUrl` o de la variable `PHARMA_API_URL`. Sin valor se usa `10.0.2.2` (emulador). |
| 14 | `usesCleartextTraffic="true"` global, sin configuración de seguridad de red | Media | **Se cierra hoy** → cerrado | Hallado hoy. `res/xml/network_security_config.xml` declara el HTTP de PharmaSoft en la red local. |
| 15 | `versionName = "1.0"` y `applicationId` distinto del pedido para la entrega | Baja | **Se cierra hoy** → cerrado en el paso 3 | `versionName 1.0.0`, `versionCode 1`, `applicationId pe.upeu.pharmamobil`. |

### Alcance congelado (10 de noviembre de 2026)

A partir de este commit no entra funcionalidad nueva: ni funciones fuera del alcance, ni cambios de
arquitectura o de librerías, ni rediseños de pantalla, ni migraciones del esquema. Las dos consultas
nuevas de `Producto.sq` (`borrarTodo`, `borrarMeta`) no cambian el esquema ni requieren migración.
El producto que se entrega es: inicio de sesión JWT con roles, CRUD de productos contra PharmaSoft,
inventario offline-first con cola de envío y sincronización, permiso de notificaciones con tres estados,
cierre de sesión que limpia el equipo y telemetría opcional con Firebase.

## Paso 2. Almacén de claves del equipo

Se creó **un solo** almacén para el Equipo 2, fuera de la carpeta del repositorio, con el comando de la guía
(también en [scripts/crear-almacen-equipo2.sh](../scripts/crear-almacen-equipo2.sh), que se niega a crear el
archivo dentro del repositorio):

```bash
keytool -genkeypair -v \
   -keystore equipo2.jks -storetype JKS \
   -keyalg RSA -keysize 2048 -validity 10000 \
   -alias equipo2
```

| Dato | Valor |
|---|---|
| Archivo | `equipo2.jks` (JKS, fuera del repositorio) |
| Alias | `equipo2` |
| Algoritmo | RSA 2048 bits |
| Titular | `CN=Juan Paredes Cardenas, OU=Equipo 2 - Desarrollo de Aplicaciones Moviles, O=Universidad Peruana Union, L=Lima, ST=Lima, C=PE` |
| Vigencia | 10 000 días: del 10/11/2026 al 28/03/2054 |
| Huella SHA-256 | `00:0D:3C:8A:39:15:29:59:17:3B:23:5C:B3:B6:1A:F1:99:77:CF:96:2B:2A:90:D8:97:85:90:D9:05:83:EF:53` |
| Huella SHA-1 | `85:41:DA:35:99:82:CA:DB:5A:BD:4F:67:8C:03:09:E0:36:0E:CE:9D` |
| Contraseñas | Generadas al azar (24 caracteres). **No se transcriben en ningún archivo del repositorio.** |

**Custodia.** El responsable de la entrega (Juan Paredes) conserva el original. Hay una copia fuera del equipo
de un solo integrante: el archivo y sus contraseñas están guardados en la nube personal del responsable, en una
carpeta privada, y como *Secrets* cifrados del repositorio en GitHub (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`), que GitHub nunca vuelve a mostrar. Si el almacén se pierde no hay recuperación:
habría que publicar la aplicación con otro identificador.
