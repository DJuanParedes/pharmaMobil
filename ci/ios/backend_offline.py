"""Backend real aislado para pruebas iOS. Credenciales efímeras; no usa cuentas del alumno."""
import os, sys, subprocess, time, json, re, secrets
from pathlib import Path
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.request import Request, urlopen
import bcrypt
root = Path(__file__).resolve().parents[2]
out = Path(os.environ['RUNNER_TEMP']) / 'backend-s11'
subprocess.run([sys.executable, str(root/'ci/preparar_backend_s10.py'), str(root/'backend/pharmaSoft'), str(out)], check=True)
password = secrets.token_urlsafe(18)
hashpw = bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()
for path in (out/'resources/db/migration').glob('*.sql'):
    text = path.read_text()
    text = re.sub(r'\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}', lambda m: hashpw, text)
    path.write_text(text)
with (out/'resources/application-s10.properties').open('a') as f:
    f.write('\nspring.flyway.locations=classpath:db/migration\n')
log = open(root/'evidencias-ci/backend-build.txt', 'w')
subprocess.run(['mvn','-B','-f',str(out/'pom.xml'),'package','-DskipTests'], stdout=log, stderr=subprocess.STDOUT, check=True)
log.close()
backendlog = open(root/'evidencias-ci/backend.txt','w')
env = dict(os.environ, SPRING_PROFILES_ACTIVE='s10')
backend = subprocess.Popen(['java','-jar',str(out/'target/PharmaBackend-0.0.1-SNAPSHOT.jar')],env=env,stdout=backendlog,stderr=subprocess.STDOUT)
def api(path, body, token=None):
    headers = {'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    with urlopen(Request('http://127.0.0.1:8080/api/v1/'+path, json.dumps(body).encode(),headers),timeout=10) as r: return json.load(r)
for intento in range(90):
    try:
        token = api('auth/login',{'username':'admin','password':password})['token']; break
    except Exception:
        if backend.poll() is not None: raise RuntimeError('Backend terminó; revisar backend.txt')
        time.sleep(2)
else: raise RuntimeError('No se pudo autenticar el backend de CI')
cat = api('categorias',{'nombre':'S11 pruebas','estado':True},token)
api('productos',{'nombre':'AA S11 iOS persistente','precio':4.5,'stock':20,'estado':True,'categoriaId':cat['id']},token)
class Control(BaseHTTPRequestHandler):
    def log_message(self,*args): pass
    def do_GET(self):
        if self.path == '/credenciales': payload={'password':password}
        elif self.path == '/offline':
            backend.terminate(); backend.wait(timeout=20); payload={'backend':'apagado'}
        else: payload={'listo':True}
        data=json.dumps(payload).encode();self.send_response(200);self.end_headers();self.wfile.write(data)
try: HTTPServer(('127.0.0.1',8099),Control).serve_forever()
finally:
    if backend.poll() is None: backend.terminate()
