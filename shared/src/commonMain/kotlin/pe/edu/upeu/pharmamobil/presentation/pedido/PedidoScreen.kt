package pe.edu.upeu.pharmamobil.presentation.pedido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import kotlin.math.roundToInt
import pe.edu.upeu.pharmamobil.domain.model.Cliente
import pe.edu.upeu.pharmamobil.domain.model.EstadoPedido
import pe.edu.upeu.pharmamobil.domain.model.Pedido
import pe.edu.upeu.pharmamobil.domain.model.Producto

@Composable
fun PedidoScreen(
    clientes: List<Cliente>,
    productos: List<Producto>,
    pedidos: List<Pedido>,
    onPedidosChange: (List<Pedido>) -> Unit,
) {
    val productosDisponibles = productos.filter { it.activo && it.stock > 0 }
    var clienteId by rememberSaveable { mutableStateOf(clientes.firstOrNull()?.id) }
    var productoId by rememberSaveable { mutableStateOf(productosDisponibles.firstOrNull()?.id) }
    var cantidad by rememberSaveable { mutableStateOf("") }
    var menuClientesAbierto by rememberSaveable { mutableStateOf(false) }
    var menuProductosAbierto by rememberSaveable { mutableStateOf(false) }
    var mensaje by rememberSaveable { mutableStateOf("") }
    var registroExitoso by rememberSaveable { mutableStateOf(false) }
    var intentoRegistrar by rememberSaveable { mutableStateOf(false) }

    val clienteSeleccionado = clientes.firstOrNull { it.id == clienteId }
    val productoSeleccionado = productosDisponibles.firstOrNull { it.id == productoId }
    val errorActual = if (intentoRegistrar) {
        validarPedidoRegistro(clienteSeleccionado, productoSeleccionado, cantidad) as? ResultadoRegistroPedido.Error
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
            text = "Gestión de Pedidos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Crea pedidos y revisa las órdenes registradas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = "Pedidos registrados: ${pedidos.size}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (pedidos.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Aún no hay pedidos registrados.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp),
                )
            }
        } else {
            pedidos.forEach { pedido -> PedidoCard(pedido) }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "Registrar pedido",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { menuClientesAbierto = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = clienteSeleccionado?.let { "Cliente: ${it.nombre}" }
                        ?: "Seleccionar cliente",
                )
            }
            DropdownMenu(
                expanded = menuClientesAbierto,
                onDismissRequest = { menuClientesAbierto = false },
            ) {
                clientes.forEach { cliente ->
                    DropdownMenuItem(
                        text = { Text("${cliente.id} - ${cliente.nombre}") },
                        onClick = {
                            clienteId = cliente.id
                            menuClientesAbierto = false
                            mensaje = ""
                            registroExitoso = false
                        },
                    )
                }
            }
        }
        if (errorActual?.campo == CampoPedido.CLIENTE) {
            Text(errorActual.mensaje, color = MaterialTheme.colorScheme.error)
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { menuProductosAbierto = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = productoSeleccionado?.let {
                        "Producto: ${it.nombre} (stock ${it.stock})"
                    } ?: "Seleccionar producto",
                )
            }
            DropdownMenu(
                expanded = menuProductosAbierto,
                onDismissRequest = { menuProductosAbierto = false },
            ) {
                productosDisponibles.forEach { producto ->
                    DropdownMenuItem(
                        text = { Text("${producto.nombre} - stock ${producto.stock}") },
                        onClick = {
                            productoId = producto.id
                            menuProductosAbierto = false
                            mensaje = ""
                            registroExitoso = false
                        },
                    )
                }
            }
        }
        if (errorActual?.campo == CampoPedido.PRODUCTO) {
            Text(errorActual.mensaje, color = MaterialTheme.colorScheme.error)
        }

        OutlinedTextField(
            value = cantidad,
            onValueChange = {
                cantidad = it
                mensaje = ""
                registroExitoso = false
            },
            label = { Text("Cantidad") },
            placeholder = { Text("Ej. 2") },
            isError = errorActual?.campo == CampoPedido.CANTIDAD,
            supportingText = errorActual.takeIf { it?.campo == CampoPedido.CANTIDAD }?.let {
                { Text(it.mensaje) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                intentoRegistrar = true
                when (
                    val resultado = validarPedidoRegistro(
                        clienteSeleccionado,
                        productoSeleccionado,
                        cantidad,
                    )
                ) {
                    is ResultadoRegistroPedido.Error -> {
                        mensaje = resultado.mensaje
                        registroExitoso = false
                    }

                    is ResultadoRegistroPedido.Exito -> {
                        onPedidosChange(agregarPedidoAlRegistro(pedidos, resultado.pedido))
                        cantidad = ""
                        mensaje = "Pedido registrado correctamente."
                        registroExitoso = true
                        intentoRegistrar = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Guardar pedido")
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
private fun PedidoCard(pedido: Pedido) {
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
                    text = "Pedido #${pedido.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                EstadoPedidoChip(pedido.estado)
            }
            Text(
                text = "Cliente: ${pedido.cliente.nombre}",
                style = MaterialTheme.typography.bodyMedium,
            )
            pedido.detalles.forEach { detalle ->
                Text(
                    text = "${detalle.producto.nombre} × ${detalle.cantidad}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Total: S/ ${formatearImporte(pedido.total())}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EstadoPedidoChip(estado: EstadoPedido) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = estado.descripcion(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

private fun formatearImporte(importe: Double): String {
    val centavos = (importe * 100).roundToInt()
    val parteEntera = centavos / 100
    val parteDecimal = (centavos % 100).toString().padStart(2, '0')
    return "$parteEntera.$parteDecimal"
}
