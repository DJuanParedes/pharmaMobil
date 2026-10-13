# PharmaSoft con MySQL y JWT

Backend Spring Boot basado en [dreyna/pharmaSoft](https://github.com/dreyna/pharmaSoft), revisión fff44f9. Se incorporó MySQL sin cambiar los contratos que utiliza PharmaMobil Android.

## Requisitos

Java 21 y MySQL 8.4. Maven se descarga mediante el wrapper incluido. No se necesita Oracle ni H2 para el perfil MySQL.

## Preparación de la base

Con una cuenta administradora de MySQL, crea una base nueva y un usuario exclusivo. Sustituye el marcador por una contraseña propia; no guardes la contraseña en Git.

```sql
CREATE DATABASE pharma_s10 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci;
CREATE USER 'pharma_app'@'localhost' IDENTIFIED BY '<tu-clave-local>';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES, DROP
ON pharma_s10.* TO 'pharma_app'@'localhost';
```

Define `DB_PASSWORD` y `JWT_SECRET` en tu entorno. La clave JWT debe ser Base64 de al menos 32 bytes aleatorios. Opcionalmente configura `DB_URL`, `DB_USER`, `SERVER_PORT` y `CORS_ORIGINS`.

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
.\Iniciar-Backend.ps1
```

En la laptop de Juan las claves están únicamente en `C:\Users\juanp\Documents\Cursos\Sesion10\mysql-local\credenciales.local.json`. Para ejecutar con ese archivo:

```powershell
.\Iniciar-MySQL-Local.ps1
.\Iniciar-Backend.ps1 -ArchivoClaves C:\Users\juanp\Documents\Cursos\Sesion10\mysql-local\credenciales.local.json
```

La instancia local de MySQL también debe estar encendida. Sus datos están en la carpeta `mysql-local\data`; no elimines esa carpeta para reiniciar.

Swagger: http://localhost:8080/swagger-ui.html. Android Emulator: `http://10.0.2.2:8080/api/v1/`.

## Funcionamiento

Flyway aplica V1 (catálogo, clientes y ventas), V2 (usuarios y roles) y V3 (usuarios del laboratorio) desde `src/main/resources/db/mysql`. Hibernate valida el esquema, sin borrar ni recrear tablas. Los datos permanecen en MySQL después de cerrar el backend.

`POST /api/v1/auth/login` verifica BCrypt y devuelve un JWT; los demás endpoints reciben `Authorization: Bearer <token>`. Sin autenticación se responde 401; con un rol insuficiente, 403. ADMIN dispone de todos los permisos, VENDEDOR registra ventas y atiende clientes, y ALMACENERO gestiona catálogo y stock. Las contraseñas de las cuentas de prueba se mantienen fuera del repositorio.

Los perfiles Oracle originales conservan sus migraciones en un directorio independiente. Para MySQL se adaptaron identidades AUTO_INCREMENT, tipos BIT/DECIMAL/DATETIME, índices únicos funcionales y fechas CURRENT_TIMESTAMP. No se ejecutaron pruebas iOS.
