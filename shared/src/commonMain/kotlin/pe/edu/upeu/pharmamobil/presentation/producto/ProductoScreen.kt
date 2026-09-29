package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import pe.edu.upeu.pharmamobil.domain.model.Producto

@Composable
fun ProductoScreen(
    uiState: ProductoUiState,
    onFiltroChange: (FiltroInventario) -> Unit,
    onNombreChange: (String) -> Unit,
    onPrecioChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onEditar: (Producto) -> Unit,
    onCancelarEdicion: () -> Unit,
    onEliminar: (Long) -> Unit,
    onLimpiarMensaje: () -> Unit,
    onReintentar: () -> Unit,
) {
    val formulario = uiState.formulario
    val operando = uiState.operacion is ProductoOperacion.EnCurso
    val productosFiltrados = when (val fase = uiState.fase) {
        is ProductoFase.ConProductos -> filtrarInventario(fase.productos, uiState.filtro)
        ProductoFase.Cargando, ProductoFase.SinProductos, is ProductoFase.Error -> emptyList()
    }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Inventario conectado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "CRUD REST de productos sobre PharmaSoft.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (operando) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        uiState.mensajeExito?.let { mensaje ->
            MensajeOperacion(
                mensaje = mensaje,
                esError = false,
                onCerrar = onLimpiarMensaje,
            )
        }
        (uiState.operacion as? ProductoOperacion.Fallida)?.let { operacion ->
            MensajeOperacion(
                mensaje = operacion.mensaje,
                esError = true,
                onCerrar = onLimpiarMensaje,
            )
        }

        PrimaryScrollableTabRow(
            selectedTabIndex = uiState.filtro.ordinal,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            FiltroInventario.entries.forEach { filtro ->
                Tab(
                    selected = uiState.filtro == filtro,
                    onClick = { onFiltroChange(filtro) },
                    text = { Text(filtro.titulo) },
                )
            }
        }

        Text(
            "${uiState.filtro.titulo}: ${productosFiltrados.size}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        when (val fase = uiState.fase) {
            ProductoFase.Cargando -> EstadoCargando()
            ProductoFase.SinProductos -> MensajeSinProductos()
            is ProductoFase.ConProductos -> {
                if (productosFiltrados.isEmpty()) {
                    MensajeSinProductos("No hay productos en esta categoría.")
                } else {
                    productosFiltrados.forEach { producto ->
                        val eliminando = (uiState.operacion as? ProductoOperacion.EnCurso)?.let {
                            it.tipo == ProductoOperacion.Tipo.Eliminar && it.productoId == producto.id
                        } == true
                        ProductoInventarioCard(
                            producto = producto,
                            habilitado = !operando,
                            eliminando = eliminando,
                            onEditar = { onEditar(producto) },
                            onEliminar = { productoAEliminar = producto },
                        )
                    }
                }
            }
            is ProductoFase.Error -> EstadoError(fase.mensaje, onReintentar)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            if (formulario.enEdicion) "Editar producto #${formulario.productoId}" else "Registrar producto",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        CampoTexto(
            valor = formulario.nombre,
            onCambio = onNombreChange,
            etiqueta = "Nombre",
            ejemplo = "Ej. Paracetamol 500 mg",
            error = formulario.nombreError,
            habilitado = !operando,
        )
        CampoTexto(
            valor = formulario.precio,
            onCambio = onPrecioChange,
            etiqueta = "Precio",
            ejemplo = "Ej. 15.50",
            error = formulario.precioError,
            habilitado = !operando,
            teclado = KeyboardType.Decimal,
            prefijo = "S/ ",
        )
        CampoTexto(
            valor = formulario.stock,
            onCambio = onStockChange,
            etiqueta = "Stock",
            ejemplo = "Ej. 100",
            error = formulario.stockError,
            habilitado = !operando,
            teclado = KeyboardType.Number,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (formulario.enEdicion) {
                OutlinedButton(
                    onClick = onCancelarEdicion,
                    enabled = !operando,
                    modifier = Modifier.weight(1f),
                ) { Text("Cancelar edición") }
            }
            Button(
                onClick = onGuardar,
                enabled = !operando,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    when {
                        operando -> "Procesando..."
                        formulario.enEdicion -> "Guardar cambios"
                        else -> "Crear producto"
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    productoAEliminar?.let { producto ->
        AlertDialog(
            onDismissRequest = { productoAEliminar = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Deseas eliminar ${producto.nombre}? Esta acción se enviará a PharmaSoft.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        productoAEliminar = null
                        onEliminar(producto.id)
                    },
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { productoAEliminar = null }) { Text("Volver") }
            },
        )
    }
}

@Composable
private fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    ejemplo: String,
    error: String?,
    habilitado: Boolean,
    teclado: KeyboardType = KeyboardType.Text,
    prefijo: String? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        placeholder = { Text(ejemplo) },
        prefix = prefijo?.let { { Text(it) } },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        enabled = habilitado,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MensajeOperacion(mensaje: String, esError: Boolean, onCerrar: () -> Unit) {
    Surface(
        color = if (esError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (esError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(mensaje, modifier = Modifier.weight(1f))
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        }
    }
}

@Composable
private fun EstadoCargando() {
    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text("Cargando productos...", Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun MensajeSinProductos(mensaje: String = "Todavía no hay productos registrados.") {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(mensaje, Modifier.padding(20.dp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun EstadoError(mensaje: String, onReintentar: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(mensaje, textAlign = TextAlign.Center)
            Button(onClick = onReintentar) { Text("Reintentar") }
        }
    }
}

@Composable
private fun ProductoInventarioCard(
    producto: Producto,
    habilitado: Boolean,
    eliminando: Boolean,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(producto.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("S/ ${formatearPrecio(producto.precio)}", color = MaterialTheme.colorScheme.primary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Stock: ${producto.stock}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                EstadoProducto(producto)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onEditar, enabled = habilitado, modifier = Modifier.weight(1f)) {
                    Text("Editar")
                }
                Button(onClick = onEliminar, enabled = habilitado, modifier = Modifier.weight(1f)) {
                    Text(if (eliminando) "Eliminando..." else "Eliminar")
                }
            }
        }
    }
}

@Composable
private fun EstadoProducto(producto: Producto) {
    val (texto, color) = when {
        !producto.activo -> "Inactivo" to MaterialTheme.colorScheme.secondary
        producto.requiereReposicion() -> "Bajo stock" to MaterialTheme.colorScheme.error
        else -> "Activo" to MaterialTheme.colorScheme.primary
    }
    Surface(color = color.copy(alpha = 0.12f), contentColor = color, shape = MaterialTheme.shapes.small) {
        Text(texto, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.SemiBold)
    }
}

private fun formatearPrecio(precio: Double): String {
    val centimos = (precio * 100).roundToInt()
    return "${centimos / 100}.${(centimos % 100).toString().padStart(2, '0')}"
}
