package pe.edu.upeu.pharmamobil.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenTest {
    @Test
    fun conservaLosDestinosPreviosYAgregaAcercaDe() {
        assertEquals(
            listOf("Inicio", "Productos", "Clientes", "Pedidos", "Acerca de"),
            Screen.destinos.map { it.titulo },
        )
    }

    @Test
    fun cadaDestinoTieneUnIdentificadorUnico() {
        val ids = Screen.destinos.map { it.id }

        assertEquals(ids.size, ids.distinct().size)
    }
}
