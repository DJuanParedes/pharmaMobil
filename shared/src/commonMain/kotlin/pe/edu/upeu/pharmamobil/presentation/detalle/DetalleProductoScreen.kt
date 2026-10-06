package pe.edu.upeu.pharmamobil.presentation.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DetalleProductoScreen(
    uiState: DetalleProductoUiState,
    onCompartir: () -> Unit,
    onVolver: () -> Unit,
    onReintentar: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Detalle del producto", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        when (uiState) {
            DetalleProductoUiState.Cargando -> {
                CircularProgressIndicator()
                Text("Consultando producto...")
            }
            is DetalleProductoUiState.Error -> {
                Text(uiState.mensaje, color = MaterialTheme.colorScheme.error)
                Button(onClick = onReintentar) { Text("Reintentar") }
            }
            is DetalleProductoUiState.ConProducto -> {
                val producto = uiState.productoUi
                Text(producto.nombre, style = MaterialTheme.typography.headlineSmall)
                Text(producto.precio, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text("Stock: ${producto.stock}", style = MaterialTheme.typography.titleMedium)
                Text(if (producto.activo) "Producto activo" else "Producto inactivo")
                uiState.errorCompartir?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = onCompartir, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir")
                }
            }
        }
        OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver al listado") }
    }
}
