package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio
import pe.edu.upeu.pharmamobil.domain.model.ResumenSync
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import pe.edu.upeu.pharmamobil.domain.port.NotificadorNulo
import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.domain.port.TelemetriaNula
import pe.edu.upeu.pharmamobil.domain.repository.ProductoEnvioRepository
import pe.edu.upeu.pharmamobil.domain.repository.ProductoLocalRepository

/**
 * Sube lo pendiente con intentos por fila y espera creciente, y después baja lo actualizado.
 * No conoce códigos HTTP: la capa de datos ya tradujo cada error a Transitorio o Permanente.
 */
class SincronizarUseCase(
    private val local: ProductoLocalRepository,
    private val remoto: ProductoEnvioRepository,
    private val telemetria: Telemetria = TelemetriaNula,
    private val notificador: Notificador = NotificadorNulo,
    private val reloj: () -> Long = { 0L },
    private val esperar: suspend (Long) -> Unit = { delay(it) },
) {
    suspend operator fun invoke(): ResumenSync {
        var enviadas = 0
        var fallidas = 0
        for (fila in local.pendientes(MAX_INTENTOS)) {
            when (val r = remoto.enviar(fila)) {
                is ResultadoEnvio.Exito -> {
                    local.confirmar(fila.id, r.producto); enviadas++
                }
                is ResultadoEnvio.Permanente -> {
                    local.marcarFallida(fila.id, r.motivo, MAX_INTENTOS); fallidas++
                    telemetria.registrarError(
                        IllegalStateException("Envío rechazado"),
                        mapOf("operacion" to "sincronizar", "tipo" to "permanente", "motivo" to r.motivo),
                    )
                }
                is ResultadoEnvio.Transitorio -> {
                    local.sumarIntento(fila.id, r.motivo)
                    if (fila.intentos + 1 >= MAX_INTENTOS) fallidas++
                    esperar(espera(fila.intentos))
                    // La red o el servicio no están disponibles: el resto espera a la siguiente ronda.
                    break
                }
            }
        }
        if (fallidas > 0) notificador.avisarFilasFallidas(fallidas)
        // Solo después de subir se baja: invertirlo borraría lo pendiente.
        val productos = try {
            remoto.listar()
        } catch (e: ErrorApiException) {
            return ResumenSync(enviadas, fallidas, local.contarPendientes(MAX_INTENTOS), e.error)
        }
        local.conciliar(productos)
        local.registrarSincronizacion(reloj())
        return ResumenSync(enviadas, fallidas, local.contarPendientes(MAX_INTENTOS))
    }

    companion object {
        const val MAX_INTENTOS = 5

        /** 1 s, 2 s, 4 s, 8 s y 16 s como máximo. */
        fun espera(intentos: Int): Long = 1_000L shl intentos.coerceIn(0, 4)
    }
}
