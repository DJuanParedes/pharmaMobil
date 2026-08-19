package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.domain.model.Pedido
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.result.ResultadoProcesamientoPedido
import pe.edu.upeu.pharmamobil.domain.service.PedidoService

class InventarioRepositoryMemoria(
    productosIniciales: List<Producto>,
    private val latenciaMillis: Long = 500,
    private val pedidoService: PedidoService = PedidoService()
) : InventarioRepository {
    private val mutex = Mutex()
    private val inventario = MutableStateFlow(productosIniciales.toList())

    init {
        require(latenciaMillis >= 0) { "La latencia no puede ser negativa" }
        require(productosIniciales.map { it.id }.distinct().size == productosIniciales.size) {
            "El inventario no puede contener identificadores repetidos"
        }
    }

    override fun observarInventario(): Flow<List<Producto>> = inventario.asStateFlow()

    override suspend fun procesarPedido(pedido: Pedido): ResultadoProcesamientoPedido =
        mutex.withLock {
            delay(latenciaMillis)

            val resultado = pedidoService.procesarPedido(
                pedido = pedido,
                inventario = inventario.value
            )

            if (resultado is ResultadoProcesamientoPedido.Exito) {
                inventario.value = resultado.inventarioActualizado
            }

            resultado
        }
}
