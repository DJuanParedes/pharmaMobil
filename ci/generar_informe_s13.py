"""Informe UPeU de la sesión 13. Usa solo evidencias reales de evidencias/sesion13."""
from pathlib import Path
import json, re, xml.etree.ElementTree as ET
from xml.sax.saxutils import escape
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import cm
from reportlab.lib.colors import HexColor, black
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.enums import TA_CENTER
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer, Table, TableStyle,
                                Image, PageBreak, KeepTogether, Preformatted, NextPageTemplate)

ROOT = Path(__file__).resolve().parents[1]
E = ROOT / 'evidencias/sesion13'
OUT = ROOT / 'output/pdf/S13_Practica_Integrador_Juan_Paredes.pdf'
OUT.parent.mkdir(parents=True, exist_ok=True)

F = '/usr/share/fonts/truetype/liberation/LiberationSerif-'
for nombre, archivo in [('Times', 'Regular'), ('Times-B', 'Bold'), ('Times-I', 'Italic'), ('Times-BI', 'BoldItalic')]:
    pdfmetrics.registerFont(TTFont(nombre, F + archivo + '.ttf'))
pdfmetrics.registerFontFamily('Times', normal='Times', bold='Times-B', italic='Times-I', boldItalic='Times-BI')

W, H = A4
M = 2.54 * cm
CW = W - 2 * M
GRIS = HexColor('#666666'); LINEA = HexColor('#BBBBBB'); CAB = HexColor('#EEEEEE'); VERDE = HexColor('#0B7A5C')
body = ParagraphStyle('body', fontName='Times', fontSize=11.5, leading=16.5, spaceAfter=9)
small = ParagraphStyle('small', fontName='Times', fontSize=9.5, leading=12.5)
cap = ParagraphStyle('cap', fontName='Times', fontSize=10.5, leading=13.5)
h1 = ParagraphStyle('h1', fontName='Times-B', fontSize=17, leading=21, spaceAfter=4)
sub = ParagraphStyle('sub', fontName='Times', fontSize=9.5, leading=12, spaceAfter=16)
h3 = ParagraphStyle('h3', fontName='Times-B', fontSize=11.5, leading=15, spaceBefore=4, spaceAfter=4)
code = ParagraphStyle('code', fontName='Courier', fontSize=7.6, leading=9.4, textColor=HexColor('#333333'))
centro = lambda size, bold=False, after=6: ParagraphStyle('c%s%s' % (size, bold), fontName='Times-B' if bold else 'Times',
                                                         fontSize=size, leading=size * 1.3, alignment=TA_CENTER, spaceAfter=after)

def P(t, s=body): return Paragraph(t, s)

def tabla(filas, anchos):
    celdas = [[P(escape(str(v)), small) for v in f] for f in filas]
    t = Table(celdas, colWidths=anchos, hAlign='LEFT', repeatRows=1)
    t.setStyle(TableStyle([('BACKGROUND', (0, 0), (-1, 0), CAB), ('VALIGN', (0, 0), (-1, -1), 'TOP'),
                           ('LINEBELOW', (0, 0), (-1, -1), 0.4, LINEA), ('LEFTPADDING', (0, 0), (-1, -1), 7),
                           ('RIGHTPADDING', (0, 0), (-1, -1), 7), ('TOPPADDING', (0, 0), (-1, -1), 6),
                           ('BOTTOMPADDING', (0, 0), (-1, -1), 6)]))
    return t

def imagen(path, maxw, maxh):
    img = Image(str(path)); iw, ih = img.imageWidth, img.imageHeight
    k = min(maxw / iw, maxh / ih); img.drawWidth, img.drawHeight = iw * k, ih * k
    return img

def par(a, b, ca, cb, alto=540):
    ancho = (CW - 18) / 2
    t = Table([[P(ca, cap), P(cb, cap)], [imagen(E / 'android' / a, ancho, alto), imagen(E / 'android' / b, ancho, alto)]],
              colWidths=[ancho + 9, ancho + 9], hAlign='LEFT')
    t.setStyle(TableStyle([('VALIGN', (0, 0), (-1, -1), 'TOP'), ('LEFTPADDING', (0, 0), (-1, -1), 0),
                           ('RIGHTPADDING', (0, 0), (-1, -1), 9), ('BOTTOMPADDING', (0, 0), (-1, 0), 10)]))
    return t

def seccion(titulo, subtitulo):
    return [P(titulo, h1), P(subtitulo, sub)]

def cabecera(c, doc):
    c.saveState()
    c.setFont('Times', 9); c.setFillColor(GRIS)
    c.drawString(M, H - 1.6 * cm, 'UNIVERSIDAD PERUANA UNIÓN'); c.drawRightString(W - M, H - 1.6 * cm, 'Ingeniería de Sistemas')
    c.setStrokeColor(LINEA); c.setLineWidth(0.6); c.line(M, H - 1.8 * cm, W - M, H - 1.8 * cm)
    c.drawString(M, 1.5 * cm, 'Juan Paredes | Desarrollo de Aplicaciones Móviles'); c.drawRightString(W - M, 1.5 * cm, str(doc.page - 1))
    c.restoreState()

# ---------------- datos reales ----------------
pruebas = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
por_clase = {}
for xml in sorted((E / 'pruebas').glob('TEST-*.xml')):
    r = ET.parse(xml).getroot()
    for k in pruebas: pruebas[k] += int(r.get(k, 0))
    por_clase[r.get('name').split('.')[-1]] = (int(r.get('tests')), int(r.get('failures')) + int(r.get('errors')), r.get('time'))
nuevas = ['SincronizacionTest', 'SincronizacionTextoTest', 'PermisoNotificacionesTest', 'ProductoRepositorioOfflineTest']
instr = (E / 'android/instrumentacion.txt').read_text(encoding='utf-8', errors='replace')
registro = (E / 'android/registro-prueba.txt').read_text(encoding='utf-8')
verificado = [l.split('VERIFICADO: ', 1)[1].strip() for l in registro.splitlines() if 'VERIFICADO: ' in l]
ok_instr = re.search(r'OK \((\d+) tests?\)', instr)
version = (E / 'android/android-version.txt').read_text().split()
servidor = json.loads((E / 'productos-servidor.json').read_text(encoding='utf-8'))
s13 = [p for p in servidor.get('contenido', []) if p['nombre'].startswith('S13')]
gradle = (E / 'gradle.txt').read_text(encoding='utf-8', errors='replace')
build_ok = re.search(r'BUILD SUCCESSFUL in [^\n]+', gradle)
commits = (E / 'commits.txt').read_text(encoding='utf-8').strip().splitlines()

doc = BaseDocTemplate(str(OUT), pagesize=A4, leftMargin=M, rightMargin=M, topMargin=M, bottomMargin=M,
                      title='Práctica 13 - Proyecto integrador - Juan Paredes', author='Juan Paredes')
frame = Frame(M, M, CW, H - 2 * M, id='f', leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
doc.addPageTemplates([PageTemplate('portada', [frame]), PageTemplate('normal', [frame], onPage=cabecera)])
s = []

# ---------------- portada ----------------
s += [Spacer(1, 0.6 * cm), P('UNIVERSIDAD PERUANA UNIÓN', centro(22, True, 14)), P('FACULTAD DE INGENIERÍA Y ARQUITECTURA', centro(13, after=2)),
      P('Escuela Profesional de Ingeniería de Sistemas', centro(13, after=40)), imagen(ROOT / 'documentacion/logo_upeu.png', 10 * cm, 3.4 * cm),
      Spacer(1, 2.2 * cm), P('INFORME DE PRÁCTICA DE LABORATORIO N.º 13', centro(12.5, True, 22)),
      P('Proyecto integrador: sincronización robusta,<br/>permisos y Firebase', centro(18, True, 14)),
      P('Proyecto: PharmaMobil sobre PharmaSoft', centro(12.5, after=36)),
      P('<b>Asignatura</b>', centro(12.5, after=2)), P('Desarrollo de Aplicaciones Móviles', centro(12.5, after=18)),
      P('<b>Autor</b>', centro(12.5, after=2)), P('Juan Paredes', centro(12.5, after=18)),
      P('<b>Docente</b>', centro(12.5, after=2)), P('Mg. Reyna Barreto Benjamin David', centro(12.5, after=40)),
      P('Ciclo VI · Semestre 2026-2', centro(12.5, after=2)), P('3 de noviembre de 2026', centro(12.5, after=2)), P('Perú', centro(12.5)),
      NextPageTemplate('normal'), PageBreak()]

# ---------------- índice ----------------
indice = [('1. Desarrollo de la práctica', 2), ('2. Paso 1: tabla de huecos', 3), ('3. Pasos 2 y 3: esquema y caso de uso', 4),
          ('4. Evidencias: permiso de notificaciones (1/2)', 5), ('5. Evidencias: permiso de notificaciones (2/2)', 6),
          ('6. Evidencias: estado de sincronización', 7), ('7. Evidencias: rechazo 409 y notificación', 8),
          ('8. Comprobación en PharmaSoft', 9), ('9. Pruebas y resultados', 10), ('10. Salida real de las pruebas', 11),
          ('11. Firebase y desviaciones', 12), ('12. Guion de sustentación', 13), ('13. Lista de cotejo y referencias', 14)]
s += seccion('Presentación e índice', 'Informe académico de la actividad práctica de la sesión 13')
s.append(P('<b>Introducción.</b> Este informe documenta el proyecto integrador de PharmaMobil. Se recorrió el flujo completo, se registraron sus huecos y se cerraron los de mayor severidad: cola de sincronización con intentos y espera creciente, distinción entre fallos transitorios y permanentes, estado visible en la interfaz, permiso de notificaciones en sus tres estados y telemetría con Firebase detrás de una interfaz del dominio.'))
s.append(P('<b>Objetivo.</b> Dejar la aplicación lista para la sustentación final: que ningún producto guardado sin conexión se pierda en silencio, que el usuario sepa qué está pendiente o falló, y que los permisos y errores se manejen con criterio.'))
s.append(tabla([['Contenido', 'Página']] + [[a, b] for a, b in indice], [CW - 70, 70]))
s.append(Spacer(1, 10))
s.append(P('<b>Conclusiones de la ejecución.</b> La suite host aprobó %d pruebas con %d fallos. En el emulador Android %s (API %s) la prueba instrumentada recorrió los tres estados del permiso y el ciclo de sincronización contra PharmaSoft real: dos altas se confirmaron y la tercera fue rechazada con 409, quedó visible como fallida y generó la notificación.' % (pruebas['tests'], pruebas['failures'] + pruebas['errors'], version[0], version[1])))
s.append(PageBreak())

# ---------------- 1 ----------------
s += seccion('1. Desarrollo de la práctica', 'Guía práctica de laboratorio N.º 13 | Unidad 3 | Juan Paredes')
s.append(P('<b>Objetivo:</b> integrar lo construido en las sesiones 8 a 11 y cerrar los huecos que impiden usar la aplicación en condiciones reales de red.'))
s.append(P('<b>Rama:</b> feature/integrador-13<br/><b>Base:</b> feature/offline-first-paredes (sesión 11).<br/><b>Repositorio:</b> <link href="https://github.com/DJuanParedes/pharmaMobil/tree/feature/integrador-13" color="#0B7A5C">DJuanParedes/pharmaMobil</link>'))
s.append(tabla([['Paso solicitado', 'Implementación'],
                ['1. Recorrido y huecos', 'Doce huecos con severidad y decisión: hoy, sesión 14 o descartar.'],
                ['2. Esquema', 'Columnas intentos y ultimoError, tabla sync_meta y migración 1.sqm.'],
                ['3. Caso de uso', 'ResultadoEnvio Exito/Transitorio/Permanente; 5 intentos; espera de 1, 2, 4, 8 y 16 s; subir antes de bajar.'],
                ['4. Estado en la interfaz', 'Conexión, cambios por enviar, antigüedad de los datos, filas fallidas y acciones.'],
                ['5. Permisos', 'GestorPermisos común con tres estados; implementación Android con PuentePermisos; manifiesto e Info.plist.'],
                ['6. Firebase', 'GitLive 2.7.0 (Crashlytics y Messaging) detrás de la interfaz Telemetria.'],
                ['7. Pruebas', 'commonTest con MockEngine y driver en memoria; prueba instrumentada en el emulador.']], [130, CW - 130]))
s.append(Spacer(1, 6))
s.append(P('<b>Flujo de sincronización:</b> filas pendientes &gt; POST por fila &gt; Exito confirma el ID real; Transitorio suma un intento, espera y corta la ronda; Permanente marca la fila fallida y avisa &gt; GET de todas las páginas &gt; conciliación y hora de la última sincronización.'))
s.append(P('<b>Commits por paso:</b> ' + '; '.join(escape(c) for c in commits[:8]) + '.', small))
s.append(PageBreak())

# ---------------- 2 ----------------
s += seccion('2. Paso 1: tabla de huecos', 'Recorrido completo del flujo: login, inventario, alta con y sin red, reconexión y cierre de sesión')
huecos = [['#', 'Hueco encontrado', 'Sev.', 'Decisión'],
          ['1', 'Una fila rechazada por el servidor (409, 400) se reenviaba en cada sincronización y bloqueaba la cola.', 'Alta', 'Hoy: intentos, ultimoError y fallo permanente.'],
          ['2', 'Un 500 o 503 se trataba igual que un 409.', 'Alta', 'Hoy: ResultadoEnvio.'],
          ['3', 'Reintentos inmediatos y sin límite con el servidor caído.', 'Media', 'Hoy: 5 intentos, 1–16 s.'],
          ['4', 'La pantalla solo mostraba «N pendientes».', 'Alta', 'Hoy: barra de estado.'],
          ['5', 'El usuario no se entera de una fila fallida en segundo plano.', 'Media', 'Hoy: notificación con permiso.'],
          ['6', 'Los errores en producción no se ven.', 'Media', 'Hoy: Crashlytics vía Telemetria.'],
          ['7', 'Al cerrar sesión la base local conserva datos del usuario anterior.', 'Alta', 'Sesión 14.'],
          ['8', 'Si el token vence a mitad de un formulario, se pierde lo escrito.', 'Media', 'Sesión 14: guardar borrador.'],
          ['9', 'Reintentar una creación puede duplicarla si se perdió la respuesta del POST.', 'Alta', 'Sesión 14: Idempotency-Key. Hoy mitigado por el 409.'],
          ['10', 'Un 500 en registrar mostraba error aunque el producto ya estaba guardado.', 'Baja', 'Hoy: se trata como transitorio.'],
          ['11', 'Permisos y telemetría en iOS.', 'Media', 'Sesión 14.'],
          ['12', 'Permiso de cámara para escanear códigos.', 'Baja', 'Descartado: fuera de alcance.']]
s.append(tabla(huecos, [26, CW - 26 - 42 - 150, 42, 150]))
s.append(Spacer(1, 8))
s.append(P('Lo que ya funcionaba se dejó igual: el login distingue credenciales inválidas de falta de conexión y la lista vacía tiene su propio texto.'))
s.append(PageBreak())

# ---------------- 3 ----------------
s += seccion('3. Pasos 2 y 3: esquema y caso de uso', 'SQLDelight 2.1.0 y SincronizarUseCase en commonMain')
s.append(P('<b>Esquema (Producto.sq y 1.sqm).</b> La migración agrega las columnas sin borrar datos de las instalaciones existentes:'))
s.append(Preformatted('ALTER TABLE producto ADD COLUMN intentos INTEGER NOT NULL DEFAULT 0;\nALTER TABLE producto ADD COLUMN ultimoError TEXT;\nCREATE TABLE IF NOT EXISTS sync_meta (clave TEXT NOT NULL PRIMARY KEY, valor INTEGER NOT NULL);\n\npendientesPorEnviar:\nSELECT * FROM producto WHERE pendiente = 1 AND intentos < :maxIntentos ORDER BY id;\nmarcarFallida:\nUPDATE producto SET intentos = :maxIntentos, ultimoError = :motivo WHERE id = :id;', code))
s.append(Spacer(1, 8))
s.append(P('<b>Traducción de errores (capa de datos).</b> El dominio no conoce códigos HTTP:'))
s.append(tabla([['ErrorApi', 'ResultadoEnvio', 'Efecto en la fila'],
                ['SinConexion, TiempoAgotado, Servidor (5xx)', 'Transitorio', 'Suma un intento, espera y corta la ronda. Al 5.º intento queda fallida.'],
                ['Validacion (400), Conflicto (409), NoEncontrado, SinPermiso', 'Permanente', 'Fallida al momento, con el mensaje del servidor; se reporta a Telemetria.'],
                ['SesionExpirada (401)', 'Permanente', '«Sesión vencida»; la fila se conserva.']], [170, 85, CW - 255]))
s.append(Spacer(1, 8))
s.append(P('<b>Espera creciente:</b> espera(i) = 1000 ms × 2<super>min(i, 4)</super>, es decir 1, 2, 4, 8 y 16 s. En las pruebas la espera se inyecta como una función que solo registra el valor, sin demorar la suite.'))
s.append(P('<b>Orden:</b> se sube antes de bajar. Invertirlo haría que la conciliación reemplace la lista local y se pierdan las altas pendientes.'))
s.append(PageBreak())

# ---------------- 4-7 capturas ----------------
s += seccion('4. Evidencias: permiso de notificaciones (1/2)', 'Capturas reales del emulador Android %s; instalación limpia y POST_NOTIFICATIONS revocado' % version[0] + '')
s.append(par('01-permiso-explicacion.png', '02-dialogo-del-sistema.png', '1. Nunca pedido: la tarjeta explica para qué se usa antes de pedirlo.', '2. Diálogo del sistema al pulsar «Permitir notificaciones». Se responde «No permitir».'))
s.append(PageBreak())
s += seccion('5. Evidencias: permiso de notificaciones (2/2)', 'Los otros dos estados de EstadoPermiso')
s.append(par('03-permiso-denegado-para-siempre.png', '04-permiso-concedido.png', '3. Tras la segunda negativa: DenegadoParaSiempre. Solo se ofrece «Abrir Ajustes».', '4. Activado en Ajustes: al volver (ON_RESUME) se lee Concedido.'))
s.append(PageBreak())
s += seccion('6. Evidencias: estado de sincronización', 'Barra de estado sobre el inventario; PharmaSoft real en ejecución')
s.append(par('05-sync-en-linea.png', '06-sync-sin-conexion-pendientes.png', '5. Con red: «En línea · todo enviado» y antigüedad de los datos.', '6. Modo avión y tres altas: «Sin conexión · 3 cambios por enviar».'))
s.append(PageBreak())
s += seccion('7. Evidencias: rechazo 409 y notificación', 'Al reconectar, PharmaSoft rechaza un nombre que ya existe')
s.append(par('07-sync-rechazo-409-visible.png', '08-notificacion-fila-fallida.png', '7. Dos altas confirmadas; la tercera queda fallida y visible con el motivo del servidor.', '8. Notificación local generada por el caso de uso (permiso concedido).'))
s.append(PageBreak())

# ---------------- 8 servidor ----------------
s += seccion('8. Comprobación en PharmaSoft', 'GET /api/v1/productos al terminar la prueba instrumentada')
s.append(P('PharmaSoft se ejecutó con su código oficial sobre H2 en modo Oracle, migraciones Flyway y una clave de administrador generada para la ejecución, que no se publica. Antes de la prueba se registraron «S13 Paracetamol 500» y «S13 Amoxicilina 500».'))
s.append(tabla([['ID', 'Nombre', 'Precio', 'Stock', 'Origen']] +
               [[p['id'], p['nombre'], 'S/ %.2f' % p['precio'], p['stock'],
                 'Semilla del servidor' if p['nombre'] in ('S13 Paracetamol 500', 'S13 Amoxicilina 500') else 'Alta offline confirmada'] for p in s13],
               [40, 170, 70, 50, CW - 330]))
s.append(Spacer(1, 8))
s.append(P('<b>Resultado:</b> «S13 Ibuprofeno 400 R1» y «S13 Loratadina 10 R1» se crearon desde el teléfono al reconectar (R1 identifica la ronda de la prueba). «S13 Amoxicilina 500» existe una sola vez: el reintento no la duplicó y el rechazo quedó en la fila local.'))
for v in verificado: s.append(P('<font face="Courier" size="8.5">VERIFICADO: ' + escape(v) + '</font>', small))
s.append(PageBreak())

# ---------------- 9 pruebas ----------------
s += seccion('9. Pruebas y resultados', 'commonTest con MockEngine y SQLDelight en memoria; reporte HTML de Gradle')
s.append(tabla([['Caso nuevo', 'Resultado esperado y obtenido'],
                ['503 transitorio', 'Suma un intento y la fila sigue pendiente; no se marca fallida.'],
                ['409 permanente', 'Fallida con el mensaje del servidor; la siguiente ronda no la reenvía y se avisa una vez.'],
                ['Espera creciente', 'Esperas registradas: 1000, 2000, 4000, 8000 y 16000 ms; nunca más de 16 s.'],
                ['Cinco transitorios', 'La fila queda fallida, visible y no se pierde.'],
                ['Éxito', 'POST antes de GET; se guarda la hora de sincronización.'],
                ['Reintentar fallidas', 'La fila vuelve a la cola y se envía.'],
                ['Permisos', 'Gestor falso: explicar y pedir, ninguna acción, abrir Ajustes.'],
                ['Textos de estado', '«Sin conexión · 3 cambios por enviar», «Actualizado hace 2 h».']], [120, CW - 120]))
s.append(Spacer(1, 8))
s.append(P('Reporte HTML generado por Gradle: resumen de la suite y detalle de SincronizacionTest.', small))
ancho = (CW - 12) / 2
t = Table([[imagen(E / 'pruebas/reporte-gradle.png', ancho, 220), imagen(E / 'pruebas/reporte-sincronizacion.png', ancho, 220)]], colWidths=[ancho + 6, ancho + 6], hAlign='LEFT')
t.setStyle(TableStyle([('LEFTPADDING', (0, 0), (-1, -1), 0), ('BOX', (0, 0), (0, 0), 0.4, LINEA), ('BOX', (1, 0), (1, 0), 0.4, LINEA)]))
s.append(t)
s.append(PageBreak())

# ---------------- 10 salida ----------------
s += seccion('10. Salida real de las pruebas', 'Extractos de los registros originales y resumen leído de los XML de JUnit')
s.append(P('<b>Resumen JUnit:</b> %s. Origen: evidencias/sesion13/pruebas/TEST-*.xml.' % escape(str(pruebas))))
s.append(tabla([['Clase', 'Pruebas', 'Fallos', 'Tiempo (s)']] + [[k, v[0], v[1], v[2]] for k, v in por_clase.items() if k in nuevas], [CW - 210, 70, 70, 70]))
s.append(Spacer(1, 8))
s.append(Preformatted('> Task :shared:testAndroidHostTest\n' + (build_ok.group(0) if build_ok else ''), code))
s.append(Spacer(1, 8))
s.append(P('<b>Instrumentación (IntegradorSesion13Test):</b>', h3))
lineas = [l for l in instr.splitlines() if l.startswith(('Time:', 'OK ', 'INSTRUMENTATION_CODE'))]
s.append(Preformatted('\n'.join(lineas), code))
for l in registro.strip().splitlines(): s.append(P('<font face="Courier" size="7.6">' + escape(l) + '</font>', small))
s.append(Spacer(1, 8))
s.append(P('<b>Entorno:</b> GitHub Actions (ubuntu-latest) con emulador Pixel 6, Android %s (API %s), y PharmaSoft compilado desde backend/pharmaSoft sobre H2. La clave del administrador se genera en cada ejecución y aparece como [REDACTADO] en los registros.' % (version[0], version[1])))
s.append(PageBreak())

# ---------------- 11 firebase ----------------
s += seccion('11. Firebase y desviaciones', 'Paso 6 y decisiones documentadas')
s.append(P('<b>Telemetría.</b> El caso de uso solo conoce la interfaz <i>Telemetria</i> del dominio. En Android la implementa <i>TelemetriaFirebase</i> con <i>Firebase.crashlytics</i> de GitLive 2.7.0: registra la excepción y las claves operacion, tipo y motivo. No se envían tokens, contraseñas ni datos personales. Cada llamada está protegida con runCatching para que la app no falle si Firebase no está configurado.'))
s.append(P('<b>Activación.</b> El plugin google-services se aplica solo si existe androidApp/google-services.json, que está en .gitignore. Para ver los errores en la consola: crear el proyecto en Firebase, registrar pe.edu.upeu.pharmamobil, descargar el archivo a androidApp/ y ejecutar.'))
s.append(tabla([['Desviación', 'Motivo'],
                ['Dependencias de Firebase en androidMain y no en commonMain', 'Evita romper el enlace del framework iOS de la sesión 11 sin GoogleService-Info.plist. La interfaz Telemetria sigue en común.'],
                ['iOS con TelemetriaNula y GestorPermisosIos de relleno', 'Pendiente para la sesión 14 (hueco 11).'],
                ['Clave de idempotencia', 'Requiere cambio acordado con el backend; hoy el 409 evita el duplicado (sección 8).'],
                ['Evidencia de Crashlytics en consola', 'Requiere el google-services.json propio del alumno; no se publica.']], [190, CW - 190]))
s.append(PageBreak())

# ---------------- 12 guion ----------------
s += seccion('12. Guion de sustentación', 'Demostración propuesta | Duración: 8 minutos')
s.append(tabla([['Tiempo', 'Demostración y explicación'],
                ['0:00-0:45', 'Presentar PharmaMobil, la rama y la tabla de huecos con sus decisiones.'],
                ['0:45-2:00', 'Esquema: intentos, ultimoError y migración 1.sqm. Por qué la migración no borra datos.'],
                ['2:00-3:30', 'SincronizarUseCase: Transitorio vs Permanente, 5 intentos, espera 1–16 s, subir antes de bajar.'],
                ['3:30-4:30', 'Permiso de notificaciones: explicar, diálogo, denegado para siempre y Ajustes.'],
                ['4:30-6:00', 'Modo avión: tres altas pendientes. Reconectar: dos confirmadas y una rechazada con 409 visible.'],
                ['6:00-7:00', 'Notificación de fila fallida, «Revisar» y «Reintentar».'],
                ['7:00-8:00', 'Pruebas con MockEngine y driver en memoria; Firebase detrás de Telemetria; límites.']], [70, CW - 70]))
s.append(Spacer(1, 8))
s.append(P('<b>Conclusiones:</b> ningún alta offline se pierde: o se confirma con el ID del servidor o queda fallida y visible con su motivo. El servidor caído ya no consume la batería con reintentos inmediatos. El permiso se pide con contexto y se respeta la negativa. Las pruebas comunes verifican la lógica sin dispositivo ni red.'))
s.append(P('<b>Límites:</b> iOS conserva stubs de permisos y telemetría; la idempotencia y la limpieza de datos al cerrar sesión quedan para la sesión 14.'))
s.append(PageBreak())

# ---------------- 13 cotejo ----------------
s += seccion('13. Lista de cotejo y referencias', 'Trazabilidad de los 14 ítems de la guía')
s.append(tabla([['#', 'Criterio', 'Evidencia / resultado'],
                ['1', 'Rama con un commit por paso', 'feature/integrador-13, sección 1.'],
                ['2', 'Tabla de huecos con severidad y decisión', 'Sección 2.'],
                ['3', 'Columnas intentos y ultimoError', 'Producto.sq, sección 3.'],
                ['4', 'Migración 1.sqm', 'Sección 3.'],
                ['5', 'ResultadoEnvio Exito/Transitorio/Permanente', 'EnvioRepositorioRest, sección 3.'],
                ['6', 'Máximo 5 intentos, espera 1/2/4/8/16 s', 'SincronizacionTest, sección 9.'],
                ['7', 'Subir antes de bajar', 'Prueba de éxito (POST, GET) y sección 8.'],
                ['8', 'Pantalla con pendientes, fallidas, antigüedad y acciones', 'Capturas 5 a 7.'],
                ['9', 'GestorPermisos común con 3 estados', 'PermisoNotificacionesTest.'],
                ['10', 'Implementación Android con PuentePermisos', 'Capturas 1 a 4.'],
                ['11', 'Manifiesto e Info.plist', 'POST_NOTIFICATIONS; cámara y ubicación en iOS.'],
                ['12', 'Firebase detrás de Telemetria', 'Sección 11.'],
                ['13', 'Pruebas commonTest con MockEngine y driver en memoria', '%d pruebas, %d fallos.' % (pruebas['tests'], pruebas['failures'] + pruebas['errors'])],
                ['14', 'Capturas de los 3 estados del permiso y de la sincronización', 'Secciones 4 a 7.']], [26, 206, CW - 232]))
s.append(Spacer(1, 10))
s.append(P('<b>Referencias del material de clase</b>'))
s.append(P('Reyna Barreto, B. D. (2026). <i>Guía práctica de laboratorio N.º 13: Proyecto integrador PharmaMobil KMP</i>. Universidad Peruana Unión.'))
s.append(P('<b>Repositorio de la entrega:</b> github.com/DJuanParedes/pharmaMobil, rama feature/integrador-13.<br/><b>Archivos de verificación:</b> evidencias/sesion13/android, evidencias/sesion13/pruebas y docs/sesion13-integrador.md.'))

doc.build(s)
print(OUT)
