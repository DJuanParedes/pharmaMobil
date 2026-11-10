package pe.edu.upeu.pharmamobil.presentation.permiso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso

/** Lo que la interfaz debe ofrecer según el estado: explicar y pedir, o ir a Ajustes. */
enum class AccionPermiso { NINGUNA, EXPLICAR_Y_PEDIR, ABRIR_AJUSTES }

fun accionPara(estado: EstadoPermiso?): AccionPermiso = when (estado) {
    EstadoPermiso.Concedido -> AccionPermiso.NINGUNA
    EstadoPermiso.Denegado, null -> AccionPermiso.EXPLICAR_Y_PEDIR
    EstadoPermiso.DenegadoParaSiempre -> AccionPermiso.ABRIR_AJUSTES
}

fun textoPermiso(estado: EstadoPermiso?): String = when (estado) {
    EstadoPermiso.Concedido ->
        "Notificaciones activadas: te avisaremos si un producto no se pudo enviar al servidor."
    EstadoPermiso.Denegado, null ->
        "PharmaMobil puede avisarte cuando un producto guardado sin conexión no logra enviarse " +
            "al servidor, para que no se pierda. Solo se usa para eso."
    EstadoPermiso.DenegadoParaSiempre ->
        "Las notificaciones están bloqueadas. Si quieres recibir el aviso, actívalas en Ajustes."
}

/** Estado del permiso para la tarjeta; se prueba con un GestorPermisos falso. */
class PermisoNotificacionesModelo(private val gestor: GestorPermisos) {
    var estado: EstadoPermiso? = null
        private set

    suspend fun cargar(): EstadoPermiso = gestor.estado(Permiso.NOTIFICACIONES).also { estado = it }

    suspend fun pedir(): EstadoPermiso = gestor.solicitar(Permiso.NOTIFICACIONES).also { estado = it }

    fun abrirAjustes() = gestor.abrirAjustes()
}

@Composable
fun PermisoNotificacionesCard(modifier: Modifier = Modifier) {
    val gestor = koinInject<GestorPermisos>()
    val modelo = remember(gestor) { PermisoNotificacionesModelo(gestor) }
    val scope = rememberCoroutineScope()
    var estado by remember { mutableStateOf<EstadoPermiso?>(null) }
    // Se vuelve a leer al regresar de Ajustes.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { estado = modelo.cargar() } }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Avisos de sincronización", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(textoPermiso(estado), style = MaterialTheme.typography.bodyMedium)
            when (accionPara(estado)) {
                AccionPermiso.NINGUNA -> Unit
                AccionPermiso.EXPLICAR_Y_PEDIR -> Button(onClick = { scope.launch { estado = modelo.pedir() } }) {
                    Text("Permitir notificaciones")
                }
                AccionPermiso.ABRIR_AJUSTES -> OutlinedButton(onClick = modelo::abrirAjustes) { Text("Abrir Ajustes") }
            }
        }
    }
}
