package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Pedido
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.result.ResultadoProcesamientoPedido

interface InventarioRepository {
    fun observarInventario(): Flow<List<Producto>>

    suspend fun procesarPedido(pedido: Pedido): ResultadoProcesamientoPedido
}
