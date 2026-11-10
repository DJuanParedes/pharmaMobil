#!/usr/bin/env python3
"""Sesión 14, pasos 5 y 7 en emulador: recorre la versión de ENTREGA (firmada y minificada) como lo haría
una persona, solo con adb y el árbol de accesibilidad (uiautomator dump). No usa clases de la app: la
versión de entrega no es depurable y R8 renombró su código, así que la instrumentación no aplica.

Uso: python3 ci/sesion14/recorrido_release.py <apk-de-entrega> <carpeta-de-salida>
Necesita el backend de CI (ci/sesion13/backend_s13.py) en 8080 y su control en 8099.
La clave efímera del backend nunca se imprime.
"""
import json
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

APK = sys.argv[1]
OUT = Path(sys.argv[2]); OUT.mkdir(parents=True, exist_ok=True)
PKG = 'pe.upeu.pharmamobil'
API = 'http://127.0.0.1:8080/api/v1/'
CLAVE = json.load(urlopen('http://127.0.0.1:8099/credenciales'))['password']
REGISTRO = OUT / 'recorrido.txt'
REGISTRO.write_text('')
resultados = []


def adb(*args, timeout=90):
    r = subprocess.run(['adb', *args], capture_output=True, text=True, timeout=timeout)
    return (r.stdout + r.stderr).replace(CLAVE, '[REDACTADO]')


def sh(cmd, timeout=90):
    return adb('shell', cmd, timeout=timeout)


def log(texto):
    print(texto, flush=True)
    with REGISTRO.open('a') as f:
        f.write(texto + '\n')


# ---------------------------------------------------------------- API de PharmaSoft (verificación)
def api(path, body=None, token=None, method=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    data = json.dumps(body).encode() if body is not None else None
    try:
        with urlopen(Request(API + path, data, headers, method=method), timeout=15) as r:
            return r.status, json.loads(r.read() or b'null')
    except HTTPError as e:
        cuerpo = e.read().decode()
        return e.code, re.sub(r'"timestamp":"[^"]*",', '', cuerpo)


def token(usuario):
    return api('auth/login', {'username': usuario, 'password': CLAVE})[1]['token']


def productos_servidor():
    _, pagina = api('productos?pagina=0&tamanio=100', token=token('admin'))
    return pagina['contenido'] if isinstance(pagina, dict) and 'contenido' in pagina else pagina


def nombres_servidor():
    lista = productos_servidor()
    lista = lista if isinstance(lista, list) else []
    return [p.get('nombre') for p in lista if p.get('estado', True)]


# ---------------------------------------------------------------- interfaz
def nodos():
    for _ in range(4):
        sh('uiautomator dump /sdcard/ui.xml >/dev/null 2>&1')
        xml = adb('exec-out', 'cat', '/sdcard/ui.xml')
        try:
            raiz = ET.fromstring(xml[xml.index('<?xml'):] if '<?xml' in xml else xml)
            break
        except Exception:
            time.sleep(1.5)
    else:
        return []
    res = []
    for n in raiz.iter('node'):
        b = [int(v) for v in re.findall(r'-?\d+', n.get('bounds', '[0,0][0,0]'))]
        res.append({'text': n.get('text', ''), 'desc': n.get('content-desc', ''), 'cls': n.get('class', ''),
                    'b': b, 'res': n.get('resource-id', '')})
    return res


def coincide(n, texto=None, contiene=None, desc=None, cls=None):
    if texto is not None and n['text'] != texto:
        return False
    if contiene is not None and contiene not in n['text'] and contiene not in n['desc']:
        return False
    if desc is not None and n['desc'] != desc:
        return False
    if cls is not None and n['cls'] != cls:
        return False
    return n['b'][2] > n['b'][0] and n['b'][3] > n['b'][1]


def buscar(**kw):
    return [n for n in nodos() if coincide(n, **kw)]


def cerrar_anr():
    for n in nodos():
        if "isn't responding" in n['text']:
            for m in nodos():
                if m['text'].lower() == 'wait':
                    tocar(m)


def esperar(timeout=40, **kw):
    limite = time.time() + timeout
    while time.time() < limite:
        hallados = buscar(**kw)
        if hallados:
            return hallados[0]
        cerrar_anr()
        time.sleep(1.5)
    return None


def exigir(nombre, timeout=40, **kw):
    n = esperar(timeout, **kw)
    if n is None:
        captura('error-' + nombre)
        (OUT / f'error-{nombre}.xml').write_text(adb('exec-out', 'cat', '/sdcard/ui.xml'))
        raise AssertionError(f'No apareció {kw} ({nombre})')
    return n


def tocar(n):
    b = n['b']
    sh(f'input tap {(b[0] + b[2]) // 2} {(b[1] + b[3]) // 2}')
    time.sleep(1.2)


def teclado_visible():
    salida = sh('dumpsys input_method')
    return 'mInputShown=true' in salida or 'isInputViewShown=true' in salida


def ocultar_teclado():
    if teclado_visible():
        sh('input keyevent KEYCODE_BACK')
        time.sleep(0.8)


def escribir(campo, texto):
    tocar(campo)
    sh('input keyevent KEYCODE_MOVE_END')
    sh('input keyevent ' + ' '.join(['KEYCODE_DEL'] * 40))
    seguro = texto.replace(' ', '%s')
    sh(f"input text '{seguro}'")
    time.sleep(0.6)


def desplazar(abajo=True):
    if abajo:
        sh('input swipe 540 1900 540 700 400')
    else:
        sh('input swipe 540 700 540 1900 400')
    time.sleep(1.0)


def desplazar_hasta(intentos=8, abajo=True, **kw):
    for _ in range(intentos):
        hallados = buscar(**kw)
        if hallados:
            return hallados[0]
        desplazar(abajo)
    return None


def arriba_del_todo():
    for _ in range(6):
        desplazar(abajo=False)


def captura(nombre):
    time.sleep(1.0)
    with open(OUT / f'{nombre}.png', 'wb') as f:
        f.write(subprocess.run(['adb', 'exec-out', 'screencap', '-p'], capture_output=True).stdout)


def modo_avion(activo):
    sh('cmd connectivity airplane-mode ' + ('enable' if activo else 'disable'))
    time.sleep(5 if activo else 14)


def abrir_app():
    sh(f'monkey -p {PKG} -c android.intent.category.LAUNCHER 1')
    time.sleep(3)


def reabrir_app():
    sh(f'am force-stop {PKG}')
    time.sleep(1)
    abrir_app()


def campos():
    return [n for n in nodos() if n['cls'] == 'android.widget.EditText']


def iniciar_sesion(usuario, clave, esperar_menu=True):
    exigir('login-' + usuario, 60, texto='Inicia sesión para gestionar tu farmacia')
    c = campos()
    escribir(c[0], usuario)
    c = campos()
    escribir(c[1], clave)
    ocultar_teclado()
    tocar(exigir('boton-login', texto='Iniciar sesión'))
    if esperar_menu:
        exigir('menu-' + usuario, 60, desc='Abrir menú')


def asegurar_sesion():
    """Si un paso anterior falló y la app quedó en el inicio de sesión, entra como admin para no arrastrar el fallo."""
    if buscar(texto='Inicia sesión para gestionar tu farmacia'):
        iniciar_sesion('admin', CLAVE)


def ir_a(seccion):
    asegurar_sesion()
    tocar(exigir('menu', 30, desc='Abrir menú'))
    tocar(exigir('drawer-' + seccion, 20, texto=seccion))


def abrir_productos():
    ir_a('Productos')
    exigir('inventario', 40, texto='Inventario offline-first')


def cerrar_sesion():
    arriba_del_todo()
    tocar(exigir('cerrar-sesion', 20, texto='Cerrar sesión'))
    exigir('login-tras-cierre', 30, texto='Inicia sesión para gestionar tu farmacia')


def formulario():
    """Devuelve los tres campos Nombre, Precio, Stock del formulario de productos (abajo de la lista)."""
    desplazar_hasta(12, texto='Crear producto') or desplazar_hasta(4, contiene='Guardar cambios')
    desplazar()
    c = campos()
    if len(c) < 3:
        raise AssertionError(f'Formulario con {len(c)} campos')
    return c[-3:]


def llenar_producto(nombre, precio, stock):
    escribir(formulario()[0], nombre)
    escribir(formulario()[1], precio)
    escribir(formulario()[2], stock)
    ocultar_teclado()


def boton_de_tarjeta(producto, boton):
    """El botón Editar/Eliminar más cercano por debajo del nombre del producto."""
    arriba_del_todo()
    for _ in range(10):
        todos = nodos()
        nombre = [n for n in todos if n['text'] == producto]
        if nombre:
            y = nombre[0]['b'][3]
            candidatos = sorted((n for n in todos if n['text'] == boton and n['b'][1] > y), key=lambda n: n['b'][1])
            if candidatos and candidatos[0]['b'][1] - y < 600:
                return candidatos[0]
        desplazar()
    raise AssertionError(f'No se encontró {boton} de {producto}')


def paso(numero, titulo, funcion):
    inicio = time.time()
    try:
        detalle = funcion()
        resultados.append({'paso': numero, 'titulo': titulo, 'resultado': 'Cumple', 'detalle': detalle})
        log(f'PASO {numero} | {titulo} | Cumple | {detalle} | {time.time() - inicio:.0f} s')
    except Exception as e:
        captura(f'error-paso{numero}')
        resultados.append({'paso': numero, 'titulo': titulo, 'resultado': 'No cumple', 'detalle': str(e)})
        log(f'PASO {numero} | {titulo} | No cumple | {e}')
        modo_avion(False)
        reabrir_app()


# ---------------------------------------------------------------- pasos del recorrido (guía, paso 7)
def p1_instalacion():
    adb('uninstall', PKG)
    salida = adb('install', '-r', APK, timeout=240)
    if 'Success' not in salida:
        raise AssertionError('adb install: ' + salida.strip())
    datos = sh(f'dumpsys package {PKG}')
    version = re.search(r'versionName=(\S+)', datos).group(1)
    codigo = re.search(r'versionCode=(\d+)', datos).group(1)
    depurable = 'DEBUGGABLE' in re.search(r'flags=\[([^\]]*)\]', datos).group(1)
    abrir_app()
    exigir('pantalla-inicial', 60, texto='Inicia sesión para gestionar tu farmacia')
    captura('01-instalacion-y-apertura')
    sh('input keyevent KEYCODE_HOME'); time.sleep(1.5)
    captura('01b-icono-en-el-lanzador')
    abrir_app()
    return (f'Instalado desde el archivo con adb install (sin Android Studio). versionName={version}, '
            f'versionCode={codigo}, depurable={"sí" if depurable else "no"}. Abre en «Inicia sesión».')


def p2_login():
    iniciar_sesion('admin', 'claveIncorrecta123', esperar_menu=False)
    exigir('credenciales-invalidas', 40, texto='Usuario o contraseña incorrectos.')
    captura('02a-login-credencial-invalida')
    modo_avion(True)
    c = campos(); escribir(c[1], CLAVE); ocultar_teclado()
    tocar(exigir('boton-login-2', texto='Iniciar sesión'))
    exigir('sin-conexion', 40, contiene='Sin conexión con PharmaSoft')
    captura('02b-login-sin-conexion')
    modo_avion(False)
    # La app limpia la contraseña después de cada error: se vuelve a escribir.
    c = campos(); escribir(c[1], CLAVE); ocultar_teclado()
    tocar(exigir('boton-login-3', texto='Iniciar sesión'))
    exigir('menu-admin', 60, desc='Abrir menú')
    captura('02c-sesion-iniciada')
    return ('Credencial inválida → «Usuario o contraseña incorrectos.». Modo avión → «Sin conexión con PharmaSoft. '
            'Intenta nuevamente.». Los mensajes se distinguen. Con red y clave correcta entra al menú (JSON de login '
            'deserializado en la versión minificada).')


def p3_crud():
    abrir_productos()
    exigir('en-linea', 60, contiene='todo enviado')
    exigir('lista', 40, texto='S13 Paracetamol 500')
    captura('03a-listado-desde-servidor')
    # Crear
    llenar_producto('S14 Ketoprofeno 100', '8.90', '30')
    tocar(exigir('crear', texto='Crear producto'))
    time.sleep(4)
    arriba_del_todo()
    exigir('creado', 40, texto='S14 Ketoprofeno 100')
    exigir('enviado', 40, contiene='todo enviado')
    captura('03b-producto-creado')
    creado = [p for p in productos_servidor() if p.get('nombre') == 'S14 Ketoprofeno 100']
    if not creado:
        raise AssertionError('El servidor no tiene S14 Ketoprofeno 100')
    # Rechazo de validación
    llenar_producto('Ab', '4.50', '10')
    tocar(exigir('crear-ab', texto='Crear producto'))
    mensaje = 'El nombre debe tener entre 3 y 150 caracteres'
    desplazar_hasta(6, abajo=False, contiene=mensaje) or desplazar_hasta(6, contiene=mensaje)
    n = exigir('validacion', 20, contiene=mensaje)
    nombre_campo = [m for m in campos() if m['b'][1] < n['b'][1]]
    captura('03c-rechazo-de-validacion')
    estado, cuerpo = api('productos', {'nombre': 'Ab', 'precio': 4.5, 'stock': 10, 'estado': True, 'categoriaId': 1},
                         token=token('admin'))
    escribir(formulario()[0], ''); escribir(formulario()[1], ''); escribir(formulario()[2], ''); ocultar_teclado()
    # Editar
    tocar(boton_de_tarjeta('S14 Ketoprofeno 100', 'Editar'))
    c = formulario()
    escribir(c[2], '25'); ocultar_teclado()
    tocar(exigir('guardar-cambios', 20, contiene='Guardar cambios'))
    time.sleep(5)
    arriba_del_todo()
    exigir('enviado-2', 40, contiene='todo enviado')
    captura('03d-producto-editado')
    stock = [p.get('stock') for p in productos_servidor() if p.get('nombre') == 'S14 Ketoprofeno 100']
    # Eliminar
    tocar(boton_de_tarjeta('S14 Ketoprofeno 100', 'Eliminar'))
    exigir('dialogo-eliminar', 20, texto='Eliminar producto')
    captura('03e-confirmar-eliminacion')
    botones = sorted(buscar(texto='Eliminar'), key=lambda m: m['b'][1])
    tocar(botones[-1])
    time.sleep(5)
    sigue = 'S14 Ketoprofeno 100' in nombres_servidor()
    captura('03f-producto-eliminado')
    if sigue or stock != [25]:
        raise AssertionError(f'Edición/eliminación no reflejada en el servidor: stock={stock}, sigue={sigue}')
    return (f'Leer: lista descargada de PharmaSoft. Crear: «S14 Ketoprofeno 100» con id {creado[0].get("id")}. '
            f'Rechazo: nombre «Ab» → «{mensaje}» bajo el campo Nombre; el servidor responde al mismo dato HTTP {estado}: {cuerpo}. '
            f'Editar: stock 30 → {stock[0]} en el servidor. Eliminar: confirmado en el diálogo, ya no aparece en el servidor.')


def p4_sin_privilegio():
    cerrar_sesion()
    iniciar_sesion('vendedor', CLAVE)
    abrir_productos()
    time.sleep(3)
    crear = desplazar_hasta(10, texto='Crear producto')
    arriba_del_todo()
    eliminar = buscar(texto='Eliminar')
    captura('04-vendedor-sin-privilegio')
    estado, _ = api('productos', {'nombre': 'S14 Vendedor', 'precio': 3.0, 'stock': 5, 'estado': True, 'categoriaId': 1},
                    token=token('vendedor'))
    sigue = esperar(10, desc='Abrir menú') is not None
    cerrar_sesion()
    iniciar_sesion('admin', CLAVE)
    if crear or eliminar or estado != 403 or not sigue:
        raise AssertionError(f'crear={bool(crear)} eliminar={bool(eliminar)} http={estado} sesion={sigue}')
    return ('Vendedor (sin CATALOGO_ESCRIBIR): la pantalla no ofrece Crear, Editar ni Eliminar. El mismo POST /productos '
            f'con su token → HTTP {estado} Forbidden. La sesión sigue abierta (menú visible) tras el rechazo.')


def p5_modo_avion():
    modo_avion(True)
    reabrir_app()
    exigir('menu-offline', 60, desc='Abrir menú')
    abrir_productos()
    exigir('sin-conexion-barra', 40, contiene='Sin conexión')
    exigir('datos-offline', 30, texto='S13 Paracetamol 500')
    captura('05a-modo-avion-lectura')
    llenar_producto('S14 Loratadina 10', '5.20', '18')
    tocar(exigir('crear-offline', texto='Crear producto'))
    time.sleep(3)
    arriba_del_todo()
    exigir('pendiente', 40, contiene='S14 Loratadina 10: pendiente')
    exigir('cambio-por-enviar', 40, contiene='1 cambio por enviar')
    captura('05b-modo-avion-alta-pendiente')
    avion = sh('settings get global airplane_mode_on').strip()
    return (f'airplane_mode_on={avion}. Reabierta sin red: restaura la sesión guardada y muestra el inventario local. '
            '«S14 Loratadina 10» queda «pendiente de sincronización» y la barra dice «Sin conexión · 1 cambio por enviar».')


def p6_sincronizar():
    modo_avion(False)
    sola = esperar(45, contiene='todo enviado') is not None
    if not sola:
        tocar(exigir('sync-ahora', 20, texto='Sincronizar ahora'))
        exigir('enviado-3', 60, contiene='todo enviado')
    arriba_del_todo()
    captura('06-sincronizado')
    veces = nombres_servidor().count('S14 Loratadina 10')
    if veces != 1:
        raise AssertionError(f'S14 Loratadina 10 aparece {veces} veces en el servidor')
    pendiente = buscar(contiene='S14 Loratadina 10: pendiente')
    return (('Al volver la red la pantalla se actualizó sola' if sola else
             'Al volver la red se pulsó «Sincronizar ahora»') +
            f'; «En línea · todo enviado». En el servidor «S14 Loratadina 10» existe {veces} vez (sin duplicado); '
            f'marca pendiente en pantalla: {"sí" if pendiente else "no"}.')


def p7_permiso():
    perm = 'android.permission.POST_NOTIFICATIONS'
    sh(f'pm revoke {PKG} {perm}')
    sh(f'pm clear-permission-flags {PKG} {perm} user-set user-fixed')
    reabrir_app()
    exigir('menu-p7', 60, desc='Abrir menú')
    ir_a('Inicio')
    tocar(exigir('pedir-1', 30, texto='Permitir notificaciones'))
    tocar(exigir('dialogo-1', 20, contiene="Don"))  # «Don't allow»
    captura('07a-permiso-denegado')
    tocar(exigir('pedir-2', 30, texto='Permitir notificaciones'))
    segundo = esperar(10, contiene="Don")
    if segundo:
        tocar(segundo)
    exigir('abrir-ajustes', 30, texto='Abrir Ajustes')
    captura('07b-permiso-denegado-para-siempre')
    tocar(exigir('boton-ajustes', texto='Abrir Ajustes'))
    time.sleep(3)
    ajustes = 'com.android.settings' in sh('dumpsys activity activities | grep -m3 -E "topResumedActivity|mResumedActivity"')
    captura('07c-ajustes-de-la-app')
    sh(f'pm grant {PKG} {perm}')
    sh('input keyevent KEYCODE_BACK'); time.sleep(2)
    abrir_app()
    exigir('concedido', 30, contiene='Notificaciones activadas')
    captura('07d-permiso-concedido')
    if not ajustes:
        raise AssertionError('«Abrir Ajustes» no llevó a la pantalla de Ajustes')
    return ('Denegado (1.ª vez): la tarjeta vuelve a ofrecer «Permitir notificaciones». Denegado para siempre (2.ª vez): '
            'muestra «Abrir Ajustes» y el botón abre los Ajustes de la app. Concedido: «Notificaciones activadas». '
            'Al volver a la app el estado se relee en ON_RESUME.')


def p8_cierre():
    modo_avion(True)
    abrir_productos()
    llenar_producto('S14 Cetirizina 10', '3.80', '12')
    tocar(exigir('crear-p8', texto='Crear producto'))
    arriba_del_todo()
    exigir('pendiente-p8', 40, contiene='S14 Cetirizina 10: pendiente')
    cerrar_sesion()
    reabrir_app()
    exigir('login-reabierta', 60, texto='Inicia sesión para gestionar tu farmacia')
    captura('08a-reabierta-sin-token')
    modo_avion(False)
    iniciar_sesion('admin', CLAVE)
    abrir_productos()
    exigir('enviado-p8', 60, contiene='todo enviado')
    arriba_del_todo()
    queda = desplazar_hasta(6, contiene='S14 Cetirizina 10')
    captura('08b-equipo-limpio')
    en_servidor = 'S14 Cetirizina 10' in nombres_servidor()
    if queda or en_servidor:
        raise AssertionError(f'Quedaron datos del usuario anterior: pantalla={bool(queda)} servidor={en_servidor}')
    return ('Al cerrar sesión y reabrir, la app pide credenciales: el token se borró. El alta pendiente del usuario que '
            'salió («S14 Cetirizina 10», sin red) no aparece para el siguiente inicio ni llegó al servidor: la base local '
            'se limpió (hueco 7). Efecto declarado como L-1: no se avisa antes de descartar pendientes.')


def revisar_registro():
    """Solo cuenta excepciones de la app (su paquete, Ktor, Koin, kotlinx.serialization o SQLDelight) y el búfer de fallos;
    el emulador registra ClassNotFoundException propias de Ajustes que no tienen relación con PharmaMobil."""
    completo = adb('logcat', '-d', timeout=120)
    (OUT / 'logcat.txt').write_text(completo)
    fallos = adb('logcat', '-d', '-b', 'crash', timeout=60)
    (OUT / 'logcat-crash.txt').write_text(fallos)
    marcas = ('pe.upeu.pharmamobil', 'pe.edu.upeu.pharmamobil', 'io.ktor', 'org.koin', 'kotlinx.serialization', 'app.cash.sqldelight')
    lineas = completo.splitlines()
    propias = [l for i, l in enumerate(lineas)
               if any(m in ' '.join(lineas[max(0, i - 3):i + 4]) for m in marcas) and ('Exception' in l or 'FATAL' in l)]
    salida = '\n'.join(propias) + '\n' + fallos
    (OUT / 'logcat-app.txt').write_text(salida)
    patrones = ['FATAL EXCEPTION', 'SerializationException', 'NoBeanDefFoundException', 'ClassNotFoundException',
                'NoSuchMethodException', 'NoSuchFieldException', 'InstanceCreationException', 'MissingFieldException']
    conteo = {p: salida.count(p) for p in patrones}
    (OUT / 'logcat-resumen.json').write_text(json.dumps(conteo, indent=1))
    log('LOGCAT | ' + json.dumps(conteo))
    return conteo


if __name__ == '__main__':
    sh('settings put global hide_error_dialogs 1')
    sh('input keyevent KEYCODE_WAKEUP'); sh('wm dismiss-keyguard')
    adb('logcat', '-c')
    log('Android ' + sh('getprop ro.build.version.release').strip() + ' (API ' + sh('getprop ro.build.version.sdk').strip()
        + '), modelo ' + sh('getprop ro.product.model').strip())
    paso(1, 'Instalación desde el archivo firmado', p1_instalacion)
    paso(2, 'Inicio de sesión correcto e incorrecto', p2_login)
    paso(3, 'CRUD completo con un rechazo de validación', p3_crud)
    paso(4, 'Operación sin el privilegio necesario', p4_sin_privilegio)
    paso(5, 'Modo avión: abrir, leer y crear', p5_modo_avion)
    paso(6, 'Recuperar la conexión y sincronizar', p6_sincronizar)
    paso(7, 'Permiso: denegar, denegar para siempre y conceder', p7_permiso)
    paso(8, 'Cerrar sesión y reabrir', p8_cierre)
    conteo = revisar_registro()
    (OUT / 'resultados.json').write_text(json.dumps({'pasos': resultados, 'logcat': conteo}, ensure_ascii=False, indent=1))
    fallos = [r for r in resultados if r['resultado'] != 'Cumple']
    sys.exit(1 if fallos else 0)
