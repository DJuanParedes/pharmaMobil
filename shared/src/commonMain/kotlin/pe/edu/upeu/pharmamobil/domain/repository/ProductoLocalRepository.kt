package pe.edu.upeu.pharmamobil.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.FilaPendiente
import pe.edu.upeu.pharmamobil.domain.model.Producto

/** Lo que la sincronización necesita de la base local; la implementa la capa de datos. */
interface ProductoLocalRepository {
    fun pendientes(maxIntentos: Int): List<FilaPendiente>
    fun fallidas(maxIntentos: Int): List<FilaPendiente>
    fun confirmar(idLocal: Long, remoto: Producto)
    fun sumarIntento(id: Long, motivo: String)
    fun marcarFallida(id: Long, motivo: String, maxIntentos: Int)
    fun reintentarFallidas(maxIntentos: Int)
    fun conciliar(productos: List<Producto>)
    fun contarPendientes(maxIntentos: Int): Int
    fun observarPendientes(maxIntentos: Int): Flow<Int>
    fun observarFallidas(maxIntentos: Int): Flow<Int>
    fun registrarSincronizacion(epochMillis: Long)
    fun observarUltimaSincronizacion(): Flow<Long?>
}

/** Lo que la sincronización necesita del servidor; la implementa la capa de datos. */
interface ProductoEnvioRepository {
    suspend fun enviar(fila: FilaPendiente): pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio
    /** Lanza ErrorApiException si la descarga falla. */
    suspend fun listar(): List<Producto>
}
