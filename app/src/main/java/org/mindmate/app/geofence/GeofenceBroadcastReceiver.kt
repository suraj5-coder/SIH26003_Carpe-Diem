package org.mindmate.app.geofence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mindmate.app.R
import org.mindmate.app.data.local.AlertEntity
import org.mindmate.app.data.local.MindMateDatabase

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError() || event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT) return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = MindMateDatabase.get(context).dao()
                val zoneId = event.triggeringGeofences?.firstOrNull()?.requestId?.toLongOrNull()
                val zone = dao.enabledGeofences().firstOrNull { it.id == zoneId } ?: return@launch
                val location = event.triggeringLocation
                val message = "Patient has left the designated safe area. Last location recorded locally."
                dao.insertAlert(
                    AlertEntity(
                        patientId = zone.patientId,
                        type = "GEOFENCE_EXIT",
                        title = "Safety Alert",
                        message = message,
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                    ),
                )
                showAlert(context, message)
            } finally {
                result.finish()
            }
        }
    }

    private fun showAlert(context: Context, message: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.notification_channel_alerts), NotificationManager.IMPORTANCE_HIGH),
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Safety Alert")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Please return to your safe area. $message"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        if (android.os.Build.VERSION.SDK_INT < 33 || androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(7100, notification)
        }
    }

    companion object { private const val CHANNEL = "mindmate_safety" }
}
