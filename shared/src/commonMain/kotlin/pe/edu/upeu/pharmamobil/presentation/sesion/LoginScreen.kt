package pe.edu.upeu.pharmamobil.presentation.sesion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.navigation.Screen

val LocalSesion = staticCompositionLocalOf<Sesion?> { null }
val LocalCerrarSesion = staticCompositionLocalOf<() -> Unit> { {} }
fun permitido(screen: Screen, sesion: Sesion?): Boolean = when (screen) {
    Screen.Productos -> sesion?.puede("CATALOGO_LEER") == true
    Screen.Clientes -> sesion?.puede("CLIENTES_LEER") == true
    Screen.Pedidos -> sesion?.puede("VENTAS_LEER") == true
    else -> true
}
@Composable
fun LoginScreen(state: SesionUiState, onUsuario: (String) -> Unit,
    onClave: (String) -> Unit, onEntrar: () -> Unit, onRestaurar: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("PharmaMobil", style = MaterialTheme.typography.headlineLarge)
            Text("Inicia sesión para gestionar tu farmacia")
            OutlinedTextField(value = state.usuario, onValueChange = onUsuario,
                label = { Text("Usuario") }, singleLine = true,
                enabled = !state.cargando, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.clave, onValueChange = onClave,
                label = { Text("Contraseña") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), enabled = !state.cargando,
                modifier = Modifier.fillMaxWidth())
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = onEntrar, enabled = !state.cargando && state.usuario.isNotBlank() && state.clave.isNotBlank(),
                modifier = Modifier.fillMaxWidth()) {
                Text(if (state.cargando) "Ingresando..." else "Iniciar sesión")
            }
            if (state.error != null) TextButton(onClick = onRestaurar) { Text("Reintentar sesión guardada") }
        }
    }
}
