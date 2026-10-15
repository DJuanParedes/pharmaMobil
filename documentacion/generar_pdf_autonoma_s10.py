"""Construye el PDF UPeU exclusivamente a partir de evidencias ejecutadas."""
from pathlib import Path
import json,datetime,subprocess,textwrap
from xml.sax.saxutils import escape
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import Paragraph,Table,TableStyle
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib import colors
from PIL import Image
from pypdf import PdfReader
ROOT=Path(__file__).resolve().parents[1];E=ROOT/'evidencias/sesion10-autonoma';A=E/'android'
OUT=ROOT/'output/pdf/S10_ActividadAutonoma_Paredes_Cardenas.pdf';OUT.parent.mkdir(parents=True,exist_ok=True)
for name,file in [('Cal','calibri.ttf'),('CalB','calibrib.ttf'),('CamB','cambriab.ttf'),('Mono','consola.ttf')]:pdfmetrics.registerFont(TTFont(name,str(Path(r'C:\Windows\Fonts')/file)))
pdfmetrics.registerFontFamily('Cal',normal='Cal',bold='CalB')
blue=colors.HexColor('#06396B');muted=colors.HexColor('#526579');light=colors.HexColor('#F1F4F8')
body=ParagraphStyle('body',fontName='Cal',fontSize=10,leading=14,textColor=blue)
small=ParagraphStyle('small',fontName='Cal',fontSize=8.6,leading=11.5,textColor=muted)
center=ParagraphStyle('center',parent=body,alignment=1)
head=ParagraphStyle('head',fontName='CalB',fontSize=9,leading=12,textColor=colors.white)
c=canvas.Canvas(str(OUT),pagesize=(612,792));c.setTitle('S10 Actividad Autónoma Paredes Cárdenas');c.setAuthor('Juan Paredes Cárdenas')
n=0
def text(s,x,y,w=512,style=body):
 p=Paragraph(s,style);_,h=p.wrap(w,700);p.drawOn(c,x,y-h);return y-h-10
def start(title,part='ACTIVIDAD AUTÓNOMA N.º 10'):
 global n
 if n:c.showPage()
 n+=1
 c.setFont('Cal',8);c.setFillColor(muted);c.drawString(50,764,'UPeU | Facultad de Ingeniería y Arquitectura | EP Ingeniería de Sistemas')
 c.drawString(50,740,part)
 c.setFont('CamB',18);c.setFillColor(blue);c.drawString(50,712,title)
 c.setFont('Cal',8);c.setFillColor(muted);c.drawString(50,29,'Juan Paredes Cárdenas | PharmaMobil | MySQL y Android');c.drawRightString(562,29,f'Página {n}')
 return 685
def table(rows,widths,x,y):
 data=[[Paragraph(escape(str(v)),head if i==0 else small) for v in row]for i,row in enumerate(rows)]
 t=Table(data,colWidths=widths);t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),blue),('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,light]),('VALIGN',(0,0),(-1,-1),'TOP'),('TOPPADDING',(0,0),(-1,-1),7),('BOTTOMPADDING',(0,0),(-1,-1),7),('LEFTPADDING',(0,0),(-1,-1),7),('BOX',(0,0),(-1,-1),.5,colors.HexColor('#C5D5E5'))]));_,h=t.wrap(sum(widths),700);t.drawOn(c,x,y-h);return y-h-15
def image(file,x,top,w,h):
 im=Image.open(file);scale=min(w/im.width,h/im.height);ww,hh=im.width*scale,im.height*scale;c.drawImage(str(file),x+(w-ww)/2,top-hh,width=ww,height=hh,mask='auto');return top-hh
def code(s,x,y,w=512,size=8):
 c.setFont('Mono',size);c.setFillColor(blue)
 for raw in s.splitlines():
  for line in textwrap.wrap(raw,width=max(20,int(w/(size*.6))),replace_whitespace=False,drop_whitespace=False)or['']:
   c.drawString(x,y-10,line);y-=size+3
 return y-10
def local(iso):return datetime.datetime.fromisoformat(iso.replace('Z','+00:00')).astimezone(datetime.timezone(datetime.timedelta(hours=-5))).strftime('%d/%m/%Y %H:%M:%S -05')
rows=json.loads((A/'bitacora.json').read_text(encoding='utf-8'))
logout=json.loads((A/'logout.json').read_text(encoding='utf-8'))
restauracion=json.loads((E/'restauracion-final.json').read_text(encoding='utf-8'))
matrix=json.loads((E/'matriz-http.json').read_text(encoding='utf-8'))
ui={r['usuario']:r for r in json.loads((A/'interfaz-roles.json').read_text(encoding='utf-8'))}
assert len(rows)==6 and [r['http'] for r in rows]==[200,401,401,401,401,401]
assert restauracion['expiraEnSegundos']==3600 and restauracion['admin_habilitado']

# Portada institucional
n=1
c.setFont('Cal',8);c.setFillColor(muted);c.drawString(50,29,'Juan Paredes Cárdenas | PharmaMobil | MySQL y Android');c.drawRightString(562,29,'Página 1')
logo=ROOT/'documentacion/logo_upeu.png';image(logo,205,735,202,76)
y=text('Facultad de Ingeniería y Arquitectura · EP Ingeniería de Sistemas',50,644,style=center)
c.setFont('CamB',15);c.setFillColor(blue);c.drawCentredString(306,610,'DESARROLLO DE APLICACIONES MÓVILES')
c.setStrokeColor(colors.HexColor('#F5A400'));c.setLineWidth(1.5);c.line(50,593,562,593)
c.setFont('CamB',21);c.drawCentredString(306,561,'Actividad Autónoma N.º 10')
y=text('Validación de expiración almacenamiento y cierre de sesión',50,543,style=center)
y=table([['Dato','Detalle'],['Estudiante','Juan Paredes Cárdenas'],['Docente','Mg. Reyna Barreto Benjamin David'],['Ciclo y semestre','VI · 2026-2'],['Unidad y sesión','Unidad 2 · Sesión 10'],['Fecha de ejecución','15 de octubre de 2026'],['Entrega límite','19 de octubre de 2026 · 23:59, America/Lima'],['Modalidad','Individual · Android y MySQL 8.4.11'],['Rama de entrega','feature/autonoma-sesion10-paredes']],[133,379],50,y-4)
y=text('Los cuatro productos se verificaron mediante peticiones reales de PharmaMobil a PharmaSoft. La expiración se esperó realmente; se inspeccionó el XML cifrado del emulador; se comprobó el cierre del cliente HTTP y se registró la matriz de permisos.',50,y)
y=text('<link href="https://github.com/DJuanParedes/pharmaMobil/tree/feature/autonoma-sesion10-paredes" color="#087F8C"><u>Abrir rama y commits en GitHub</u></link>',50,y)
text('Se utiliza Android en Windows. No se dispone de una ejecución macOS/iOS para esta actividad y no se atribuye esa validación a otra persona.',50,y,style=small)

y=start('Productos y trazabilidad')
y=table([['Producto','Evidencia incluida'],['1 Bitácora de seis escenarios','Páginas 3 a 8. Pasos, horas, HTTP, mensaje literal, captura Android y conclusión.'],['2 Almacenamiento seguro','Páginas 9 a 11. XML real cifrado, registro Ktor saneado y restauración tras force-stop.'],['3 Cierre completo','Página 12. Eliminación del token, caché HTTP, limpieza de interfaz y prueba de Atrás.'],['4 Matriz de permisos','Páginas 13 y 14. Seis operaciones por tres usuarios y observaciones reales de interfaz.'],['Análisis y repositorio','Páginas 15 y 16. Hallazgos, commits, reproducción y lista de cotejo.']],[160,352],50,y)
y=text('<b>Entorno ejecutado</b><br/>Android Emulator Medium_Phone_API_37.0, paquete pe.edu.upeu.pharmamobil, Java 21, Spring Boot 4 y MySQL 8.4.11. Android utiliza http://10.0.2.2:8080/api/v1/. Las pruebas no usan respuestas MockEngine como evidencia HTTP del servidor.',50,y)
y=text('<b>Preparación reversible</b><br/>El backend se inició temporalmente con --app.jwt.expiracion-minutos=1 y perfil mysql. El escenario 6 deshabilitó admin en MySQL con una sesión abierta. El usuario se reactivó mediante finally y la duración se devolvió a 60 minutos, verificándolo con un login posterior.',50,y)
y=table([['Control final','Resultado'],['Expiración final','3600 segundos, verificada con POST auth/login'],['Estado final de admin','Habilitado en MySQL'],['Pruebas de backend','27 aprobadas: 21 existentes y 6 del filtro JWT'],['Pruebas compartidas','65 host aprobadas en la compilación de esta actividad'],['Pruebas Android de la actividad','3 pruebas de instrumentación reales aprobadas']],[220,292],50,y)
text('Los originales JSON, XML, PNG, logs de prueba y la colección importable en Postman se entregan junto al código en evidencias/sesion10-autonoma/ y documentacion/. Ninguna evidencia publica contraseñas o JWT.',50,y,style=small)

steps={1:'Iniciar sesión como admin. Abrir Productos. Ejecutar GET productos con el mismo cliente autenticado.',2:'Iniciar backend con TTL de un minuto. Hacer login nuevo. Abrir Productos y esperar 65 segundos reales. Ejecutar GET productos sin renovar el token.',3:'Hacer login. Cambiar exactamente el primer carácter de la firma del JWT guardado. Vaciar la caché Bearer para leer el valor alterado. Ejecutar GET productos.',4:'Hacer login y cerrar sesión. Lanzar GET productos con el mismo cliente Ktor, sin añadir Authorization.',5:'Hacer login. Enviar Authorization con prefijo bearer en minúscula desde instrumentación. AuthCircuitBreaker impide que Ktor reemplace ese encabezado deliberado.',6:"Hacer login y mantener Productos abierto. En MySQL ejecutar UPDATE usuarios SET estado=b'0' WHERE username='admin'. Ejecutar GET productos y reactivar después con estado=b'1'."}
conclusions={1:'Una firma válida y un usuario activo permiten consultar el catálogo; la identidad y los permisos se comprueban en el backend.',2:'El vencimiento invalida una sesión sin intervención del usuario. La app detecta el 401, borra el token y vuelve al login.',3:'Cambiar un carácter de la firma basta para invalidar el JWT. El servidor no confía en el contenido de un token alterado.',4:'El logout elimina tanto el valor persistido como la caché del proveedor: la siguiente solicitud realmente llega anónima.',5:'El filtro del backend distingue el prefijo exacto Bearer. La variante deliberada en minúscula se interpreta como una petición anónima.',6:'El backend consulta el estado actual del usuario en cada petición. Deshabilitarlo revoca el acceso de un token todavía vigente.'}
for r in rows:
 i=r['id'];y=start(f'Escenario {i}  '+['Token válido','Token expirado','Token alterado','Sin cabecera','Prefijo incorrecto','Usuario deshabilitado'][i-1],'PRODUCTO 1 / BITÁCORA REAL')
 y=text(f'<b>Inicio:</b> {local(r["inicio"])}<br/><b>Fin:</b> {local(r["fin"])}<br/><b>Plataforma:</b> Android · <b>HTTP recibido:</b> {r["http"]}',50,y)
 y=text('<b>Pasos exactos</b><br/>'+escape(steps[i]),50,y)
 top=y-6;bottom=image(A/f'escenario-{i}.png',50,top,215,415)
 yy=text('<b>Mensaje literal del campo message</b>',285,top,277)
 yy=text(escape(str(r['message'])) if r['message'] is not None else 'La respuesta 200 es un DTO paginado y no incluye campo message.',285,yy,277)
 if i!=1:yy=code(json.dumps(r['respuesta'],ensure_ascii=False,indent=2),285,yy,277,7)
 else:yy=text('La respuesta contiene los productos reales del catálogo y el estado paginado; el cuerpo completo está en bitacora.json. No se registra el token.',285,yy,277)
 yy=text('<b>Qué vio el usuario</b><br/>'+escape(r['interfaz']),285,yy,277)
 text('<b>Conclusión</b><br/>'+escape(conclusions[i]),50,min(bottom,yy)-18)

y=start('Archivo del almacén cifrado','PRODUCTO 2 / COMPROBACIÓN 1')
y=text('Se leyó el archivo real data/data/pe.edu.upeu.pharmamobil/shared_prefs/sesion_segura.xml desde el proceso de instrumentación y se exportó mediante ADB. El ensayo comprueba que el JWT leído con AlmacenSeguro no aparece en el XML en claro.',50,y)
bottom=image(E/'almacen-cifrado.png',50,y-5,512,440)
y=text('Figura 7. Vista del XML real exportado del emulador. La clave de sesión y su valor están cifrados. Los keysets de AndroidX están plegados; esta vista no se presenta como una captura de Device Explorer.',50,bottom-12,style=small)
y=text('<b>Resultado verificado:</b> token_legible=false. Se usa EncryptedSharedPreferences con MasterKey AES256_GCM, cifrado de nombres AES256_SIV y valores AES256_GCM. El acceso descifrado solo se realiza mediante el almacén de la app.',50,y)
text('Reproducción en Android Studio: Device Explorer > data/data/pe.edu.upeu.pharmamobil/shared_prefs/sesion_segura.xml. Los originales antes y después del logout se incluyen como XML para inspección independiente.',50,y,style=small)

y=start('Registro de Authorization saneado','PRODUCTO 2 / COMPROBACIÓN 2')
y=text('Se capturó adb logcat del emulador y se conservaron únicamente líneas Ktor. Authorization aparece como ***; se oculta todo el valor, incluido el prefijo, en lugar de exponer el JWT. Esta redacción satisface el propósito de Bearer *** sin conservar ninguna credencial.',50,y)
image(E/'registro-saneado.png',50,y-5,512,510)
text('Figura 8. Visor del registro real. Logging usa HEADERS únicamente en depuración y NONE en release. No se guardan cuerpos de login ni respuestas con JWT. El TXT íntegro saneado está en evidencias/sesion10-autonoma/ktor-saneado.txt.',50,110,style=small)

y=start('Sesión persistente tras cerrar el proceso','PRODUCTO 2 / COMPROBACIÓN 3')
y=text('Después de un login válido el ejecutor realizó am force-stop pe.edu.upeu.pharmamobil, abrió MainActivity y comprobó el token persistido en una nueva instrumentación. /auth/me reconstruyó la sesión sin introducir credenciales.',50,y)
b=image(A/'antes-cierre-proceso.png',50,y-10,240,430);image(A/'sesion-restaurada-proceso.png',322,y-10,240,430)
y=text('Figura 9. Sesión antes de detener por completo el proceso.',50,b-12,240,small);text('Figura 10. Sesión restaurada después de force-stop y reapertura.',322,b-12,240,small)
text('Conclusión: el JWT cifrado sobrevive a la muerte del proceso. La prueba no se limita a cambiar de pantalla ni a conservar una variable en memoria. El servidor sigue validando firma, expiración y usuario al restaurar.',50,y-15)

y=start('Cierre de sesión completo','PRODUCTO 3 / CUATRO EFECTOS')
y=table([['Orden','Verificación real','Resultado'],['1','AlmacenSeguro.leerToken() es null. El XML ya no contiene la entrada cifrada de sesión.','Aprobado'],['2','GET productos válido antes del logout; GET posterior con el mismo cliente devuelve 401 anónimo.','Aprobado'],['3','SesionViewModel reinicia el estado; no quedan identidad ni datos de la sesión en el login.','Aprobado'],['4','Pulsar Atrás y reabrir no muestra Inventario conectado. Sigue el formulario de login.','Aprobado']],[42,385,85],50,y)
b=image(A/'logout-limpio.png',50,y,230,285);image(A/'atras-sin-sesion.png',332,y,230,285)
y=text('Figura 11. Login tras el cierre.',50,b-10,230,small);text('Figura 12. Atrás y reapertura sin sesión.',332,b-10,230,small)
y=text('<b>Respuesta posterior al cierre</b><br/>HTTP 401 · '+escape(logout['message']),50,y-5)
text('La lectura del almacén después del cierre se conserva en almacen-despues-logout.xml. Solo permanecen los keysets internos; no equivalen a un JWT activo. La caché Bearer se vacía explícitamente en AuthRepositorioRest.cerrarSesion().',50,y,style=small)

y=start('Matriz HTTP y disponibilidad de interfaz','PRODUCTO 4 / TRES USUARIOS')
ops=[('Listar productos','listar_productos'),('Registrar producto','registrar_producto'),('Listar clientes','listar_clientes'),('Registrar venta','registrar_venta'),('Ver reportes','reportes'),('Consultar /auth/me','usuario_actual')]
data=[['Operación','admin','vendedor','almacen']]
for label,key in ops:
 cells=[]
 for user in ['admin','vendedor','almacen']:
  http=matrix['usuarios'][user][key]
  if key in ['listar_productos','registrar_producto','listar_clientes']:offered='Sí' if ui[user][key] else 'No'
  elif key=='registrar_venta':offered='Pedido demo' if ui[user]['pedidos_demo'] else 'No'
  elif key=='reportes':offered='No hay pantalla'
  else:offered='Automática'
  cells.append(f'{http} / {offered}')
 data.append([label,*cells])
y=table(data,[160,117,117,118],50,y)
y=text('Cada celda registra HTTP / interfaz. Los códigos proceden de la API real y la disponibilidad del menú se comprobó en Android. La ausencia de una acción permitida no es un 403 ofrecido por error.',50,y)
y=text('<b>Alcance de cada acción</b><br/>Productos está integrado con REST. Clientes tiene pantalla demostrativa visible para ADMIN y VENDEDOR. Registrar pedido también es una demostración local, no un POST de venta; el registro de venta real de esta matriz se ejecutó directamente contra la API. Reportes no tiene pantalla y /auth/me es automático al restaurar.',50,y)
y=text('<b>Hallazgo y evaluación</b><br/>No se ofrecieron acciones de escritura de catálogo a VENDEDOR ni Clientes/Pedidos a ALMACENERO. El proyecto no presenta el menú como prueba de que todos esos módulos consumen REST. Se conserva esa distinción para no atribuir funcionalidades no implementadas.',50,y)
text('Fuente: matriz-http.json (18 operaciones) e interfaz-roles.json (observación de tres usuarios). Sin token el catálogo devuelve 401.',50,y,style=small)

y=start('Capturas de permisos por rol','PRODUCTO 4 / INTERFAZ REAL')
y=text('Las capturas corresponden a la misma app y al backend MySQL de la actividad. ADMIN y ALMACENERO ven edición y eliminación; VENDEDOR dispone de consulta.',50,y)
for j,user in enumerate(['admin','vendedor','almacen']):
 x=50+j*174;b=image(A/f'productos-{user}.png',x,y-10,164,390);text(user.upper(),x,b-12,164,small)
y=text('<b>Comprobaciones de menú</b><br/>ADMIN y VENDEDOR tienen Clientes y Pedidos. ALMACENERO no los tiene. Las capturas inicio-usuario.png y menu-usuario.png se entregan junto a las de Productos para verificar esa observación.',50,y-445)
text('La acción deliberada de un escenario se inyecta desde instrumentación y no añade botones de prueba a la aplicación de producción.',50,y,style=small)

y=start('Análisis y commits del desarrollo')
y=text('<b>Hallazgos</b><br/>1. Ktor reemplaza normalmente Authorization mediante su proveedor Bearer. El ensayo de prefijo incorrecto necesita AuthCircuitBreaker para preservar la cabecera deliberada.<br/>2. Las excepciones 401 protegidas limpian el almacén y la caché y notifican a la UI. Se detectaron expiración y deshabilitación reales, sin dejar una sesión que aparentara estar activa.<br/>3. Los keysets de AndroidX permanecen tras el logout; comprobar archivo vacío sería incorrecto. Se verifica la desaparición de la entrada de sesión y leerToken()==null.<br/>4. El runner drena la salida de instrumentación en un hilo para no bloquearse si una aserción genera una traza extensa.',50,y)
gitlog=subprocess.check_output(['git','log','origin/develop..HEAD','--reverse','--format=%h|%ad|%s','--date=format-local:%Y-%m-%d %H:%M'],cwd=ROOT).decode('utf-8').strip().splitlines()
commits=[['Commit','Fecha y hora','Cambio']]+[line.split('|',2) for line in gitlog if line]
y=table(commits,[65,115,332],50,y)
y=text('Los commits registran el momento real del trabajo. No se retrocedieron fechas para aparentar un desarrollo durante otra sesión. La rama se integra mediante un merge revisable en GitHub.',50,y,style=small)
y=text('<link href="https://github.com/DJuanParedes/pharmaMobil/tree/feature/autonoma-sesion10-paredes" color="#087F8C"><u>Código y evidencias de la rama</u></link>',50,y)
text('<link href="https://github.com/DJuanParedes/pharmaMobil/commits/feature/autonoma-sesion10-paredes" color="#087F8C"><u>Historial completo de commits</u></link>',50,y)

y=start('Reproducción y lista de cotejo')
y=text('El procedimiento detallado está en docs/sesion10-actividad-autonoma.md. Antes de ejecutar: iniciar MySQL y el emulador, cargar las claves locales y establecer MYSQL_BIN. El script de pruebas no publica secretos.',50,y)
y=code('java -jar target/PharmaBackend-0.0.1-SNAPSHOT.jar `\n  --spring.profiles.active=mysql --app.jwt.expiracion-minutos=1\n# Esperar el arranque, desde la raíz del proyecto:\npython ci/ejecutar_autonoma_s10.py\n# Al terminar: reiniciar el backend con expiración 60.',50,y)
check=[['Criterio de la guía','Estado'],['PDF con nombre solicitado, portada, sesión, fecha y enlace','Sí'],['Seis escenarios con captura, HTTP y mensaje literal','Sí; 200 no contiene campo message'],['Expiración detectada y regreso al login','Sí, espera real de 65 segundos'],['Archivo de almacén no legible y log saneado','Sí, XML/ADB y Logcat reales'],['Cuatro efectos del logout, incluido el cliente HTTP','Sí'],['Matriz con tres usuarios y conducta de interfaz','Sí, con alcance demo/REST explícito'],['Expiración restaurada y admin reactivado','Sí, 3600 segundos y estado 1'],['Commits accesibles en la rama de entrega','Sí']]
y=table(check,[378,134],50,y)
y=text('<b>Postman</b><br/>Se entrega documentacion/S10_Autonomia_Postman.json para importar. La actividad no exige esa herramienta; las evidencias HTTP de este informe provienen de Android/Ktor. No se presentan vistas de archivos como si fueran Postman o Device Explorer.',50,y)
y=text('<b>Referencias y procedencia</b><br/>Guía docente: Actividad Autónoma N.º 10, Validación de expiración, almacenamiento y cierre de sesión, 7 páginas, entrega 19/10/2026. Código del backend basado en dreyna/pharmaSoft, fff44f9. Fuentes de ejecución: bitacora.json, logout.json, interfaz-roles.json, matriz-http.json y restauracion-final.json.',50,y,style=small)
c.save();print(str(OUT));print('Páginas:',len(PdfReader(OUT).pages))
