package pe.edu.upeu.pharmamobil.presentation.pedido

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import pe.edu.upeu.pharmamobil.presentation.cliente.clientesSimulados
import pe.edu.upeu.pharmamobil.presentation.producto.productosSimulados

class RegistroPedidoTest {
    @Test
    fun `se carga un pedido de ejemplo`() {
        val pedidos = pedidosSimulados(clientesSimulados, productosSimulados)

        assertEquals(1, pedidos.size)
        assertEquals("Ana Torres", pedidos.first().cliente.nombre)
        assertEquals(31.0, pedidos.first().total())
    }

    @Test
    fun `un pedido valido conserva cliente producto y cantidad`() {
        val resultado = validarPedidoRegistro(
            cliente = clientesSimulados.first(),
            producto = productosSimulados.first(),
            cantidad = "3",
        )

        val exito = assertIs<ResultadoRegistroPedido.Exito>(resultado)
        assertEquals(3, exito.pedido.detalles.single().cantidad)
        assertEquals("Paracetamol", exito.pedido.detalles.single().producto.nombre)
    }

    @Test
    fun `no se permite superar el stock disponible`() {
        val resultado = validarPedidoRegistro(
            cliente = clientesSimulados.first(),
            producto = productosSimulados.first { it.stock == 3 },
            cantidad = "4",
        )

        val error = assertIs<ResultadoRegistroPedido.Error>(resultado)
        assertEquals(CampoPedido.CANTIDAD, error.campo)
    }

    @Test
    fun `el nuevo pedido recibe un id consecutivo`() {
        val actuales = pedidosSimulados(clientesSimulados, productosSimulados)
        val pedido = assertIs<ResultadoRegistroPedido.Exito>(
            validarPedidoRegistro(
                cliente = clientesSimulados.last(),
                producto = productosSimulados.first(),
                cantidad = "1",
            )
        ).pedido

        assertEquals(2L, agregarPedidoAlRegistro(actuales, pedido).last().id)
    }
}
