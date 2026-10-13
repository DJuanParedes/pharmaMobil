"""Prepara PharmaSoft JWT con H2 y Flyway sin editar el repositorio oficial."""
from pathlib import Path
import base64,secrets,shutil,sys,re,xml.etree.ElementTree as ET
source,output=map(lambda value:Path(value).resolve(),sys.argv[1:3])
output.mkdir(parents=True,exist_ok=True)
ns='http://maven.apache.org/POM/4.0.0';ET.register_namespace('',ns)
tag=lambda n:f'{{{ns}}}{n}'
tree=ET.parse(source/'pom.xml');pom=tree.getroot()
dep=ET.SubElement(pom.find(tag('dependencies')),tag('dependency'))
for name,value in [('groupId','com.h2database'),('artifactId','h2'),('scope','runtime')]:
 ET.SubElement(dep,tag(name)).text=value
build=pom.find(tag('build'))
ET.SubElement(build,tag('sourceDirectory')).text=str(source/'src/main/java')
ET.SubElement(build,tag('directory')).text=str(output/'target')
resources=output/'resources'
shutil.copytree(source/'src/main/resources',resources,dirs_exist_ok=True)
for sql in (resources/'db/migration').glob('*.sql'):
 text=sql.read_text(encoding='utf-8')
 text=re.sub(r'create unique index ([^;]+) on ([^;]+) \(upper\(([^)]+)\)\);',r'create unique index \1 on \2 (\3);',text)
 sql.write_text(text,encoding='utf-8')
res=ET.SubElement(ET.SubElement(build,tag('resources')),tag('resource'))
ET.SubElement(res,tag('directory')).text=str(resources)
config=resources/'application-s10.properties'
if not config.exists():
 secret=base64.b64encode(secrets.token_bytes(64)).decode()
 config.write_text('\n'.join([
 'spring.datasource.url=jdbc:h2:mem:pharma;MODE=Oracle;DB_CLOSE_DELAY=-1',
 'spring.datasource.driver-class-name=org.h2.Driver',
 'spring.datasource.username=sa','spring.datasource.password=',
 'spring.jpa.database-platform=org.hibernate.dialect.H2Dialect',
 'spring.jpa.hibernate.ddl-auto=none','spring.jpa.show-sql=false',
 'spring.flyway.baseline-on-migrate=false','app.jwt.secret='+secret,
 'app.jwt.expiracion-minutos=60','logging.level.org.hibernate.SQL=INFO',
 'logging.level.org.hibernate.orm.jdbc.bind=INFO']),encoding='utf-8')
tree.write(output/'pom.xml',encoding='utf-8',xml_declaration=True)
print('Entorno preparado con secreto local generado. No publicar el directorio temporal.')
