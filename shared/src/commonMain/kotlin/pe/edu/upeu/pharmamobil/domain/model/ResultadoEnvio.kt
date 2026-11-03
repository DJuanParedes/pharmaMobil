package pe.edu.upeu.pharmamobil.domain.model

/** Resultado de enviar una fila pendiente al servidor (sesión 13). */
sealed interface ResultadoEnvio {
    data class Exito(val producto: Producto) : ResultadoEnvio
    /** Sin red, tiempo agotado o servicio no disponible: se reintenta con espera creciente. */
    data class Transitorio(val motivo: String) : ResultadoEnvio
    /** Validación, recurso inexistente, conflicto o falta de privilegio: no se reintenta solo. */
    data class Permanente(val motivo: String) : ResultadoEnvio
}
