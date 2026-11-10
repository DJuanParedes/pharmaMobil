"""Sesión 14: empaqueta PharmaSoft para Azure App Service con H2 en archivo persistente (/home/data).
Reutiliza ci/preparar_backend_s10.py. El jar NO lleva secretos: la llave JWT llega por la variable JWT_SECRET
(configuración de la aplicación en Azure). Las cuentas de prueba son las del curso (migración V3)."""
import subprocess, sys
from pathlib import Path
raiz = Path(__file__).resolve().parents[2]
salida = Path(sys.argv[1]).resolve()
subprocess.run([sys.executable, str(raiz / 'ci/preparar_backend_s10.py'), str(raiz / 'backend/pharmaSoft'), str(salida)], check=True)
recursos = salida / 'resources'
(recursos / 'application-s10.properties').unlink(missing_ok=True)  # contiene una llave generada: no se empaqueta
(recursos / 'application-azure.properties').write_text('\n'.join([
    'server.port=${PORT:8080}',
    'spring.datasource.url=${DB_URL:jdbc:h2:file:/home/data/pharma;MODE=Oracle;DB_CLOSE_ON_EXIT=FALSE}',
    'spring.datasource.driver-class-name=org.h2.Driver',
    'spring.datasource.username=sa', 'spring.datasource.password=',
    'spring.jpa.database-platform=org.hibernate.dialect.H2Dialect',
    'spring.jpa.hibernate.ddl-auto=none', 'spring.jpa.show-sql=false',
    'spring.flyway.locations=classpath:db/migration',
    'app.jwt.secret=${JWT_SECRET}',
    'app.jwt.expiracion-minutos=60',
    '']), encoding='utf-8')
print('PharmaSoft para Azure preparado en', salida)
