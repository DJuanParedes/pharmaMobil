# Actividad autónoma 10 Sesión y token

Juan Paredes Cárdenas. Desarrollo de Aplicaciones Móviles, ciclo VI, semestre 2026-2.
Docente: Mg. Reyna Barreto Benjamin David. Entrega individual, Android y MySQL.

Rama: `feature/autonoma-sesion10-paredes`. PDF: `output/pdf/S10_ActividadAutonoma_Paredes_Cardenas.pdf`.

## Qué se entrega

Los cuatro productos son la bitácora de seis escenarios, almacenamiento cifrado, cierre completo y matriz de roles con HTTP y disponibilidad de la interfaz. Las evidencias se obtienen del proyecto real ejecutado contra MySQL. No se ejecuta iOS ni se inventa un integrante que lo haya verificado.

## Reproducción

1. Iniciar MySQL, JDK 21 y el emulador Android. Cargar `DB_PASSWORD`, `JWT_SECRET` y las claves de prueba `PHARMA_ADMIN_PASSWORD`, `PHARMA_VENDEDOR_PASSWORD`, `PHARMA_ALMACEN_PASSWORD` en el entorno. No publicar sus valores.
2. Desde `backend/pharmaSoft`, compilar y ejecutar con expiración temporal de un minuto:

```powershell
.\mvnw.cmd -DskipTests package
java -jar target\PharmaBackend-0.0.1-SNAPSHOT.jar --spring.profiles.active=mysql --app.jwt.expiracion-minutos=1
```

3. Desde la raíz, compilar la app y las pruebas:

```powershell
.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:assembleDebugAndroidTest
$env:MYSQL_BIN = 'RUTA_A_MYSQL\bin\mysql.exe'
python ci/ejecutar_autonoma_s10.py
```

El ejecutor instala las APK, provoca los seis escenarios, coordina la deshabilitación temporal en MySQL y reactiva al usuario en un bloque `finally`. Luego realiza un `am force-stop`, reabre la aplicación, verifica el cierre y recorre los tres roles. Exporta capturas y respuestas sin JWT. `MYSQL_BIN` debe apuntar al cliente real y `DB_PASSWORD` debe corresponder al usuario `pharma_app`.

4. Al terminar, detener el backend temporal y volver a ejecutarlo con 60 minutos. Mantener el mismo secreto JWT y comprobar que el login informa `expiraEnSegundos=3600`. Verificar también `SELECT username, estado+0 FROM usuarios WHERE username='admin';`: debe devolver 1.

```powershell
java -jar target\PharmaBackend-0.0.1-SNAPSHOT.jar --spring.profiles.active=mysql --app.jwt.expiracion-minutos=60
```

5. Ejecutar la matriz HTTP real, que crea datos sintéticos con identificadores distintos:

```powershell
$env:PHARMA_DB_DESCRIPCION = 'MySQL 8.4 con Flyway V1 V2 V3'
python ci/verificar_auth_s10.py evidencias/sesion10-autonoma/matriz-http.json
```

## Precauciones que forman parte de la prueba

La expiración se provoca esperando 65 segundos después de un login del backend con TTL de un minuto. No se sustituye por un token inventado ni por un reloj falso. El token alterado cambia un carácter de su firma. El prefijo minúsculo se inyecta con `AuthCircuitBreaker` exclusivamente desde instrumentación: Ktor normalmente corrige Authorization mediante el proveedor Bearer, por lo que ese mecanismo permite que la cabecera deliberadamente incorrecta llegue realmente al servidor.

El almacén se inspecciona leyendo el XML real del proceso de la app. Se comprueba que el JWT completo no aparece, y que la entrada cifrada de sesión desaparece después del logout. Los keysets internos de AndroidX permanecen y no representan una sesión activa. El visor HTML muestra esas lecturas reales de ADB; no se presenta como una ventana de Device Explorer. El registro se obtiene de `adb logcat` y solo se conserva el subconjunto Ktor.

La prueba de cierre calienta la caché Bearer con un GET válido antes de pulsar Cerrar sesión. Después comprueba token nulo, estado vacío, GET 401 sin autenticación y que Atrás/reabrir no devuelve al listado. La respuesta literal se conserva en `logout.json`.

## Matriz de interfaz

La API y la interfaz se registran por separado. Productos está integrado con REST; Clientes y Pedidos conservan las pantallas demostrativas de las sesiones anteriores. El acceso al menú no se confunde con un POST de venta real. Reportes y `/auth/me` no tienen una acción manual en el menú: el primero se prueba directamente en la API y el segundo se consulta automáticamente al restaurar sesión.

## Postman

La guía no exige Postman. Se entrega adicionalmente `documentacion/S10_Autonomia_Postman.json` para importar y repetir las peticiones. Define `adminPassword` solo localmente y no exportes los valores de JWT ni contraseñas. El caso expirado requiere esperar después del login; el caso deshabilitado requiere modificar y restaurar el usuario en MySQL. No se presentan capturas de otro programa como si fueran Postman.

## Archivos de evidencia

`evidencias/sesion10-autonoma/android/` contiene las capturas, `bitacora.json`, XML cifrado antes/después, `logout.json` e `interfaz-roles.json`. El directorio superior contiene la matriz HTTP, el registro saneado y los resultados de instrumentación. El PDF integra esos cuatro productos, los mensajes literales, horas, conclusiones, enlaces de rama y commits y la lista de cotejo.
