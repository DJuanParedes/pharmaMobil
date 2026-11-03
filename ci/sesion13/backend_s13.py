"""PharmaSoft real con H2 para la sesión 13. Credenciales efímeras; no usa cuentas del alumno."""
import os, sys, subprocess, time, json, re, secrets
from pathlib import Path
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.request import Request, urlopen
import bcrypt
root = Path(__file__).resolve().parents[2]
ev = root / 'evidencias-ci'; ev.mkdir(exist_ok=True)
out = Path(os.environ.get('RUNNER_TEMP', '/tmp')) / 'backend-s13'
subprocess.run([sys.executable, str(root/'ci/preparar_backend_s10.py'), str(root/'backend/pharmaSoft'), str(out)], check=True)
password = secrets.token_urlsafe(18)
hashpw = bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()
for path in (out/'resources/db/migration').glob('*.sql'):
    path.write_text(re.sub(r'\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}', lambda m: hashpw, path.read_text()))
with (out/'resources/application-s10.properties').open('a') as f:
    f.write('\nspring.flyway.locations=classpath:db/migration\n')
with open(ev/'backend-build.txt', 'w') as log:
    subprocess.run(['mvn','-B','-f',str(out/'pom.xml'),'package','-DskipTests'], stdout=log, stderr=subprocess.STDOUT, check=True)
backendlog = open(ev/'backend.txt','w')
backend = subprocess.Popen(['java','-jar',str(out/'target/PharmaBackend-0.0.1-SNAPSHOT.jar')],
                           env=dict(os.environ, SPRING_PROFILES_ACTIVE='s10'), stdout=backendlog, stderr=subprocess.STDOUT)
def api(path, body=None, token=None):
    headers = {'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    data = json.dumps(body).encode() if body is not None else None
    with urlopen(Request('http://127.0.0.1:8080/api/v1/'+path, data, headers), timeout=10) as r: return json.load(r)
for intento in range(120):
    try:
        token = api('auth/login',{'username':'admin','password':password})['token']; break
    except Exception:
        if backend.poll() is not None: raise RuntimeError('Backend terminó; revisar backend.txt')
        time.sleep(2)
else: raise RuntimeError('No se pudo autenticar el backend de CI')
cat = api('categorias',{'nombre':'S13 Medicamentos','estado':True},token)
for nombre, stock in [('S13 Paracetamol 500', 40), ('S13 Amoxicilina 500', 15)]:
    api('productos',{'nombre':nombre,'precio':4.5,'stock':stock,'estado':True,'categoriaId':cat['id']},token)
(ev/'semilla.json').write_text(json.dumps({'categoria': cat, 'productos': ['S13 Paracetamol 500','S13 Amoxicilina 500']}, ensure_ascii=False, indent=1))
class Control(BaseHTTPRequestHandler):
    def log_message(self,*args): pass
    def do_GET(self):
        if self.path == '/credenciales': payload={'password':password}
        elif self.path == '/productos': payload=api('productos?pagina=0&tamanio=50', token=token)
        else: payload={'listo':True}
        data=json.dumps(payload, ensure_ascii=False).encode();self.send_response(200);self.end_headers();self.wfile.write(data)
try: HTTPServer(('127.0.0.1',8099),Control).serve_forever()
finally:
    if backend.poll() is None: backend.terminate()
