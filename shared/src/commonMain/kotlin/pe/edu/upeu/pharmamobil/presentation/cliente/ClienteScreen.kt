package pe.edu.upeu.pharmamobil.presentation.cliente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobil.domain.model.Cliente

@Composable
fun ClienteScreen(
    clientes: List<Cliente>,
    onClientesChange: (List<Cliente>) -> Unit,
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var mensaje by rememberSaveable { mutableStateOf("") }
    var registroExitoso by rememberSaveable { mutableStateOf(false) }
    var intentoRegistrar by rememberSaveable { mutableStateOf(false) }

    val errorActual = if (intentoRegistrar) {
        validarClienteRegistro(nombre, correo, telefono) as? ResultadoRegistroCliente.Error
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Gestión de Clientes",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Registra clientes y consulta la información guardada.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = "Clientes registrados: ${clientes.size}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        clientes.forEach { cliente -> ClienteCard(cliente) }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "Registrar cliente",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                mensaje = ""
                registroExitoso = false
            },
            label = { Text("Nombre completo") },
            placeholder = { Text("Ej. María López") },
            isError = errorActual?.campo == CampoCliente.NOMBRE,
            supportingText = errorActual.takeIf { it?.campo == CampoCliente.NOMBRE }?.let {
                { Text(it.mensaje) }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = correo,
            onValueChange = {
                correo = it
                mensaje = ""
                registroExitoso = false
            },
            label = { Text("Correo") },
            placeholder = { Text("nombre@correo.com") },
            isError = errorActual?.campo == CampoCliente.CORREO,
            supportingText = errorActual.takeIf { it?.campo == CampoCliente.CORREO }?.let {
                { Text(it.mensaje) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = telefono,
            onValueChange = {
                telefono = it
                mensaje = ""
                registroExitoso = false
            },
            label = { Text("Teléfono (opcional)") },
            placeholder = { Text("987654321") },
            isError = errorActual?.campo == CampoCliente.TELEFONO,
            supportingText = errorActual.takeIf { it?.campo == CampoCliente.TELEFONO }?.let {
                { Text(it.mensaje) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                intentoRegistrar = true
                when (val resultado = validarClienteRegistro(nombre, correo, telefono)) {
                    is ResultadoRegistroCliente.Error -> {
                        mensaje = resultado.mensaje
                        registroExitoso = false
                    }

                    is ResultadoRegistroCliente.Exito -> {
                        val actualizados = agregarClienteAlRegistro(clientes, resultado.cliente)
                        onClientesChange(actualizados)
                        nombre = ""
                        correo = ""
                        telefono = ""
                        mensaje = "Cliente registrado correctamente."
                        registroExitoso = true
                        intentoRegistrar = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Guardar cliente")
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
    }
}

@Composable
private fun ClienteCard(cliente: Cliente) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = cliente.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "ID ${cliente.id}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(cliente.correo, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Teléfono: ${cliente.obtenerTelefono()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
