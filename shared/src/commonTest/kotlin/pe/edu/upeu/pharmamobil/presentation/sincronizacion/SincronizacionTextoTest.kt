package pe.edu.upeu.pharmamobil.presentation.sincronizacion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SincronizacionTextoTest {
    @Test
    fun elTituloDiceConexionYCambiosPorEnviar() {
        assertEquals("Sin conexión · 3 cambios por enviar",
            SincronizacionUiState(pendientes = 3, sinConexion = true).titulo())
        assertEquals("En línea · todo enviado", SincronizacionUiState().titulo())
        assertEquals("Sincronizando… · 1 cambio por enviar",
            SincronizacionUiState(enCurso = true, pendientes = 1).titulo())
    }

    @Test
    fun laAntiguedadSeExpresaEnPalabras() {
        val ahora = 10L * 24 * 60 * 60 * 1000
        assertEquals("Aún no se sincroniza con el servidor", antiguedad(null, ahora))
        assertEquals("Actualizado hace un momento", antiguedad(ahora - 20_000, ahora))
        assertEquals("Actualizado hace 15 min", antiguedad(ahora - 15 * 60_000, ahora))
        assertEquals("Actualizado hace 2 h", antiguedad(ahora - 2 * 60 * 60_000, ahora))
        assertEquals("Actualizado hace 3 d", antiguedad(ahora - 3L * 24 * 60 * 60_000, ahora))
    }

    @Test
    fun conFilasFallidasElMensajeOfreceUnaAccion() {
        assertNull(SincronizacionUiState().avisoFallidas())
        assertEquals("2 productos no se pudieron enviar. Revísalos o reinténtalos.",
            SincronizacionUiState(fallidas = 2).avisoFallidas())
    }
}
