# Lista de cotejo de la práctica 10

| Número | Criterio | Estado y evidencia |
|---|---|---|
| 1 | Login obtiene JWT | Verificado en Android con los tres usuarios y en API real |
| 2 | AlmacenSeguro por plataforma | Android cifrado verificado en dispositivo; Keychain iOS implementado y compilado, ejecución pendiente |
| 3 | Auth adjunta Authorization | Verificado en MockEngine y solicitudes Android |
| 4 | sendWithoutRequest evita primer 401 | Pruebas registran una solicitud protegida por operación con su token |
| 5 | Authorization saneada | Logcat real y prueba automática; Ktor muestra Authorization con tres asteriscos |
| 6 | Sesión sobrevive a cierre y reapertura | Verificado en Android y captura sesion-restaurada |
| 7 | 401 cierra sesión y vuelve al login | Prueba de dispositivo y captura 401-regreso-login |
| 8 | 403 mantiene sesión y avisa | Prueba de dispositivo y captura 403-mensaje-usuario |
| 9 | Acciones ocultas por permisos | Vendedor solo lectura, almacén sin Clientes y Pedidos |
| 10 | Android e iOS autenticados en ejecución | Android verificado; iOS compila pero ejecución y capturas pendientes |

## Puntos de control

1. Swagger: capturas 401 sin token, 200 con admin y 403 en clientes con almacén.
2. Dominio: Sesion.kt, AuthRepository.kt y ErrorSesion.kt sin dependencias HTTP ni de plataforma.
3. Koin: almacenes registrados en los platformModule de Android e iOS.
4. Logging: cabecera saneada y nivel NONE fuera de depuración.
5. Persistencia: token cifrado y restauración con auth/me al reabrir Android.
6. Almacén: acceso a catálogo con escritura y navegación sin Clientes ni Pedidos.

## Límites de la entrega

No se abrió un simulador iOS, conforme a la indicación del usuario. La compilación del framework para dispositivo no demuestra su ejecución ni el funcionamiento del Keychain en un dispositivo físico. Las capturas iOS requeridas por la guía no se fabricaron.

Las verificaciones backend se ejecutaron sobre el código oficial con H2 modo Oracle y tres migraciones Flyway; la adaptación de índices y el entorno temporal se detallan en sesion10-autenticacion.md. No se valida aquí una instalación Oracle.
