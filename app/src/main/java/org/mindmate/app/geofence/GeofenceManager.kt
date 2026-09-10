package org.mindmate.app.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import org.mindmate.app.data.local.GeofenceEntity

sealed interface GeofenceRegistrationResult {
    data object Registered : GeofenceRegistrationResult
    data class PermissionRequired(val permission: String) : GeofenceRegistrationResult
    data class Failed(val message: String) : GeofenceRegistrationResult
}

class GeofenceManager(private val context: Context) {
    private val client = LocationServices.getGeofencingClient(context)
    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            7001,
            Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    fun register(zone: GeofenceEntity, callback: (GeofenceRegistrationResult) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback(GeofenceRegistrationResult.PermissionRequired(Manifest.permission.ACCESS_FINE_LOCATION))
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            callback(GeofenceRegistrationResult.PermissionRequired(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            return
        }
        val request = runCatching {
            val geofence = Geofence.Builder()
                .setRequestId(zone.id.toString())
                .setCircularRegion(zone.latitude, zone.longitude, zone.radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_ENTER)
                .setNotificationResponsiveness(60_000)
                .build()
            GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()
        }.getOrElse {
            callback(GeofenceRegistrationResult.Failed(it.message ?: "Invalid safe area"))
            return
        }
        @Suppress("MissingPermission")
        client.removeGeofences(listOf(zone.id.toString())).addOnCompleteListener {
            client.addGeofences(request, pendingIntent)
                .addOnSuccessListener { callback(GeofenceRegistrationResult.Registered) }
                .addOnFailureListener { callback(GeofenceRegistrationResult.Failed(it.message ?: "Could not register safe area")) }
        }
    }

    fun unregister() {
        client.removeGeofences(pendingIntent)
    }
}
