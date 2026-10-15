"""Pruebas reales Android/MySQL. Preparar el backend con TTL de un minuto antes de ejecutar.

Credenciales: PHARMA_ADMIN_PASSWORD, PHARMA_VENDEDOR_PASSWORD, PHARMA_ALMACEN_PASSWORD,
DB_PASSWORD (usuario pharma_app). No guarda contraseñas ni JWT en las evidencias.
"""
import os,subprocess,time,sys,json,shutil,threading
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ADB=os.environ.get('ADB',str(Path(os.environ['LOCALAPPDATA'])/'Android/Sdk/platform-tools/adb.exe'))
MYSQL=os.environ['MYSQL_BIN']
OUT=ROOT/'evidencias/sesion10-autonoma';OUT.mkdir(parents=True,exist_ok=True)
def adb(*args,check=True):return subprocess.run([ADB,*args],check=check,capture_output=True,text=True,encoding='utf-8',errors='replace')
def sql(query):
 env=dict(os.environ,MYSQL_PWD=os.environ['DB_PASSWORD'])
 return subprocess.run([MYSQL,'-h127.0.0.1','-upharma_app','-N','pharma_s10','-e',query],env=env,check=True,capture_output=True,text=True).stdout.strip()
def exists(name):return adb('shell','ls',f'/sdcard/Download/S10AA/{name}',check=False).returncode==0
def flag(name):adb('shell','touch',f'/sdcard/Download/S10AA/{name}')
def instrument(method,coordinate=False):
 args=[ADB,'shell','am','instrument','-w','-r','-e','class',f'pe.edu.upeu.pharmamobil.AutonomaSesion10Test#{method}']
 for user,key in [('admin','ADMIN'),('vendedor','VENDEDOR'),('almacen','ALMACEN')]:args+=['-e',user+'Password',os.environ['PHARMA_'+key+'_PASSWORD']]
 args+=['pe.edu.upeu.pharmamobil.test/androidx.test.runner.AndroidJUnitRunner']
 p=subprocess.Popen(args,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8',errors='replace')
 captured=[]
 reader=threading.Thread(target=lambda: captured.append(p.stdout.read()),daemon=True);reader.start()
 disabled=False;reactivated=False;start=time.monotonic()
 try:
  while p.poll() is None:
   if time.monotonic()-start>300:p.terminate();raise TimeoutError(method)
   if coordinate and not disabled and exists('deshabilitar.flag'):
    sql("UPDATE usuarios SET estado=b'0' WHERE username='admin';")
    assert sql("SELECT estado+0 FROM usuarios WHERE username='admin';")=='0'
    flag('deshabilitado.flag');disabled=True;print('Usuario deshabilitado en MySQL para escenario 6.',flush=True)
   if coordinate and disabled and not reactivated and exists('reactivar.flag'):
    sql("UPDATE usuarios SET estado=b'1' WHERE username='admin';")
    flag('reactivado.flag');reactivated=True;print('Usuario reactivado.',flush=True)
   time.sleep(1)
  reader.join(timeout=10);output=''.join(captured)
  (OUT/(method+'.txt')).write_text(output,encoding='utf-8')
  if p.returncode or 'FAILURES!!!' in output or 'OK (1 test)' not in output:raise RuntimeError('Falló '+method+'. Revisar salida de instrumentación.')
  print(method+': PASO',flush=True)
 finally:
  if coordinate:sql("UPDATE usuarios SET estado=b'1' WHERE username='admin';")

adb('wait-for-device')
for apk in [ROOT/'androidApp/build/outputs/apk/debug/androidApp-debug.apk',ROOT/'androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk']:
 adb('install','-r',str(apk))
adb('shell','mkdir','-p','/sdcard/Download/S10AA')
# Solo elimina banderas del ensayo previo; conserva sus capturas hasta que se regeneren.
for name in ['deshabilitar.flag','deshabilitado.flag','reactivar.flag','reactivado.flag','persistencia-lista.flag']:
 adb('shell','rm','-f','/sdcard/Download/S10AA/'+name)
sql("UPDATE usuarios SET estado=b'1' WHERE username='admin';")
adb('logcat','-c')
try:
 instrument('seisEscenariosRealesYArchivoCifrado',True)
 adb('shell','am','force-stop','pe.edu.upeu.pharmamobil')
 adb('shell','am','start','-n','pe.edu.upeu.pharmamobil/.MainActivity')
 time.sleep(4)
 instrument('cierreCompleto')
 instrument('matrizDeInterfaz')
finally:
 sql("UPDATE usuarios SET estado=b'1' WHERE username='admin';")
 adb('pull','/sdcard/Download/S10AA',str(OUT/'android'),check=False)
 logs=adb('logcat','-d','-v','time').stdout
 lines=[line for line in logs.splitlines() if 'Ktor:' in line or 'Authorization: ***' in line]
 # Nunca almacena el logcat completo, que puede contener datos de otras aplicaciones.
 (OUT/'ktor-saneado.txt').write_text('\n'.join(lines),encoding='utf-8')
