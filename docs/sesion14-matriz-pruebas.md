# Matriz de pruebas · PharmaMobil 1.0.0 (Equipo 2)

Recorrido del paso 7 de la guía con el **instalable de entrega firmado** (`Equipo2_ProyectoFinal_Android.apk`,
versionName 1.0.0, versionCode 1, `pe.upeu.pharmamobil`), instalado desde el archivo y sin Android Studio conectado.
Servicio: PharmaSoft en Azure (`https://pharmasoft-equipo2-upeu.azurewebsites.net`) para los teléfonos; PharmaSoft
sobre H2 en el runner para el emulador.

## Dispositivos

| Id | Tipo | Marca y modelo | Android (API) | Red | Instalación |
|---|---|---|---|---|---|
| E1 | Emulador de CI (referencia del paso 5) | Google sdk_gphone64_x86_64, perfil Pixel 6 | 14 (34) | Red del runner | `adb install` del APK de entrega minificado (run 42913534563) |
| R1 | Teléfono real | _pendiente_ | _pendiente_ | Wi-Fi / datos → Azure | APK firmado copiado al teléfono |
| R2 | Teléfono real | _pendiente_ | _pendiente_ | Wi-Fi / datos → Azure | APK firmado copiado al teléfono |

## Resultados por paso

✅ cumple · ⚠️ cumple con observación (ver defecto) · ❌ no cumple · ⏳ pendiente

| N.º | Paso del recorrido | Qué se observa | E1 | R1 | R2 |
|---|---|---|---|---|---|
| 1 | Instalación desde el archivo firmado | Instala sin el entorno; nombre e icono definitivos | ✅ [01](../evidencias/sesion14/emulador/01-instalacion-y-apertura.png) [01b](../evidencias/sesion14/emulador/01b-icono-en-el-lanzador.png) | ⏳ | ⏳ |
| 2 | Inicio de sesión correcto e incorrecto | Distingue credencial inválida de falta de conexión | ✅ [02a](../evidencias/sesion14/emulador/02a-login-credencial-invalida.png) [02b](../evidencias/sesion14/emulador/02b-login-sin-conexion.png) [02c](../evidencias/sesion14/emulador/02c-sesion-iniciada.png) | ⏳ | ⏳ |
| 3 | CRUD completo con un rechazo de validación | El mensaje llega al campo que lo originó | ✅ [03a–03f](../evidencias/sesion14/emulador/03c-rechazo-de-validacion.png) | ⏳ | ⏳ |
| 4 | Operación sin el privilegio necesario | Se informa el rechazo y la sesión no se cierra | ✅ [04](../evidencias/sesion14/emulador/04-vendedor-sin-privilegio.png) (403) | ⏳ | ⏳ |
| 5 | Modo avión: abrir, leer y crear | Sigue mostrando datos; lo creado queda pendiente | ✅ [05a](../evidencias/sesion14/emulador/05a-modo-avion-lectura.png) [05b](../evidencias/sesion14/emulador/05b-modo-avion-alta-pendiente.png) | ⏳ | ⏳ |
| 6 | Recuperar la conexión y sincronizar | Sube lo pendiente, sin duplicados, pantalla actualizada | ⚠️ D-01 [06](../evidencias/sesion14/emulador/06-sincronizado.png) | ⏳ | ⏳ |
| 7 | Permiso: conceder, denegar y denegar para siempre | Tres estados; el tercero ofrece ir a Ajustes | ✅ [07a–07d](../evidencias/sesion14/emulador/07b-permiso-denegado-para-siempre.png) | ⏳ | ⏳ |
| 8 | Cerrar sesión y reabrir | Token borrado; equipo limpio para otro usuario | ⚠️ D-02 [08a](../evidencias/sesion14/emulador/08a-reabierta-sin-token.png) [08b](../evidencias/sesion14/emulador/08b-equipo-limpio.png) | ⏳ | ⏳ |

Detalle de E1 paso por paso: [evidencias/sesion14/emulador/recorrido.txt](../evidencias/sesion14/emulador/recorrido.txt).

## Registro de defectos

| Id | Dispositivo y versión | Pasos para reproducir | Resultado esperado | Resultado obtenido | Severidad | Estado |
|---|---|---|---|---|---|---|
| D-01 | E1 · emulador Pixel 6, Android 14 (API 34), APK 1.0.0 | 1. Iniciar sesión como admin y abrir Productos. 2. Activar el modo avión y crear «S14 Loratadina 10» (queda pendiente). 3. Desactivar el modo avión y esperar 45 s sin tocar nada. | Lo pendiente sube y la pantalla se actualiza sola al volver la red. | La barra sigue en «Sin conexión · 1 cambio por enviar» hasta pulsar «Sincronizar ahora». Después sube una sola vez, sin duplicado, y queda «En línea · todo enviado» ([06](../evidencias/sesion14/emulador/06-sincronizado.png)). | Media (estorba el recorrido, no lo impide) | **Pendiente, declarado.** Corregirlo exige un observador de conectividad (`ConnectivityManager.NetworkCallback`) que dispare `SincronizarUseCase`. Es funcionalidad nueva y queda fuera del alcance congelado. |
| D-02 | E1 · emulador Pixel 6, Android 14 (API 34), APK 1.0.0 | 1. Iniciar sesión como admin. 2. Con modo avión, crear «S14 Cetirizina 10» (pendiente). 3. Pulsar «Cerrar sesión». | El equipo queda limpio, pero se avisa antes de descartar cambios sin enviar. | El equipo queda limpio (hueco 7 corregido), pero el alta pendiente se descarta sin aviso ([08b](../evidencias/sesion14/emulador/08b-equipo-limpio.png)). | Media | **Pendiente, declarado como L-1** (diálogo nuevo, fuera del alcance congelado). |
