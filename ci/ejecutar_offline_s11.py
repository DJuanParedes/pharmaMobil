"""Ciclo real Android: requiere PHARMA_ADMIN_PASSWORD y el backend local activo."""
import os, subprocess, time, json, shlex
from pathlib import Path
from urllib.request import Request, urlopen
ROOT=Path(__file__).resolve().parents[1]
ADB=os.environ.get('ADB',str(Path(os.environ['LOCALAPPDATA'])/'Android/Sdk/platform-tools/adb.exe'))
OUT=ROOT/'evidencias/sesion11/android';OUT.mkdir(parents=True,exist_ok=True)
PASSWORD=os.environ['PHARMA_ADMIN_PASSWORD']
NAME='AA S11 Paredes '+time.strftime('%H%M%S')
def adb(*args):
    p=subprocess.run([ADB,*args],capture_output=True,text=True,encoding='utf-8',errors='replace')
    if p.returncode: raise RuntimeError('ADB falló: '+p.stderr.replace(PASSWORD,'[REDACTADO]'))
    return p.stdout
def shell(*args): return adb('shell',shlex.join(args))
def instrument(method,filename):
    args=['am','instrument','--user','0','-w','-r','-e','class','pe.edu.upeu.pharmamobil.OfflineSesion11Test#'+method,
          '-e','productoNombre',NAME]
    if method=='prepararConRed': args+=['-e','adminPassword',PASSWORD]
    output=shell(*args,'pe.edu.upeu.pharmamobil.test/androidx.test.runner.AndroidJUnitRunner')
    (OUT/filename).write_text(output.replace(PASSWORD,'[REDACTADO]'),encoding='utf-8')
    if 'OK (1 test)' not in output: raise RuntimeError('Falló '+method+'; revisar '+filename)
    print(method+': APROBADO',flush=True)
def api(path,body=None,token=None):
    headers={'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    req=Request('http://localhost:8080/api/v1/'+path,json.dumps(body).encode() if body else None,headers)
    with urlopen(req,timeout=20) as response: return json.load(response)
for file in ['androidApp/build/outputs/apk/debug/androidApp-debug.apk','androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk']:
    adb('install','-r',str(ROOT/file))
shell('input','keyevent','KEYCODE_WAKEUP');shell('wm','dismiss-keyguard')
try:
    instrument('prepararConRed','01-preparar.txt')
    shell('am','force-stop','--user','0','pe.edu.upeu.pharmamobil')
    shell('cmd','connectivity','airplane-mode','enable')
    shell('svc','wifi','disable');shell('svc','data','disable');time.sleep(3)
    state=shell('settings','get','global','airplane_mode_on').strip()
    assert state=='1'
    (OUT/'modo-avion.txt').write_text('airplane_mode_on='+state+'\n',encoding='utf-8')
    instrument('abrirSinRedYRegistrar','02-offline.txt')
finally:
    shell('cmd','connectivity','airplane-mode','disable')
    shell('svc','wifi','enable');shell('svc','data','enable')
time.sleep(5)
instrument('reconectarYVerificarServidor','03-reconexion.txt')
for name in ['01-listado-con-red','02-reapertura-modo-avion','03-producto-pendiente','04-reconexion-confirmada']:
    adb('pull','/sdcard/Android/data/pe.edu.upeu.pharmamobil/files/s11/'+name+'.png',str(OUT/(name+'.png')))
token=api('auth/login',{'username':'admin','password':PASSWORD})['token']
page=0; found=[]
while True:
    data=api('productos?pagina='+str(page)+'&tamanio=100',token=token)
    found += [p for p in data['contenido'] if p['nombre']==NAME]
    if data['ultima']:break
    page+=1
assert len(found)==1 and found[0]['id']>0
product=api('productos/'+str(found[0]['id']),token=token)
(OUT.parent/'producto-servidor.json').write_text(json.dumps(product,ensure_ascii=False,indent=2),encoding='utf-8')
print('Verificado en servidor: id='+str(product['id'])+'; nombre='+NAME,flush=True)
