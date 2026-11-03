package pe.edu.upeu.pharmamobil.permiso

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred

/**
 * Conecta la corrutina con el lanzador de la Activity. Se registra en onCreate y convierte
 * la respuesta del diálogo en el valor que la función de suspensión estaba esperando.
 */
class PuentePermisos(val actividad: ComponentActivity) {
    private var pendiente: CompletableDeferred<Boolean>? = null
    private val lanzador = actividad.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> pendiente?.complete(concedido) }

    suspend fun pedir(nombre: String): Boolean {
        val espera = CompletableDeferred<Boolean>()
        pendiente = espera
        lanzador.launch(nombre)
        return espera.await()
    }
}
