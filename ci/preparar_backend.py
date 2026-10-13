"""POM temporal de laboratorio: conserva el código oficial y utiliza H2."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

source, output = map(lambda p: Path(p).resolve(), sys.argv[1:3])
output.mkdir(parents=True, exist_ok=True)
namespace = 'http://maven.apache.org/POM/4.0.0'
ET.register_namespace('', namespace)
tag = lambda name: f'{{{namespace}}}{name}'
tree = ET.parse(source / 'pom.xml')
pom = tree.getroot()
dep = ET.SubElement(pom.find(tag('dependencies')), tag('dependency'))
for key, value in [('groupId', 'com.h2database'), ('artifactId', 'h2'), ('scope', 'runtime')]:
    ET.SubElement(dep, tag(key)).text = value
build = pom.find(tag('build'))
ET.SubElement(build, tag('sourceDirectory')).text = str(source / 'src/main/java')
ET.SubElement(build, tag('directory')).text = str(output / 'target')
resource = ET.SubElement(ET.SubElement(build, tag('resources')), tag('resource'))
ET.SubElement(resource, tag('directory')).text = str(source / 'src/main/resources')
tree.write(output / 'pom.xml', encoding='utf-8', xml_declaration=True)
