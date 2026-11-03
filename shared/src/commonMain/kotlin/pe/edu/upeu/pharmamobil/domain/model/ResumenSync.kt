package pe.edu.upeu.pharmamobil.domain.model

import pe.edu.upeu.pharmamobil.domain.error.ErrorApi

/** Lo que pasó en una ronda de sincronización. */
data class ResumenSync(
    val enviadas: Int,
    val fallidas: Int,
    val pendientes: Int,
    /** Error al bajar la lista del servidor; null si la descarga terminó bien. */
    val errorDescarga: ErrorApi? = null,
) {
    val completa: Boolean get() = errorDescarga == null
}
