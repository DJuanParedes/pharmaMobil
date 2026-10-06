"""Datos sintéticos reproducibles, ingresados mediante la API real."""
import json
import time
from urllib.request import Request, urlopen

base = 'http://localhost:8080/api/v1/'
for attempt in range(90):
    try:
        with urlopen(base + 'productos', timeout=2) as response:
            assert response.status == 200
        break
    except Exception:
        if attempt == 89:
            raise
        time.sleep(2)

def post(path, payload):
    request = Request(base + path, json.dumps(payload).encode(), {'Content-Type': 'application/json'}, method='POST')
    with urlopen(request) as response:
        return json.load(response)

post('categorias', {'nombre': 'Medicamentos', 'estado': True})
for nombre, precio, stock in [('Paracetamol 500 mg', 15.5, 100), ('Vitamina C 1 g', 1234.56, 4)]:
    post('productos', {'nombre': nombre, 'precio': precio, 'stock': stock, 'estado': True, 'categoriaId': 1})
