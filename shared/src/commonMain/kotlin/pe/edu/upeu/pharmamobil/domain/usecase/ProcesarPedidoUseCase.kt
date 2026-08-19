package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.data.repository.InventarioRepository
import pe.edu.upeu.pharmamobil.domain.model.Pedido
import pe.edu.upeu.pharmamobil.domain.result.ResultadoProcesamientoPedido

class ProcesarPedidoUseCase(
    private val repository: InventarioRepository
) {
    suspend operator fun invoke(pedido: Pedido): ResultadoProcesamientoPedido =
        repository.procesarPedido(pedido)
}
