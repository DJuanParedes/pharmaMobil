package pe.edu.upeu.pharmamobil.platform

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Hora actual en milisegundos desde epoch, igual en Android e iOS. */
@OptIn(ExperimentalTime::class)
fun ahoraMillis(): Long = Clock.System.now().toEpochMilliseconds()
