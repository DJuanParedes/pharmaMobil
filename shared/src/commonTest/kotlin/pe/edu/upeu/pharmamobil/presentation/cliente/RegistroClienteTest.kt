package pe.edu.upeu.pharmamobil.presentation.cliente

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RegistroClienteTest {
    @Test
    fun `los clientes simulados se muestran desde el inicio`() {
        assertEquals(2, clientesSimulados.size)
        assertEquals("Ana Torres", clientesSimulados.first().nombre)
    }

    @Test
    fun `un registro valido crea un cliente`() {
        val resultado = validarClienteRegistro(
            nombre = "María López",
            correo = "maria@correo.com",
            telefono = "999888777",
        )

        val exito = assertIs<ResultadoRegistroCliente.Exito>(resultado)
        assertEquals("María López", exito.cliente.nombre)
        assertEquals("999888777", exito.cliente.telefono)
    }

    @Test
    fun `el correo invalido se rechaza`() {
        val resultado = validarClienteRegistro("María", "correo-invalido", "")

        val error = assertIs<ResultadoRegistroCliente.Error>(resultado)
        assertEquals(CampoCliente.CORREO, error.campo)
    }

    @Test
    fun `el nuevo cliente recibe un id consecutivo`() {
        val nuevo = agregarClienteAlRegistro(
            clientes = clientesSimulados,
            cliente = assertIs<ResultadoRegistroCliente.Exito>(
                validarClienteRegistro("José Pérez", "jose@correo.com", "")
            ).cliente,
        ).last()

        assertEquals(3L, nuevo.id)
        assertEquals(null, nuevo.telefono)
    }
}
