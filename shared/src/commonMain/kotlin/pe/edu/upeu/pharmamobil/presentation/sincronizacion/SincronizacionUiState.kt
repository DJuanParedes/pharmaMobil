package pe.edu.upeu.pharmamobil.presentation.sincronizacion

data class SincronizacionUiState(
    val enCurso: Boolean = false,
    val pendientes: Int = 0,
    val fallidas: Int = 0,
    val ultimaSync: Long? = null, // epoch en milisegundos
    val sinConexion: Boolean = false,
    val detalleFallidas: List<String> = emptyList(),
)

/** «Sin conexión · 3 cambios por enviar» dice más que un icono rojo. */
fun SincronizacionUiState.titulo(): String {
    val conexion = when {
        enCurso -> "Sincronizando…"
        sinConexion -> "Sin conexión"
        else -> "En línea"
    }
    val cambios = when (pendientes) {
        0 -> "todo enviado"
        1 -> "1 cambio por enviar"
        else -> "$pendientes cambios por enviar"
    }
    return "$conexion · $cambios"
}

/** «Actualizado hace 2 h» dice más que una fecha completa. */
fun antiguedad(ultimaSync: Long?, ahora: Long): String {
    if (ultimaSync == null) return "Aún no se sincroniza con el servidor"
    val minutos = ((ahora - ultimaSync).coerceAtLeast(0L)) / 60_000L
    return when {
        minutos < 1 -> "Actualizado hace un momento"
        minutos < 60 -> "Actualizado hace $minutos min"
        minutos < 60 * 24 -> "Actualizado hace ${minutos / 60} h"
        else -> "Actualizado hace ${minutos / (60 * 24)} d"
    }
}

fun SincronizacionUiState.avisoFallidas(): String? = when (fallidas) {
    0 -> null
    1 -> "1 producto no se pudo enviar. Revísalo o reinténtalo."
    else -> "$fallidas productos no se pudieron enviar. Revísalos o reinténtalos."
}
