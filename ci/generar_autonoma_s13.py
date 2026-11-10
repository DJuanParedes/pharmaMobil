"""Actividad autónoma 13: PDF carta con evidencias reales de evidencias/sesion13-autonoma y evidencias/sesion13."""
from pathlib import Path
import json, re, sys
from xml.sax.saxutils import escape
from reportlab.lib.pagesizes import letter
from reportlab.lib.units import cm
from reportlab.lib.colors import HexColor
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.enums import TA_CENTER
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer, Table, TableStyle,
                                Image, PageBreak, NextPageTemplate, KeepTogether)

ROOT = Path(__file__).resolve().parents[1]
A = ROOT / 'evidencias/sesion13-autonoma'
S = ROOT / 'evidencias/sesion13'
DATOS = json.loads((A / 'datos-portada.json').read_text(encoding='utf-8'))
OUT = ROOT / 'output/pdf/Paredes_Juan_Sesion13_Evidencias.pdf'
OUT.parent.mkdir(parents=True, exist_ok=True)

F = '/usr/share/fonts/truetype/liberation/LiberationSerif-'
for nombre, archivo in [('Times', 'Regular'), ('Times-B', 'Bold'), ('Times-I', 'Italic'), ('Times-BI', 'BoldItalic')]:
    pdfmetrics.registerFont(TTFont(nombre, F + archivo + '.ttf'))
pdfmetrics.registerFontFamily('Times', normal='Times', bold='Times-B', italic='Times-I', boldItalic='Times-BI')

W, H = letter
M = 2.2 * cm
CW = W - 2 * M
GRIS = HexColor('#666666'); LINEA = HexColor('#BBBBBB'); CAB = HexColor('#EEEEEE'); FONDO = HexColor('#F5F7F6')
body = ParagraphStyle('body', fontName='Times', fontSize=11, leading=15.5, spaceAfter=8)
small = ParagraphStyle('small', fontName='Times', fontSize=9.3, leading=12)
cap = ParagraphStyle('cap', fontName='Times', fontSize=9.6, leading=12.4)
h1 = ParagraphStyle('h1', fontName='Times-B', fontSize=16.5, leading=20, spaceAfter=3)
sub = ParagraphStyle('sub', fontName='Times', fontSize=9.5, leading=12, spaceAfter=12, textColor=GRIS)
centro = lambda size, bold=False, after=6: ParagraphStyle('c%s%s' % (size, bold), fontName='Times-B' if bold else 'Times',
                                                         fontSize=size, leading=size * 1.3, alignment=TA_CENTER, spaceAfter=after)
def P(t, s=body): return Paragraph(t, s)

def tabla(filas, anchos, cabecera=True):
    celdas = [[c if not isinstance(c, str) else P(c if c.startswith('<') else escape(c), small) for c in f] for f in filas]
    t = Table(celdas, colWidths=anchos, hAlign='LEFT', repeatRows=1 if cabecera else 0)
    estilo = [('VALIGN', (0, 0), (-1, -1), 'TOP'), ('LINEBELOW', (0, 0), (-1, -1), 0.4, LINEA),
              ('LEFTPADDING', (0, 0), (-1, -1), 6), ('RIGHTPADDING', (0, 0), (-1, -1), 6),
              ('TOPPADDING', (0, 0), (-1, -1), 5), ('BOTTOMPADDING', (0, 0), (-1, -1), 5)]
    if cabecera: estilo.append(('BACKGROUND', (0, 0), (-1, 0), CAB))
    t.setStyle(TableStyle(estilo)); return t

def imagen(path, maxw, maxh):
    img = Image(str(path)); k = min(maxw / img.imageWidth, maxh / img.imageHeight)
    img.drawWidth, img.drawHeight = img.imageWidth * k, img.imageHeight * k
    return img

def recortar(path, destino):
    """Recorta la barra de estado y la barra de gestos: queda solo el contenido de la app."""
    from PIL import Image as PI
    im = PI.open(path); w, h = im.size
    im.crop((0, int(h * 0.045), w, int(h * 0.975))).save(destino)
    return destino

TMP = ROOT / 'build-informe'; TMP.mkdir(exist_ok=True)
def captura(nombre, carpeta=A):
    return recortar(carpeta / nombre, TMP / ('r-' + nombre))

def seccion(titulo, subtitulo): return [P(titulo, h1), P(subtitulo, sub)]

def cabecera(c, doc):
    c.saveState(); c.setFont('Times', 8.5); c.setFillColor(GRIS)
    c.drawString(M, H - 1.4 * cm, 'Actividad Autónoma N.º 13 · Sesión 13')
    c.drawRightString(W - M, H - 1.4 * cm, 'Desarrollo de Aplicaciones Móviles · UPeU')
    c.setStrokeColor(LINEA); c.setLineWidth(0.5); c.line(M, H - 1.6 * cm, W - M, H - 1.6 * cm)
    c.drawString(M, 1.2 * cm, 'Juan Paredes Cárdenas · PharmaMobil · feature/integrador-13')
    c.drawRightString(W - M, 1.2 * cm, 'Página %d' % doc.page)
    c.restoreState()

# ---------- datos ----------
estado = {}
for linea in (A / 'estado.txt').read_text(encoding='utf-8').splitlines():
    if ' | ' in linea:
        k, v = linea.split(' | ', 1); estado[k.strip()] = v.strip()
version = (A / 'android-version.txt').read_text().split()
ios = (A / 'ios-resumen.json')
ios = json.loads(ios.read_text(encoding='utf-8')) if ios.exists() else {}
commit = DATOS['commit']

doc = BaseDocTemplate(str(OUT), pagesize=letter, leftMargin=M, rightMargin=M, topMargin=2.2 * cm, bottomMargin=2 * cm,
                      title='Actividad Autónoma 13 - Evidencias - Juan Paredes', author='Juan Paredes Cárdenas')
fr = Frame(M, 2 * cm, CW, H - 4.2 * cm, leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
doc.addPageTemplates([PageTemplate('portada', [fr]), PageTemplate('normal', [fr], onPage=cabecera)])
s = []

# ---------- portada ----------
s += [P('UNIVERSIDAD PERUANA UNIÓN', centro(20, True, 10)), P('Facultad de Ingeniería y Arquitectura', centro(12.5, after=2)),
      P('Escuela Profesional de Ingeniería de Sistemas', centro(12.5, after=26)), imagen(ROOT / 'documentacion/logo_upeu.png', 9 * cm, 3 * cm),
      Spacer(1, 1.4 * cm), P('ACTIVIDAD AUTÓNOMA N.º 13', centro(12.5, True, 12)),
      P('Flujo completo y evidencias por plataforma', centro(19, True, 8)),
      P('PharmaMobil (Kotlin Multiplatform) sobre PharmaSoft', centro(12, after=28))]
ficha = [['Estudiante', 'Juan Paredes Cárdenas'], ['Código', DATOS['codigo']], ['Grupo', DATOS['grupo']],
         ['Asignatura', 'Desarrollo de Aplicaciones Móviles'], ['Docente', 'Mg. Benjamin David Reyna Barreto'],
         ['Unidad y sesión', 'Unidad 3 · Sesión 13'], ['Ciclo y semestre', 'VI · 2026-2'],
         ['Fecha de ejecución', DATOS['fecha']], ['Entrega', 'Lunes 9 de noviembre de 2026, 23:59']]
t = tabla(ficha, [4.2 * cm, 9 * cm], cabecera=False); t.hAlign = 'CENTER'
s += [t, NextPageTemplate('normal'), PageBreak()]

# ---------- índice ----------
s += seccion('Índice', 'Cuatro productos reunidos en un documento; el repositorio se enlaza en la última página')
indice = [('1. Alcance y entorno de ejecución', 3), ('2. Producto A · Mapa del flujo completo (pasos 1 y 2)', 4),
          ('    Pasos 3 y 4', 5), ('    Pasos 5 y 6', 6), ('    Pasos 7 y 8', 7), ('    Pasos 9 y 10', 8),
          ('3. Producto B · Evidencias por plataforma', 9), ('    Android frente a iOS', 10),
          ('4. Producto C · Bitácora de permisos', 11), ('5. Producto D · Ficha de integración', 12),
          ('6. Lista de cotejo y repositorio', 13)]
s.append(tabla([['Contenido', 'Página']] + [[a, str(b)] for a, b in indice], [CW - 60, 60]))
s.append(Spacer(1, 12))
s.append(P('<b>Cómo leer las evidencias.</b> Cada captura está recortada a la pantalla de la aplicación y lleva un pie que indica qué muestra y en qué momento del recorrido. La línea «Estado del sistema» la escribió la propia prueba instrumentada al terminar cada paso, leyendo la base SQLDelight, el almacenamiento seguro y las respuestas de PharmaSoft; no se redactó a mano.'))
s.append(PageBreak())

# ---------- 1 alcance ----------
s += seccion('1. Alcance y entorno de ejecución', 'Qué se ejecutó, dónde y con qué datos')
s.append(P('El recorrido se ejecutó de principio a fin en Android con la prueba instrumentada <i>AutonomaSesion13Test</i>, que toca la interfaz con UI Automator igual que lo haría un usuario y registra el estado del sistema después de cada paso. PharmaSoft se compiló desde <i>backend/pharmaSoft</i> del mismo repositorio y se ejecutó con su configuración JWT y migraciones Flyway sobre H2; los usuarios admin y vendedor son los de la migración V3, con una contraseña generada para la ejecución que se reemplaza por [REDACTADO] en los registros.'))
s.append(tabla([['Elemento', 'Detalle'],
                ['Dispositivo', 'Emulador Pixel 6, Android %s (API %s), Google APIs x86_64' % (version[0], version[1])],
                ['Servidor', 'PharmaSoft (Spring Boot, JWT HS512, Flyway) en 10.0.2.2:8080; H2 en modo Oracle'],
                ['Ejecución', 'GitHub Actions, run %s, %s' % (DATOS['run_android'], DATOS['fecha'])],
                ['Código evaluado', 'Rama feature/integrador-13, commit %s' % commit],
                ['Datos iniciales', 'Categoría «S13 Medicamentos»; productos «S13 Paracetamol 500» y «S13 Amoxicilina 500»'],
                ['iOS', ios.get('resumen', 'Ver sección 3')]], [3.6 * cm, CW - 3.6 * cm]))
s.append(Spacer(1, 10))
s.append(P('<b>Por qué un emulador en CI y no capturas del monitor.</b> La ficha pide capturas recortadas a la pantalla del dispositivo. UI Automator guarda exactamente esa pantalla, con hora del sistema y barra de estado reales, y el mismo recorrido puede repetirse para la sustentación sin depender de tomar las capturas a mano.'))
s.append(PageBreak())

# ---------- producto A ----------
pasos = [
    ('1', 'Primera apertura, sin sesión', '01-primera-apertura.png', 'Instalación limpia: la app abre en Iniciar sesión.'),
    ('2', 'Inicio de sesión correcto', '02-sesion-iniciada.png', 'Sesión iniciada como admin; Inicio con sus accesos.'),
    ('3', 'Listado tras la primera sincronización', '03-listado-sincronizado.png', 'Inventario descargado: «En línea · todo enviado».'),
    ('4', 'Creación con conexión', '04-alta-con-conexion.png', 'Alta «AA S13 Clotrimazol 1%» confirmada por el servidor.'),
    ('5', 'Creación en modo avión', '05-alta-sin-red.png', 'Modo avión: el alta queda pendiente con ID provisional.'),
    ('6', 'Sincronización al recuperar la señal', '06-sincronizado-al-volver.png', 'Con señal: el pendiente se envía y recibe su ID.'),
    ('7', 'Error de validación provocado', '07-error-validacion.png', 'Nombre «Ab»: el mensaje aparece bajo el campo Nombre.'),
    ('8', 'Operación sin privilegio', '08-vendedor-sin-privilegio.png', 'Vendedor: inventario solo lectura, sin Editar ni Eliminar.'),
    ('9', 'Token vencido', '09-token-vencido.png', 'JWT vencido: la app vuelve a Iniciar sesión con aviso.'),
    ('10', 'Cierre de sesión', '10-sesion-cerrada.png', 'Tras «Cerrar sesión»: pantalla de Iniciar sesión.'),
]
for i in range(0, 10, 2):
    titulo = '2. Producto A · Mapa del flujo completo' if i == 0 else 'Producto A · Pasos %s y %s' % (pasos[i][0], pasos[i + 1][0])
    s += seccion(titulo, 'Recorrido de un usuario nuevo, en orden; capturas del emulador recortadas a la aplicación')
    ancho = (CW - 16) / 2
    fila_t, fila_i, fila_c, fila_e = [], [], [], []
    for n, nombre, archivo, pie in pasos[i:i + 2]:
        fila_t.append(P('<b>Paso %s · %s</b>' % (n, nombre), ParagraphStyle('pt', parent=cap, fontSize=10.5, leading=13)))
        fila_i.append(imagen(captura(archivo), ancho, 395))
        fila_c.append(P('<i>Captura %s.</i> %s' % (n, pie), cap))
        fila_e.append(P('<b>Estado del sistema:</b> ' + escape(estado.get(n, 'sin registro')), small))
    t = Table([fila_t, fila_i, fila_c, fila_e], colWidths=[ancho + 8, ancho + 8], hAlign='LEFT')
    t.setStyle(TableStyle([('VALIGN', (0, 0), (-1, -1), 'TOP'), ('ALIGN', (0, 1), (-1, 1), 'CENTER'),
                           ('LEFTPADDING', (0, 0), (-1, -1), 0), ('RIGHTPADDING', (0, 0), (-1, -1), 8),
                           ('BOTTOMPADDING', (0, 0), (-1, -1), 6), ('BACKGROUND', (0, 3), (-1, 3), FONDO)]))
    s.append(t)
    if i == 6 and (A / 'error-validacion-http.txt').exists():
        pass
    s.append(PageBreak())

# ---------- producto B ----------
s += seccion('3. Producto B · Evidencias por plataforma', 'Android documentado de principio a fin; estado real del objetivo iOS')
s.append(P('<b>Android.</b> Los diez pasos del Producto A se ejecutaron en Android %s. Además se comprobó la reinstalación: se desinstaló la app y se volvió a instalar, y la prueba <i>p11</i> verificó el estado inicial.' % version[0]))
ancho = (CW - 16) / 2
t = Table([[imagen(captura('11-reinstalacion.png'), ancho * 0.8, 300),
            [P('<b>Reinstalación.</b> ' + escape(estado.get('11', 'sin registro')), small), Spacer(1, 8),
             P('<b>Sin red.</b> Con el modo avión activado el inventario sigue visible desde SQLDelight, la barra dice «Sin conexión · N cambios por enviar» y las altas reciben ID negativo (capturas 5 y 6). Al volver la señal, «Sincronizar ahora» sube primero los pendientes y luego descarga.', small), Spacer(1, 8),
             P('<b>Notificaciones.</b> Exigen POST_NOTIFICATIONS en Android 13 o superior. Si se concede, el caso de uso avisa cuando una fila queda fallida; si se deniega, el aviso no se publica y la fila sigue visible en la barra de estado (Producto C).', small)]]],
          colWidths=[ancho * 0.8 + 10, CW - ancho * 0.8 - 10], hAlign='LEFT')
t.setStyle(TableStyle([('VALIGN', (0, 0), (-1, -1), 'TOP'), ('LEFTPADDING', (0, 0), (-1, -1), 0)]))
s.append(t)
s.append(Spacer(1, 6))
s.append(P('<i>Captura 11.</i> Tras desinstalar y reinstalar: la app vuelve a pedir inicio de sesión.', cap))
s.append(Spacer(1, 10))
s.append(P('<b>iOS.</b> ' + ios.get('detalle', 'Sin ejecución registrada.')))
s.append(PageBreak())

s += seccion('Producto B · Android frente a iOS', 'Comparación aspecto por aspecto; lo de iOS se indica desde el código cuando no hubo ejecución en simulador')
s.append(tabla([['Aspecto', 'Android (ejecutado)', 'iOS (%s)' % ios.get('etiqueta', 'estado')],
                ['Almacenamiento del token', 'EncryptedSharedPreferences «sesion_segura», AES-256-GCM con llave del Android Keystore. allowBackup=false: al reinstalar el token desaparece (captura 11).', 'Keychain, kSecClassGenericPassword, servicio pe.edu.upeu.pharmamobil.sesion, accesible AfterFirstUnlockThisDeviceOnly. El Keychain puede sobrevivir a la desinstalación: diferencia a cerrar en la sesión 14 borrándolo en la primera apertura.'],
                ['Permisos', 'Diálogo del sistema «Allow PharmaMobil to send you notifications?». Se muestra como máximo dos veces; después solo Ajustes > Apps > PharmaMobil > Notificaciones.', 'Info.plist declara cámara y ubicación. El diálogo de notificaciones de iOS se muestra una sola vez; GestorPermisosIos aún es un stub que responde Denegado.'],
                ['Sin red', 'Inventario desde SQLDelight; altas con ID negativo; aviso «Sin conexión».', 'Mismo código común y NativeSqliteDriver; commonTest en el simulador: %s.' % ios.get('pruebas', 'sin dato')],
                ['Notificaciones', 'Llegan con permiso concedido (captura de la notificación en Producto C).', 'No implementadas: NotificadorNulo.'],
                ['Diferencias visuales', 'Material 3 con Roboto, barra de estado y barra de gestos de Android; drawer con «Abrir menú».', 'Compose Multiplatform con la tipografía del sistema (San Francisco) y áreas seguras de la isla dinámica; no se capturó la interfaz en simulador.']],
               [3.2 * cm, (CW - 3.2 * cm) / 2, (CW - 3.2 * cm) / 2]))
s.append(Spacer(1, 8))
s.append(P('<b>Plan para iOS (sesión 14, 10/11/2026):</b> implementar GestorPermisosIos con UNUserNotificationCenter, borrar el Keychain en la primera apertura tras reinstalar y repetir los diez pasos en el simulador iPhone.'))
s.append(PageBreak())

# ---------- producto C ----------
s += seccion('4. Producto C · Bitácora de permisos', 'POST_NOTIFICATIONS (Android 13+): el único permiso en tiempo de ejecución que usa la app en Android')
s.append(tabla([['Estado', 'Qué ve el usuario', 'Cómo recupera la función'],
                ['Sin pedir (Denegado)', 'Tarjeta «Avisos de sincronización» que explica para qué se usa, con el botón «Permitir notificaciones».', 'Pulsar el botón: aparece el diálogo del sistema.'],
                ['Denegado', 'Tras «Don\'t allow» la tarjeta sigue explicando y ofrece pedirlo otra vez; el sistema aún muestra el diálogo.', 'Pulsar de nuevo «Permitir notificaciones» y elegir «Allow».'],
                ['Denegado para siempre', 'Tras la segunda negativa: «Las notificaciones están bloqueadas…» y solo el botón «Abrir Ajustes».', 'Abrir Ajustes > Apps > PharmaMobil > Notificaciones y activarlas; al volver (ON_RESUME) la tarjeta se actualiza.'],
                ['Concedido', '«Notificaciones activadas»; los productos que no se pudieron enviar generan una notificación.', 'No requiere acción.']],
               [3.3 * cm, (CW - 3.3 * cm) / 2, (CW - 3.3 * cm) / 2]))
s.append(Spacer(1, 8))
nombres = [('01-permiso-explicacion.png', 'a) Sin pedir'), ('02-dialogo-del-sistema.png', 'b) Diálogo; se responde «Don\'t allow»'),
           ('03-permiso-denegado-para-siempre.png', 'c) Denegado para siempre'), ('04-permiso-concedido.png', 'd) Concedido'),
           ('08-notificacion-fila-fallida.png', 'e) Aviso con permiso concedido')]
ancho = (CW - 4 * 6) / 5
s.append(Table([[imagen(captura(n, S / 'android'), ancho, 220) for n, _ in nombres], [P(c, small) for _, c in nombres]],
               colWidths=[ancho + 6] * 5, hAlign='LEFT', style=[('LEFTPADDING', (0, 0), (-1, -1), 0), ('VALIGN', (0, 0), (-1, -1), 'TOP')]))
s.append(Spacer(1, 6))
s.append(P('Capturas del mismo build tomadas por <i>IntegradorSesion13Test</i> (instalación limpia, permiso revocado). Cámara y ubicación solo están declaradas en el Info.plist de iOS y no se usan en Android, por eso no tienen tabla.', small))
s.append(PageBreak())

# ---------- producto D ----------
s += seccion('5. Producto D · Ficha de integración', 'Huecos del recorrido del paso 1 de la guía, con decisión y fecha; lista de trabajo de la sesión 14')
hoy, s14 = '03/11/2026', '10/11/2026'
s.append(tabla([['N.º', 'Hueco detectado', 'Gravedad', 'Decisión', 'Cierre'],
                ['1', 'Una fila rechazada (409, 400) se reenviaba en cada sincronización y bloqueaba la cola.', 'Alta', 'Cerrado hoy: intentos, ultimoError y fallo permanente.', hoy],
                ['2', 'Un 500/503 se trataba igual que un 409.', 'Alta', 'Cerrado hoy: ResultadoEnvio.', hoy],
                ['3', 'Reintentos inmediatos y sin límite con el servidor caído.', 'Media', 'Cerrado hoy: 5 intentos, espera de 1 a 16 s.', hoy],
                ['4', 'La pantalla no decía si hay red, qué falló ni la antigüedad de los datos.', 'Alta', 'Cerrado hoy: barra de estado.', hoy],
                ['5', 'Una fila fallida en segundo plano no se avisaba.', 'Media', 'Cerrado hoy: notificación con permiso.', hoy],
                ['6', 'Errores de producción invisibles.', 'Media', 'Cerrado hoy: Crashlytics detrás de Telemetria.', hoy],
                ['7', 'Al cerrar sesión la base local queda con los datos del usuario anterior (paso 10).', 'Alta', 'Sesión 14: limpiar PharmaDb al cerrar sesión.', s14],
                ['8', 'Con el token vencido se vuelve al login y se pierde lo escrito (paso 9).', 'Media', 'Sesión 14: guardar borrador antes de redirigir.', s14],
                ['9', 'Reintentar un alta puede duplicarla si se perdió la respuesta del POST.', 'Alta', 'Sesión 14: Idempotency-Key con el backend.', s14],
                ['10', 'Keychain de iOS puede conservar el token tras reinstalar (Producto B).', 'Media', 'Sesión 14: borrar en la primera apertura.', s14],
                ['11', 'Permisos y notificaciones en iOS son stubs.', 'Media', 'Sesión 14: UNUserNotificationCenter.', s14],
                ['12', 'Validación 400 solo se ve del servidor si el cliente se salta su regla (paso 7).', 'Baja', 'Descartado: el cliente aplica las mismas reglas que el DTO; duplicarlas en la interfaz no aporta.', hoy],
                ['13', 'Permiso de cámara para escanear códigos de barras.', 'Baja', 'Descartado: no hay pantalla de escaneo en el alcance del curso.', hoy]],
               [0.9 * cm, 6.0 * cm, 1.9 * cm, CW - 0.9 * cm - 6.0 * cm - 1.9 * cm - 2 * cm, 2 * cm]))
s.append(PageBreak())

# ---------- cotejo ----------
s += seccion('6. Lista de cotejo y repositorio', 'Revisión antes de subir el documento')
s.append(tabla([['N.º', 'Criterio observable', 'Cumple'],
                ['1', 'Portada con nombre, código y grupo.', 'Sí'],
                ['2', 'Mapa del flujo con los diez pasos, cada uno con captura y estado del sistema.', 'Sí'],
                ['3', 'Capturas recortadas y con pie que explica qué muestran.', 'Sí'],
                ['4', 'Recorrido documentado en Android de principio a fin.', 'Sí'],
                ['5', 'iOS documentado o explicado el motivo.', 'Sí (sección 3)'],
                ['6', 'Bitácora con los tres estados de cada permiso usado.', 'Sí'],
                ['7', 'Camino a Ajustes en el estado denegado definitivo.', 'Sí'],
                ['8', 'Ficha con gravedad, decisión y fecha.', 'Sí'],
                ['9', 'Cada descarte justificado en una línea.', 'Sí'],
                ['10', 'Enlace a la rama y al commit evaluado.', 'Sí'],
                ['11', 'Nombre pedido y menos de 20 MB.', 'Sí']], [1 * cm, CW - 4 * cm, 3 * cm]))
s.append(Spacer(1, 12))
s.append(P('<b>Repositorio:</b> <link href="https://github.com/DJuanParedes/pharmaMobil/tree/feature/integrador-13" color="#0B7A5C">github.com/DJuanParedes/pharmaMobil/tree/feature/integrador-13</link>'))
s.append(P('<b>Commit evaluado:</b> <link href="https://github.com/DJuanParedes/pharmaMobil/commit/%s" color="#0B7A5C">%s</link>' % (commit, commit)))
s.append(P('<b>Prueba del recorrido:</b> androidApp/src/androidTest/kotlin/pe/edu/upeu/pharmamobil/AutonomaSesion13Test.kt<br/><b>Evidencias originales:</b> evidencias/sesion13-autonoma (capturas sin recortar, estado.txt y salida de la instrumentación).'))
s.append(Spacer(1, 8))
s.append(P('<b>Referencias</b>', body))
for r in ['Google. (2025). <i>Request runtime permissions</i>. https://developer.android.com/training/permissions/requesting',
          'Google. (2025). <i>Build an offline-first app</i>. https://developer.android.com/topic/architecture/data-layer/offline-first',
          'Firebase. (2026). <i>Add Firebase to your Android project</i>. https://firebase.google.com/docs/android/setup',
          'Reyna Barreto, B. D. (2026). <i>Actividad Autónoma N.º 13: Documentar el flujo completo y las evidencias por plataforma</i>. Universidad Peruana Unión.']:
    s.append(P(r, small))

doc.build(s)
print(OUT, round(OUT.stat().st_size / 1e6, 2), 'MB')
