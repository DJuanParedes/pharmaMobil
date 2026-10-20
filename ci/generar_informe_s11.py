"""Genera el informe usando exclusivamente evidencias existentes."""
from pathlib import Path
import json, xml.etree.ElementTree as ET
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4
from reportlab.lib.colors import HexColor, white
from reportlab.platypus import Paragraph, Table, TableStyle
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.utils import ImageReader
from xml.sax.saxutils import escape
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/pdf/S11_Practica_Offline_First_Juan_Paredes.pdf'
OUT.parent.mkdir(parents=True,exist_ok=True)
E=ROOT/'evidencias/sesion11'
PRODUCT=json.loads((E/'producto-servidor.json').read_text(encoding='utf-8-sig'))
W,H=A4; M=42; CW=W-2*M
c=canvas.Canvas(str(OUT),pagesize=A4)
c.setTitle('Práctica 11 - Offline-first - Juan Paredes')
c.setAuthor('Juan Paredes')
NAVY=HexColor('#163147');TEAL=HexColor('#007D69');GRAY=HexColor('#546473')
body=ParagraphStyle('body',fontName='Helvetica',fontSize=10,leading=14,textColor=NAVY)
small=ParagraphStyle('small',parent=body,fontSize=8.5,leading=11)
page=0

def p(text,x,y,width=CW,style=body):
    item=Paragraph(text,style);_,height=item.wrap(width,700);item.drawOn(c,x,y-height);return y-height-10

def start(title,subtitle):
    global page
    page+=1
    c.setFillColor(NAVY);c.rect(0,H-94,W,94,fill=1,stroke=0)
    c.setFillColor(HexColor('#71E1C3'));c.setFont('Helvetica-Bold',9);c.drawString(M,H-27,'DESARROLLO DE APLICACIONES MÓVILES / SESIÓN 11')
    c.setFillColor(white);c.setFont('Helvetica-Bold',19);c.drawString(M,H-55,title)
    c.setFont('Helvetica',9);c.drawString(M,H-76,subtitle)
    c.setFillColor(GRAY);c.setFont('Helvetica',8);c.drawString(M,25,'Juan Paredes | 20 de octubre de 2026 | PharmaMobil')
    c.drawRightString(W-M,25,str(page))
    return H-116

def table(rows,y,widths):
    cells=[[Paragraph(escape(str(v)),small) for v in row] for row in rows]
    t=Table(cells,colWidths=widths,hAlign='LEFT')
    t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),HexColor('#DDF4EC')),('VALIGN',(0,0),(-1,-1),'TOP'),('LINEBELOW',(0,0),(-1,-1),0.3,HexColor('#D9E2E5')),('LEFTPADDING',(0,0),(-1,-1),8),('RIGHTPADDING',(0,0),(-1,-1),8),('TOPPADDING',(0,0),(-1,-1),6),('BOTTOMPADDING',(0,0),(-1,-1),6)]))
    _,height=t.wrap(CW,700);t.drawOn(c,M,y-height);return y-height-12

def image(path,x,top,maxw,maxh):
    img=ImageReader(str(path));iw,ih=img.getSize();scale=min(maxw/iw,maxh/ih)
    w,h=iw*scale,ih*scale;c.drawImage(img,x+(maxw-w)/2,top-h,w,h,mask='auto');return top-h

def pair(title,subtitle,files,captions):
    y=start(title,subtitle);width=(CW-18)/2
    for i,(file,caption) in enumerate(zip(files,captions)):
        x=M+i*(width+18);p(caption,x,y,width,small);image(E/'android'/file,x,y-34,width,570)
    c.showPage()

y=start('Persistencia local y offline-first','Guía práctica de laboratorio N.º 11 | Unidad 2 | Juan Paredes')
y=p('<b>Objetivo:</b> implementar almacenamiento local y sincronización básica sobre el CRUD autenticado de la sesión 10. La interfaz lee la base SQLDelight y conserva el inventario cuando PharmaSoft no responde.',M,y)
y=p('<b>Rama:</b> feature/offline-first-paredes<br/><b>Base:</b> origin/develop, con la sesión 10 integrada.<br/><b>Repositorio:</b> <link href="https://github.com/DJuanParedes/pharmaMobil/tree/feature/offline-first-paredes" color="#007D69">DJuanParedes/pharmaMobil</link>',M,y)
y=table([['Paso solicitado','Implementación'],['1. Esquema SQLDelight','SQLDelight 2.1.0, PharmaDb, tabla producto y marca pendiente.'],['2. Drivers por plataforma','AndroidSqliteDriver y NativeSqliteDriver registrados en Koin. JDBC solo para pruebas host.'],['3. Capa local','ProductoLocal observa consultas, mapea a dominio y usa transacciones.'],['4. Repositorio offline','Alta local con ID negativo; POST de pendientes antes de GET. Confirmación con ID real.'],['5. ViewModel reactivo','Una suscripción al Flow. Guardar, editar y eliminar no recargan el listado.'],['6. Ciclo de comprobación','Sincronización inicial, cierre, modo avión, reapertura, alta y reconexión.'] ],y,[126,CW-126])
y=p('<b>Flujo de datos:</b> pantalla &gt; ViewModel &gt; Flow &gt; SQLDelight.<br/><b>Sincronización:</b> pendientes locales &gt; POST &gt; confirmación del ID &gt; GET de todas las páginas &gt; transacción local.',M,y)
y=p('<b>Resultado Android:</b> se ejecutaron las tres fases instrumentadas. El producto offline llegó al servidor con ID '+str(PRODUCT['id'])+'. La evidencia de Swagger se presenta más adelante.',M,y)
y=p('<b>Pruebas:</b> 73 pruebas host aprobadas, incluidas 8 nuevas de offline-first; 0 fallos y 0 errores. APK e instrumentación compilados.',M,y)
c.showPage()
pair('Android: abrir sin conexión','Capturas reales del emulador; persistencia tras cerrar el proceso.', ['01-listado-con-red.png','02-reapertura-modo-avion.png'],['1. Listado sincronizado con red.','2. Reapertura en modo avión: datos guardados y aviso de desconexión.'])
pair('Android: registrar y reconectar','El inventario se actualiza a través del Flow de la base local.', ['03-producto-pendiente.png','04-reconexion-confirmada.png'],['3. Alta offline visible y marcada como pendiente.','4. Tras reconectar, el alta tiene ID del servidor y deja de estar pendiente.'])
y=start('Evidencia del servidor','Consulta real en Swagger: GET /api/v1/productos/{id}')
y=p('<b>HTTP 200.</b> ID: '+str(PRODUCT['id'])+'; producto: '+escape(PRODUCT['nombre'])+'; precio: S/ '+format(PRODUCT['precio'],'.2f')+'; stock: '+str(PRODUCT['stock'])+'. El token se oculta en la captura.',M,y)
image(E/'swagger/05-producto-id-positivo.png',M,y,CW,590)
c.showPage()
y=start('Pruebas y criterios verificables','commonTest y evidencias originales de ejecución')
y=table([['Caso nuevo','Resultado esperado y obtenido'],['Con red','El Flow muestra la lista descargada y persistida.'],['Sin red','El alta se conserva con ID negativo y pendiente.'],['Reconexión','POST antes de GET; repetir no duplica lo confirmado.'],['Reemplazo local','Se preservan las altas pendientes.'],['GET fallido','Las altas ya confirmadas no se vuelven a enviar.'],['Conflicto','Se conserva el pendiente y se detiene la descarga.'],['IDs negativos','Dos altas locales no colisionan entre sí ni con el servidor.'],['Backend apagado','El listado guardado sigue disponible.']],y,[120,CW-120])
y=p('<b>Comando:</b> ./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:assembleDebugAndroidTest<br/><b>Resultado:</b> BUILD SUCCESSFUL; 73 pruebas, 0 fallos, 0 errores.',M,y)
y=p('<b>Comprobaciones de la guía:</b> esquema generado; drivers registrados; capa local sin Ktor/DTO; contrato con Flow y sincronizar; ViewModel sin recarga manual; listado offline; pendiente local; subida antes de bajada; producto confirmado en servidor; pruebas comunes aprobadas.',M,y)
y=p('<b>Correcciones necesarias:</b> se confirma cada POST con el ID real para retirar el pendiente; se recorren todas las páginas; se envía estado=true en JSON; se restaura la sesión segura validada al no tener red. JDBC se aísla del código común para conservar compatibilidad iOS.',M,y)
y=p('<b>Archivos originales:</b> evidencias/sesion11/android, swagger y pruebas. Los XML y la salida Gradle permiten verificar los resultados sin depender de este informe.',M,y)
c.showPage()
y=start('Validación iOS y referencias','Estado de la segunda plataforma y trazabilidad de la entrega')
ios=E/'ios'
shots=list(ios.rglob('*.png')) if ios.exists() else []
statusfile=ios/'resultado.json'
status=json.loads(statusfile.read_text(encoding='utf-8')) if statusfile.exists() else {}
if status.get('conclusion')=='success' and shots:
    y=p('La ejecución en macOS aprobó la prueba de reapertura con PharmaSoft apagado. Las imágenes siguientes provienen de XCTest y del simulador iPhone.',M,y)
    width=(CW-18)/2
    for i,path in enumerate(shots[:2]): image(path,M+i*(width+18),y,width,465)
else:
    y=p('<b>Evidencia iOS pendiente de completar.</b> El código incluye el driver nativo, su registro y la fábrica de pruebas. GitHub Actions ejecuta commonTest, compila el framework y prepara una prueba XCTest de reapertura con el backend apagado. No se acredita una captura o ejecución exitosa hasta contar con su resultado.',M,y)
    y=p('<b>Seguimiento:</b> <link href="https://github.com/DJuanParedes/pharmaMobil/actions" color="#007D69">GitHub Actions de PharmaMobil</link>. El backend aislado de CI usa H2 en modo Oracle y credenciales efímeras; Android se verificó con MySQL local.',M,y)
    y=p('<b>Alcance:</b> las altas se guardan offline y se envían al reconectar. Las modificaciones y bajas remotas requieren conexión. La garantía ante un POST confirmado cuya respuesta se pierde requeriría idempotencia en el backend.',M,y)
y=p('<b>Material de clase utilizado</b><br/>Guía práctica N.º 11: Persistencia local con SQLDelight y estrategia offline-first, 20/10/2026.<br/>Diapositivas de la sesión 11: Persistencia local y offline-first, Mg. Reyna Barreto Benjamin David.',M,180 if shots and status.get('conclusion')=='success' else y)
y=p('El repositorio contiene README actualizado, código, ocho pruebas comunes, automatización de evidencias y registros originales. La comprobación iOS se declara por separado para mantener explícito qué se ejecutó.',M,y)
c.save();print(OUT)
