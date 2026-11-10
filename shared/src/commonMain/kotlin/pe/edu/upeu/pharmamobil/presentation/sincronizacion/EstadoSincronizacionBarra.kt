package pe.edu.upeu.pharmamobil.presentation.sincronizacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EstadoSincronizacionBarra(
    estado: SincronizacionUiState,
    ahora: Long,
    onSincronizar: () -> Unit,
    onReintentarFallidas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var verDetalle by remember { mutableStateOf(false) }
    val colores = if (estado.sinConexion || estado.fallidas > 0)
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    Card(modifier = modifier.fillMaxWidth(), colors = colores) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(estado.titulo(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(antiguedad(estado.ultimaSync, ahora), style = MaterialTheme.typography.bodyMedium)
            estado.avisoFallidas()?.let { aviso ->
                Text(aviso, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                if (verDetalle) estado.detalleFallidas.forEach {
                    Text("• $it", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSincronizar, enabled = !estado.enCurso) {
                    Text(if (estado.enCurso) "Sincronizando…" else "Sincronizar ahora")
                }
                if (estado.fallidas > 0) {
                    TextButton(onClick = onReintentarFallidas, enabled = !estado.enCurso) { Text("Reintentar") }
                    TextButton(onClick = { verDetalle = !verDetalle }) {
                        Text(if (verDetalle) "Ocultar" else "Revisar")
                    }
                }
            }
        }
    }
}
