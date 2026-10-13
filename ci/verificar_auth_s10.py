"""Matriz de roles contra PharmaSoft real. No guarda tokens ni contraseñas."""
import json,sys,time,os
from pathlib import Path
from urllib.request import Request,urlopen
from urllib.error import HTTPError
base='http://localhost:8080/api/v1/'
def llamada(ruta,token=None,payload=None,method=None):
 h={'Content-Type':'application/json'}
 if token: h['Authorization']='Bearer '+token
 req=Request(base+ruta,json.dumps(payload).encode() if payload is not None else None,h,method=method)
 try:
  with urlopen(req,timeout=15) as r: return r.status,json.loads(r.read() or b'{}')
 except HTTPError as e: return e.code,json.loads(e.read() or b'{}')
def login(user,pw):
 status,data=llamada('auth/login',payload={'username':user,'password':pw})
 assert status==200,(user,status)
 return data['token']
admin=login('admin',os.environ['PHARMA_ADMIN_PASSWORD'])
status,cat=llamada('categorias',admin,{'nombre':'S10 Medicamentos','estado':True})
assert status in (200,201),(status,cat)
catid=cat['id']
status,prod=llamada('productos',admin,{'nombre':'S10 Paracetamol','precio':15.5,'stock':100,'estado':True,'categoriaId':catid})
assert status in (200,201),(status,prod)
status,cliente=llamada('clientes',admin,{'dni':'87654321','nombres':'Cliente','apellidos':'Laboratorio','email':'laboratorio@example.test','telefono':'987654321','direccion':'Datos sintéticos','estado':True})
assert status in (200,201),(status,cliente)
result={'sin_token':llamada('productos')[0],'usuarios':{},'backend':'dreyna/pharmaSoft@fff44f9','base':'H2 modo Oracle con Flyway V1 V2 V3'}
for user,pw,expected in [('admin',os.environ['PHARMA_ADMIN_PASSWORD'],[200,201,200,201,200,200]),('vendedor',os.environ['PHARMA_VENDEDOR_PASSWORD'],[200,403,200,201,403,200]),('almacen',os.environ['PHARMA_ALMACEN_PASSWORD'],[200,201,403,403,403,200])]:
 token=login(user,pw)
 payload={'nombre':'S10 Producto '+user,'precio':12.5,'stock':40,'estado':True,'categoriaId':catid}
 operations=[('listar_productos','productos',None),('registrar_producto','productos',payload),('listar_clientes','clientes',None),('registrar_venta','ventas',{'clienteId':cliente['id'],'detalles':[{'productoId':prod['id'],'cantidad':1}]}),('reportes','reportes/productos-mas-vendidos',None),('usuario_actual','auth/me',None)]
 row={}
 for (name,path,body),want in zip(operations,expected):
  status,_=llamada(path,token,body)
  assert status==want or want==201 and status==200,(user,name,status,want)
  row[name]=status
 result['usuarios'][user]=row
assert result['sin_token']==401
out=Path(sys.argv[1]);out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(result,indent=2,ensure_ascii=False),encoding='utf-8')
print(json.dumps(result,indent=2,ensure_ascii=True))
