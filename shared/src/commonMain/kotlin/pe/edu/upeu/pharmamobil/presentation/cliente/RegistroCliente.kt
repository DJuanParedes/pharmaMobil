package pe.edu.upeu.pharmamobil.presentation.cliente

import pe.edu.upeu.pharmamobil.domain.model.Cliente

val clientesSimulados = listOf(
    Cliente(
        id = 1L,
        nombre = "Ana Torres",
        correo = "ana.torres@correo.com",
        telefono = "987654321",
    ),
    Cliente(
        id = 2L,
        nombre = "Luis Mendoza",
        correo = "luis.mendoza@correo.com",
        telefono = null,
    ),
)

internal enum class CampoCliente {
    NOMBRE,
    CORREO,
    TELEFONO,
}

internal sealed interface ResultadoRegistroCliente {
    data class Exito(val cliente: Cliente) : ResultadoRegistroCliente

    data class Error(
        val campo: CampoCliente,
        val mensaje: String,
    ) : ResultadoRegistroCliente
}

internal fun validarClienteRegistro(
    nombre: String,
    correo: String,
    telefono: String,
): ResultadoRegistroCliente {
    val correoLimpio = correo.trim()
    val telefonoLimpio = telefono.trim()

    return when {
        nombre.isBlank() -> ResultadoRegistroCliente.Error(
            CampoCliente.NOMBRE,
            "El nombre es obligatorio.",
        )

        correoLimpio.isBlank() || '@' !in correoLimpio || correoLimpio.substringAfter('@').isBlank() -> {
            ResultadoRegistroCliente.Error(
                CampoCliente.CORREO,
                "Ingresa un correo válido.",
            )
        }

        telefonoLimpio.isNotEmpty() &&
            (telefonoLimpio.length !in 7..15 || telefonoLimpio.any { !it.isDigit() }) -> {
            ResultadoRegistroCliente.Error(
                CampoCliente.TELEFONO,
                "El teléfono debe contener entre 7 y 15 dígitos.",
            )
        }

        else -> ResultadoRegistroCliente.Exito(
            Cliente(
                id = 1L,
                nombre = nombre.trim(),
                correo = correoLimpio,
                telefono = telefonoLimpio.ifBlank { null },
            )
        )
    }
}

internal fun agregarClienteAlRegistro(
    clientes: List<Cliente>,
    cliente: Cliente,
): List<Cliente> {
    val siguienteId = (clientes.maxOfOrNull { it.id } ?: 0L) + 1L
    return clientes + cliente.copy(id = siguienteId)
}
