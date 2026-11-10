package pe.edu.upeu.pharmamobil.domain.port

/**
 * Registro de errores y eventos técnicos. El dominio solo conoce esta interfaz;
 * el proveedor concreto (Firebase Crashlytics en Android) vive en la capa de datos.
 * Nunca se envían datos personales ni el token de sesión.
 */
interface Telemetria {
    fun registrarError(e: Throwable, contexto: Map<String, String> = emptyMap())
    fun registrarEvento(nombre: String, datos: Map<String, String> = emptyMap())
}

/** Implementación vacía para iOS (pendiente) y para pruebas. */
object TelemetriaNula : Telemetria {
    override fun registrarError(e: Throwable, contexto: Map<String, String>) = Unit
    override fun registrarEvento(nombre: String, datos: Map<String, String>) = Unit
}
