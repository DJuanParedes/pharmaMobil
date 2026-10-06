package pe.edu.upeu.pharmamobil.navigation

sealed class Screen(
    val id: String,
    val titulo: String,
    val abreviatura: String,
) {
    data object Inicio : Screen("inicio", "Inicio", "I")
    data object Productos : Screen("productos", "Productos", "P")
    data object Clientes : Screen("clientes", "Clientes", "C")
    data object Pedidos : Screen("pedidos", "Pedidos", "O")
    data object AcercaDe : Screen("acerca", "Acerca de", "A")

    companion object {
        val destinos: List<Screen>
            get() = listOf(Inicio, Productos, Clientes, Pedidos, AcercaDe)
    }
}
