package pe.edu.upeu.pharmamobil.presentation.acerca

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobil.platform.InfoDispositivo

@Composable
fun AcercaDeScreen() {
    val dispositivo = remember { InfoDispositivo() }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("PharmaMobil", style = MaterialTheme.typography.headlineLarge)
        Text("Información del dispositivo", style = MaterialTheme.typography.titleLarge)
        Text("Sistema operativo: ${dispositivo.sistema}", style = MaterialTheme.typography.bodyLarge)
        Text("Versión: ${dispositivo.version}", style = MaterialTheme.typography.bodyLarge)
        Text("Aplicación de gestión farmacéutica para Android e iOS.", style = MaterialTheme.typography.bodyMedium)
    }
}
