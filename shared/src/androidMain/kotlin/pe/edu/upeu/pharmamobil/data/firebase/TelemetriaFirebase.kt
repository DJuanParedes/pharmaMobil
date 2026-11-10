package pe.edu.upeu.pharmamobil.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics
import pe.edu.upeu.pharmamobil.domain.port.Telemetria

/**
 * Única clase que conoce el SDK de Firebase (GitLive). Solo registra contexto técnico
 * —pantalla, operación, código— y nunca datos personales ni el token de sesión.
 * Si el proyecto aún no tiene google-services.json, Firebase no está inicializado y
 * las llamadas se ignoran en lugar de cerrar la aplicación.
 */
class TelemetriaFirebase : Telemetria {
    override fun registrarError(e: Throwable, contexto: Map<String, String>) {
        runCatching {
            val crashlytics = Firebase.crashlytics
            contexto.forEach { (clave, valor) -> crashlytics.setCustomKey(clave, valor) }
            crashlytics.recordException(e)
        }
    }

    override fun registrarEvento(nombre: String, datos: Map<String, String>) {
        runCatching {
            val detalle = datos.entries.joinToString(", ") { "${it.key}=${it.value}" }
            Firebase.crashlytics.log(if (detalle.isEmpty()) nombre else "$nombre: $detalle")
        }
    }
}
