package pe.edu.upeu.pharmamobil.presentation.sincronizacion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.pharmamobil.platform.ahoraMillis

/** Barra lista para usar: toma el ViewModel de Koin y refresca la antigüedad cada minuto. */
@Composable
fun EstadoSincronizacion() {
    val vm = koinViewModel<SincronizacionViewModel>()
    val estado by vm.estado.collectAsStateWithLifecycle()
    var ahora by remember { mutableLongStateOf(ahoraMillis()) }
    LaunchedEffect(Unit) {
        while (true) { ahora = ahoraMillis(); delay(60_000) }
    }
    EstadoSincronizacionBarra(
        estado = estado,
        ahora = maxOf(ahora, estado.ultimaSync ?: 0L),
        onSincronizar = vm::sincronizar,
        onReintentarFallidas = vm::reintentarFallidas,
    )
}
