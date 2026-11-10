package pe.edu.upeu.pharmamobil.permiso

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import pe.edu.upeu.pharmamobil.domain.port.Notificador

/** Usa el permiso de notificaciones: si no está concedido, no hace nada. */
class NotificadorAndroid(private val contexto: Context) : Notificador {
    override fun avisarFilasFallidas(cantidad: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            contexto.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val gestor = contexto.getSystemService(NotificationManager::class.java) ?: return
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            gestor.createNotificationChannel(
                NotificationChannel(CANAL, "Sincronización", NotificationManager.IMPORTANCE_DEFAULT)
            )
            Notification.Builder(contexto, CANAL)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(contexto)
        }
        val texto = if (cantidad == 1) "1 producto no se pudo enviar al servidor."
        else "$cantidad productos no se pudieron enviar al servidor."
        gestor.notify(ID, builder
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle("PharmaMobil")
            .setContentText("$texto Ábrelo para revisarlos.")
            .setAutoCancel(true)
            .build())
    }

    private companion object {
        const val CANAL = "sincronizacion"
        const val ID = 1301
    }
}
