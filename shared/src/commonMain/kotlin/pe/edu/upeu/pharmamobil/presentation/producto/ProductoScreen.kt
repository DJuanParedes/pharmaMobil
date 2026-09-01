package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import pe.edu.upeu.pharmamobil.domain.model.Producto

private const val MENSAJE_REGISTRO_EXITOSO = "Producto registrado correctamente."
private const val ERROR_NOMBRE = "Nombre obligatorio"
private const val ERROR_PRECIO_NUMERICO = "Precio inválido"
private const val ERROR_PRECIO_POSITIVO = "El precio debe ser mayor a 0"
private const val ERROR_STOCK_ENTERO = "Stock debe ser un número entero"
private const val ERROR_STOCK_NEGATIVO = "Stock no puede ser negativo"

internal enum class CampoProducto {
    NOMBRE,
    PRECIO,
    STOCK,
}

internal sealed interface ResultadoRegistroProducto {
    data class Exito(val producto: Producto) : ResultadoRegistroProducto

    data class Error(
        val campo: CampoProducto,
        val mensaje: String,
    ) : ResultadoRegistroProducto
}

internal fun validarProductoRegistro(
    nombre: String,
    precio: String,
    stock: String,
): ResultadoRegistroProducto {
    val precioNumerico = precio.toDoubleOrNull()
    val stockNumerico = stock.toIntOrNull()

    when {
        nombre.isBlank() -> {
            return ResultadoRegistroProducto.Error(CampoProducto.NOMBRE, ERROR_NOMBRE)
        }

        precioNumerico == null -> {
            return ResultadoRegistroProducto.Error(
                CampoProducto.PRECIO,
                ERROR_PRECIO_NUMERICO,
            )
        }

        precioNumerico <= 0.0 -> {
            return ResultadoRegistroProducto.Error(
                CampoProducto.PRECIO,
                ERROR_PRECIO_POSITIVO,
            )
        }

        stockNumerico == null -> {
            return ResultadoRegistroProducto.Error(
                CampoProducto.STOCK,
                ERROR_STOCK_ENTERO,
            )
        }

        stockNumerico < 0 -> {
            return ResultadoRegistroProducto.Error(
                CampoProducto.STOCK,
                ERROR_STOCK_NEGATIVO,
            )
        }
    }

    return ResultadoRegistroProducto.Exito(
        Producto(
            id = 1L,
            nombre = nombre.trim(),
            precio = precioNumerico,
            stock = stockNumerico,
        )
    )
}

@Composable
fun ProductoScreen(
    productos: List<Producto>,
    onInventarioChange: (List<Producto>) -> Unit,
) {
    var tabSeleccionada by rememberSaveable { mutableStateOf(FiltroInventario.ACTIVOS.ordinal) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var precio by rememberSaveable { mutableStateOf("") }
    var stock by rememberSaveable { mutableStateOf("") }
    var mensaje by rememberSaveable { mutableStateOf("") }
    var productoRegistrado by remember { mutableStateOf<Producto?>(null) }
    var registroExitoso by rememberSaveable { mutableStateOf(false) }
    var intentoRegistrar by rememberSaveable { mutableStateOf(false) }

    val filtroActual = FiltroInventario.entries[tabSeleccionada]
    val productosFiltrados = filtrarInventario(productos, filtroActual)
    val errorActual = if (intentoRegistrar) {
        validarProductoRegistro(nombre, precio, stock) as? ResultadoRegistroProducto.Error
    } else {
        null
    }

    fun actualizarRetroalimentacion(
        nuevoNombre: String = nombre,
        nuevoPrecio: String = precio,
        nuevoStock: String = stock,
    ) {
        mensaje = if (intentoRegistrar) {
            when (
                val resultado = validarProductoRegistro(
                    nuevoNombre,
                    nuevoPrecio,
                    nuevoStock,
                )
            ) {
                is ResultadoRegistroProducto.Error -> resultado.mensaje
                is ResultadoRegistroProducto.Exito -> ""
            }
        } else {
            ""
        }
        productoRegistrado = null
        registroExitoso = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Inventario de Productos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Consulta el inventario por estado y registra nuevos productos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PrimaryScrollableTabRow(
            selectedTabIndex = tabSeleccionada,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            FiltroInventario.entries.forEachIndexed { index, filtro ->
                Tab(
                    selected = tabSeleccionada == index,
                    onClick = { tabSeleccionada = index },
                    text = { Text(filtro.titulo) },
                )
            }
        }

        Text(
            text = "${filtroActual.titulo}: ${productosFiltrados.size}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (productosFiltrados.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "No hay productos en esta categoría.",
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            productosFiltrados.forEach { producto ->
                ProductoInventarioCard(producto)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "Registrar producto",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                actualizarRetroalimentacion(nuevoNombre = it)
            },
            label = { Text("Nombre") },
            placeholder = { Text("Ej. Paracetamol 500 mg") },
            isError = errorActual?.campo == CampoProducto.NOMBRE,
            supportingText = if (errorActual?.campo == CampoProducto.NOMBRE) {
                { Text(errorActual.mensaje) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = precio,
            onValueChange = {
                precio = it
                actualizarRetroalimentacion(nuevoPrecio = it)
            },
            label = { Text("Precio") },
            placeholder = { Text("Ej. 15.50") },
            prefix = { Text("S/ ") },
            isError = errorActual?.campo == CampoProducto.PRECIO,
            supportingText = if (errorActual?.campo == CampoProducto.PRECIO) {
                { Text(errorActual.mensaje) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = stock,
            onValueChange = {
                stock = it
                actualizarRetroalimentacion(nuevoStock = it)
            },
            label = { Text("Stock") },
            placeholder = { Text("Ej. 100") },
            isError = errorActual?.campo == CampoProducto.STOCK,
            supportingText = if (errorActual?.campo == CampoProducto.STOCK) {
                { Text(errorActual.mensaje) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                intentoRegistrar = true
                when (val resultado = validarProductoRegistro(nombre, precio, stock)) {
                    is ResultadoRegistroProducto.Error -> {
                        mensaje = resultado.mensaje
                        productoRegistrado = null
                        registroExitoso = false
                    }

                    is ResultadoRegistroProducto.Exito -> {
                        val inventarioActualizado = agregarProductoAlInventario(
                            productos = productos,
                            producto = resultado.producto,
                        )
                        val productoAgregado = inventarioActualizado.last()
                        onInventarioChange(inventarioActualizado)
                        tabSeleccionada = if (esProductoDeBajoStock(productoAgregado)) {
                            FiltroInventario.BAJO_STOCK.ordinal
                        } else {
                            FiltroInventario.ACTIVOS.ordinal
                        }
                        mensaje = MENSAJE_REGISTRO_EXITOSO
                        productoRegistrado = productoAgregado
                        registroExitoso = true
                        nombre = ""
                        precio = ""
                        stock = ""
                        intentoRegistrar = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Registrar")
        }

        if (mensaje.isNotEmpty()) {
            Text(
                text = mensaje,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (registroExitoso) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        productoRegistrado?.let { producto ->
            Text(
                text = "ID: ${producto.id} | ${producto.nombre} | " +
                    "S/ ${formatearPrecio(producto.precio)} | Stock: ${producto.stock}",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ProductoInventarioCard(producto: Producto) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "S/ ${formatearPrecio(producto.precio)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Stock: ${producto.stock}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                EstadoProducto(producto)
            }
        }
    }
}

@Composable
private fun EstadoProducto(producto: Producto) {
    val (texto, color) = when {
        !producto.activo -> "Inactivo" to MaterialTheme.colorScheme.secondary
        esProductoDeBajoStock(producto) -> "Bajo stock" to MaterialTheme.colorScheme.error
        else -> "Activo" to MaterialTheme.colorScheme.primary
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

private fun formatearPrecio(precio: Double): String {
    val centimos = (precio * 100).roundToInt()
    val parteEntera = centimos / 100
    val parteDecimal = (centimos % 100).toString().padStart(2, '0')
    return "$parteEntera.$parteDecimal"
}
