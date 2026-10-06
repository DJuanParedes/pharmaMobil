package pe.edu.upeu.pharmamobil.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle

@OptIn(ExperimentalForeignApi::class)
actual fun formatearSoles(valor: Double): String {
    val formateador = NSNumberFormatter()
    formateador.numberStyle = NSNumberFormatterCurrencyStyle
    formateador.locale = NSLocale("es_PE")
    return formateador.stringFromNumber(NSNumber(double = valor)) ?: "S/ $valor"
}
