"""Genera una colección Postman reproducible sin credenciales reales."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def item(name,headers=None,code=401,message=None,prerequest=None,description=''):
    test=[f'pm.test("HTTP {code}", () => pm.response.to.have.status({code}));']
    if message:test.append('pm.test("Mensaje literal", () => pm.expect(pm.response.json().message).to.eql('+json.dumps(message)+'));')
    events=[{'listen':'test','script':{'type':'text/javascript','exec':test}}]
    if prerequest:events.insert(0,{'listen':'prerequest','script':{'type':'text/javascript','exec':prerequest}})
    return {'name':name,'event':events,'request':{'method':'GET','header':[{'key':k,'value':v} for k,v in (headers or {}).items()],'url':'{{baseUrl}}/productos','description':description}}
login={'name':'00 Login admin (guardar token local)','request':{'method':'POST','header':[{'key':'Content-Type','value':'application/json'}],'url':'{{baseUrl}}/auth/login','body':{'mode':'raw','raw':json.dumps({'username':'admin','password':'{{adminPassword}}'})}},'event':[{'listen':'test','script':{'type':'text/javascript','exec':['pm.test("Login 200", () => pm.response.to.have.status(200));','pm.collectionVariables.set("jwt", pm.response.json().token);']}}]}
data={'info':{'name':'S10 Actividad autónoma PharmaMobil - MySQL','schema':'https://schema.getpostman.com/json/collection/v2.1.0/collection.json','description':'Ejecución manual. Login antes de los casos 1,2,3,5,6. Caso 2: iniciar backend con TTL=1 y esperar al menos 65 segundos después del login. Caso 6: deshabilitar admin en MySQL con sesión abierta y reactivarlo en finally/al finalizar. Restaurar TTL=60. No exportar variables con claves o JWT.'},'variable':[{'key':'baseUrl','value':'http://localhost:8080/api/v1'},{'key':'adminPassword','value':''},{'key':'jwt','value':''}],'item':[login,
item('01 Token válido',{'Authorization':'Bearer {{jwt}}'},200),
item('02 Token expirado (esperar 65 segundos)',{'Authorization':'Bearer {{jwt}}'},message='El token ha expirado. Inicie sesión nuevamente'),
item('03 Firma alterada',{'Authorization':'Bearer {{alterado}}'},message='El token no es válido',prerequest=['const t = pm.collectionVariables.get("jwt");','const i = t.lastIndexOf(".") + 1;','pm.variables.set("alterado", t.slice(0,i)+(t[i]==="A"?"B":"A")+t.slice(i+1));']),
item('04 Sin cabecera (No Auth)',message='Debe iniciar sesión para acceder a este recurso'),
item('05 Prefijo bearer minúsculo',{'Authorization':'bearer {{jwt}}'},message='Debe iniciar sesión para acceder a este recurso'),
item('06 Usuario deshabilitado',{'Authorization':'Bearer {{jwt}}'},message='El usuario está deshabilitado',description="Con sesión abierta ejecutar en MySQL UPDATE usuarios SET estado=b'0' WHERE username='admin';. Restaurar a b'1' inmediatamente al finalizar.")]}
data['auth']={'type':'noauth'}
path=root/'documentacion/S10_Autonomia_Postman.json';path.parent.mkdir(exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8');print(path)
