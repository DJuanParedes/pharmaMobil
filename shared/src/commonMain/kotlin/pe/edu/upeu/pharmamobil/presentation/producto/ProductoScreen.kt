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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.usecase.CampoProducto

@Composable
fun ProductoScreen(
    uiState: ProductoUiState,
    onFiltroChange: (FiltroInventario) -> Unit,
    onNombreChange: (String) -> Unit,
    onPrecioChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onRegistrar: () -> Unit,
    onReintentar: () -> Unit,
) {
    val formulario = uiState.formulario
    val productosFiltrados = when (val fase = uiState.fase) {
        is ProductoFase.ConProductos -> filtrarInventario(fase.productos, uiState.filtro)
        ProductoFase.Cargando,
        ProductoFase.SinProductos,
        is ProductoFase.Error,
        -> emptyList()
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
            text = "${uiState.filtro.titulo}: ${productosFiltrados.size}",
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
                        ProductoInventarioCard(producto)
                    }
                }
            }

            is ProductoFase.Error -> EstadoError(
                mensaje = fase.mensaje,
                onReintentar = onReintentar,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "Registrar producto",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = formulario.nombre,
            onValueChange = onNombreChange,
            label = { Text("Nombre") },
            placeholder = { Text("Ej. Paracetamol 500 mg") },
            isError = formulario.campoConError == CampoProducto.NOMBRE,
            supportingText = formulario.mensajePara(CampoProducto.NOMBRE),
            singleLine = true,
            enabled = !formulario.enProceso,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = formulario.precio,
            onValueChange = onPrecioChange,
            label = { Text("Precio") },
            placeholder = { Text("Ej. 15.50") },
            prefix = { Text("S/ ") },
            isError = formulario.campoConError == CampoProducto.PRECIO,
            supportingText = formulario.mensajePara(CampoProducto.PRECIO),
            singleLine = true,
            enabled = !formulario.enProceso,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = formulario.stock,
            onValueChange = onStockChange,
            label = { Text("Stock") },
            placeholder = { Text("Ej. 100") },
            isError = formulario.campoConError == CampoProducto.STOCK,
            supportingText = formulario.mensajePara(CampoProducto.STOCK),
            singleLine = true,
            enabled = !formulario.enProceso,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = onRegistrar,
            enabled = !formulario.enProceso,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (formulario.enProceso) "Registrando…" else "Registrar")
        }

        formulario.mensaje?.let { mensaje ->
            Text(
                text = mensaje,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (formulario.registroExitoso) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        formulario.ultimoProductoRegistrado?.let { producto ->
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

private fun ProductoFormularioState.mensajePara(
    campo: CampoProducto,
): (@Composable () -> Unit)? = if (campoConError == campo && mensaje != null) {
    { Text(mensaje) }
} else {
    null
}

@Composable
private fun EstadoCargando() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                text = "Cargando productos…",
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MensajeSinProductos(
    mensaje: String = "Todavía no hay productos registrados.",
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = mensaje,
            modifier = Modifier.padding(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EstadoError(
    mensaje: String,
    onReintentar: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = mensaje, textAlign = TextAlign.Center)
            Button(onClick = onReintentar) {
                Text("Reintentar")
            }
        }
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
        producto.requiereReposicion() -> "Bajo stock" to MaterialTheme.colorScheme.error
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
