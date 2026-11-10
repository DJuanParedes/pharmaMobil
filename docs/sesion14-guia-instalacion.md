# Guía de instalación de PharmaMobil 1.0.0 (Equipo 2)

Actividad autónoma de la sesión 14. Sirve para que cualquier persona instale y pruebe el producto entregado
sin Android Studio. Versión `1.0.0` (versionCode `1`), identificador `pe.upeu.pharmamobil`, etiqueta Git `v1.0-final`.

## 1. Qué se entrega

| Archivo | Para qué sirve | Se instala en un teléfono |
|---|---|---|
| `Equipo2_ProyectoFinal_Android.aab` | Paquete de aplicación para la tienda (entregable E3). Contiene todas las configuraciones. | No (lo convierte la tienda o `bundletool`) |
| `Equipo2_ProyectoFinal_Android.apk` | Instalable firmado para probar y demostrar. | **Sí** |
| `Equipo2_ProyectoFinal_iOS.xcarchive.zip` | Archivo de Xcode generado con *Product › Archive* (ver limitación de iOS). | No (sin cuenta de Apple de pago no se exporta `.ipa`) |

Los tres archivos están en la rama [`evidencias-s14-ci`](https://github.com/DJuanParedes/pharmaMobil/tree/evidencias-s14-ci/entrega)
y [`evidencias-s14-ios`](https://github.com/DJuanParedes/pharmaMobil/tree/evidencias-s14-ios/entrega-ios).
Huella SHA-256 del certificado que firma el APK y el AAB:
`00:0D:3C:8A:39:15:29:59:17:3B:23:5C:B3:B6:1A:F1:99:77:CF:96:2B:2A:90:D8:97:85:90:D9:05:83:EF:53`.

## 2. Preparar el servicio PharmaSoft en la PC (Windows)

El teléfono se conecta a PharmaSoft por la red Wi-Fi. La PC y los teléfonos deben estar **en la misma red**.

1. Encender MySQL y el backend (ver [backend/pharmaSoft/README.md](../backend/pharmaSoft/README.md)):
   ```powershell
   cd backend\pharmaSoft
   .\Iniciar-MySQL-Local.ps1
   .\Iniciar-Backend.ps1 -ArchivoClaves <ruta-a-tus-credenciales.local.json>
   ```
2. Averiguar la IP de la PC: `ipconfig` → «Dirección IPv4» del adaptador Wi-Fi (por ejemplo `192.168.1.40`).
3. Abrir el puerto 8080 en el cortafuegos (PowerShell **como administrador**, una sola vez):
   ```powershell
   New-NetFirewallRule -DisplayName "PharmaSoft 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow -Profile Private
   ```
   La red Wi-Fi debe estar marcada como **Privada** en Windows.
4. Comprobar desde el navegador del teléfono: `http://192.168.1.40:8080/swagger-ui.html` debe abrir Swagger.
   Si no abre, la app tampoco podrá conectarse: revisar red, IP y cortafuegos antes de seguir.

> **La trampa de la dirección.** `10.0.2.2` solo existe dentro del emulador. El APK de entrega se compila con la
> IP de la PC (`-PpharmaApiUrl=http://<IP>:8080/api/v1/`, o el *Secret* `PHARMA_API_URL` en GitHub Actions).
> Si la IP de la PC cambia, hay que volver a compilar el APK con la nueva dirección.

## 3. Instalar el APK en un teléfono Android

**Opción A, sin cable (recomendada para la prueba «sin entorno conectado»):**

1. Copiar `Equipo2_ProyectoFinal_Android.apk` al teléfono (Google Drive, WhatsApp «mensaje a mí mismo», correo o USB).
2. Abrir el archivo desde *Archivos* / *Descargas*. Android pedirá permitir «Instalar apps desconocidas» para esa
   aplicación: aceptar solo para esta instalación.
3. Pulsar **Instalar**. Si aparece Play Protect, elegir «Instalar de todas formas» (la app no está en la tienda).

**Opción B, con cable y adb:** activar *Opciones de desarrollador › Depuración por USB* y ejecutar
`adb install -r Equipo2_ProyectoFinal_Android.apk`. Después **desconectar el cable** antes del recorrido.

Si ya había una versión de depuración instalada, desinstalarla primero: tiene otra firma y Android no permite
actualizar una app con una clave distinta (es justamente la garantía que da la firma).

## 4. Cuentas de prueba

| Usuario | Rol | Puede |
|---|---|---|
| `admin` | ADMIN | Todo, incluido el CRUD de productos |
| `almacen` | ALMACENERO | Catálogo y stock |
| `vendedor` | VENDEDOR | Leer catálogo, clientes y ventas; **no** puede crear/editar/eliminar productos |

Las contraseñas no se publican en el repositorio: las entrega el responsable del equipo o la guía del curso.

## 5. Recorrido de aceptación (8 pasos)

Es el mismo recorrido de la guía (paso 7) y de la [matriz de pruebas](sesion14-matriz-pruebas.md):
instalación desde el archivo · inicio de sesión correcto e incorrecto · CRUD con un rechazo de validación ·
operación sin privilegio (usuario `vendedor`) · modo avión: abrir, leer y crear · recuperar la red y sincronizar ·
permiso de notificaciones: conceder, denegar y denegar para siempre · cerrar sesión y reabrir.

## 6. Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| «Sin conexión con PharmaSoft» con Wi-Fi activo | IP equivocada, cortafuegos o red pública | Paso 2.2–2.4; probar Swagger desde el teléfono |
| «La app no se instaló» / «conflicto con un paquete existente» | Hay una versión con otra firma | Desinstalar la versión anterior |
| «Usuario o contraseña incorrectos» | Contraseña o usuario mal escritos | Verificar mayúsculas; las cuentas son `admin`, `almacen`, `vendedor` |
| La lista queda vacía tras iniciar sesión | Backend sin productos | Crear uno desde la app o desde Swagger |
| No aparece el diálogo de notificaciones | Android 12 o inferior concede el permiso al instalar | En esos equipos el estado es «concedido» desde el inicio |

## 7. Checklist de entrega (16 de noviembre)

- [ ] `Equipo2_ProyectoFinal_Android.aab` firmado y verificado (`apksigner`/`jarsigner`, huella del equipo).
- [ ] `Equipo2_ProyectoFinal_Android.apk` firmado, instalado y recorrido en dos teléfonos reales.
- [ ] `versionCode 1`, `versionName 1.0.0`, `applicationId pe.upeu.pharmamobil` iguales en el informe y en el archivo.
- [ ] Archivo de iOS (`.xcarchive`) o evidencia con la limitación declarada.
- [ ] Etiqueta `v1.0-final` sobre el commit compilado.
- [ ] `equipo2.jks` y contraseñas fuera del repositorio, con copia fuera de la PC del responsable.
- [ ] Matriz de pruebas con dos dispositivos y registro de defectos (identificador, pasos, severidad, estado).
- [ ] Limitaciones declaradas (L-1 a L-5) y trabajo futuro en el informe.
- [ ] Rama `feature/builds-14` fusionada en `develop` después de la verificación.
- [ ] Tercer dispositivo recorrido (actividad autónoma).
