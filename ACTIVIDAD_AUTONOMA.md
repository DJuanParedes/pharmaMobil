# Actividad autónoma: procesamiento reactivo de pedidos

## 1. Situación problemática

PharmaMobil necesita procesar pedidos sin vender productos inexistentes ni cantidades mayores al stock disponible. Cuando un pedido es aceptado, el inventario debe actualizarse de forma inmutable y el cambio debe quedar disponible para las interfaces Android e iOS mediante un flujo reactivo.

## 2. Objetivo

Aplicar los conceptos de la sesión de Kotlin y dominio KMP en una solución ubicada completamente en `shared/src/commonMain`:

- null-safety y tipos estrictos;
- data classes, inmutabilidad y `copy()`;
- consultas de colecciones;
- estados y resultados con `sealed class`;
- funciones `suspend`, corrutinas y `Flow`;
- pruebas comunes reutilizables por las plataformas.

## 3. Reglas de negocio

1. Solo se procesan pedidos con estado `Pendiente`.
2. Todos los productos solicitados deben existir en el inventario.
3. Si un producto aparece en varios detalles, sus cantidades se suman.
4. El stock disponible debe cubrir la cantidad total solicitada.
5. Un pedido válido cambia a `Procesando`.
6. El inventario se reemplaza por una nueva lista; los objetos originales no se mutan.
7. Un pedido rechazado por reglas de negocio no modifica el inventario.

## 4. Diseño de la solución

- `PedidoService`: evalúa las reglas con `groupBy`, `mapValues`, `find` y `map`.
- `ResultadoProcesamientoPedido`: representa éxito, producto inexistente, stock insuficiente o estado inválido.
- `InventarioRepository`: contrato asíncrono para procesar pedidos y observar el inventario.
- `InventarioRepositoryMemoria`: implementación con `Mutex` y `MutableStateFlow`.
- `ProcesarPedidoUseCase`: punto de entrada del dominio para las futuras interfaces.
- `DemoActividadAutonoma`: ejemplo ejecutable del caso de éxito.

Flujo principal:

```text
Pedido pendiente
      |
ProcesarPedidoUseCase
      |
InventarioRepositoryMemoria -- Mutex
      |
PedidoService -- reglas de negocio
      |
Resultado sellado + nueva emisión de Flow
```

## 5. Casos comprobados

- pedido válido: descuenta stock y cambia a `Procesando`;
- producto inexistente: devuelve el identificador faltante;
- stock insuficiente: informa cantidad solicitada y disponible;
- detalles repetidos: suma las cantidades antes de validar;
- estado no permitido: rechaza pedidos que ya están en proceso;
- inventario reactivo: emite el valor inicial y el inventario actualizado.

## 6. Validación

En Windows:

```powershell
.\gradlew.bat :shared:testAndroidHostTest
.\gradlew.bat :androidApp:assembleDebug
```

La implementación permanece en código Kotlin común, sin dependencias específicas de Android o iOS.
