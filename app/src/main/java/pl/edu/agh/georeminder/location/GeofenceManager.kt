package pl.edu.agh.georeminder.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import pl.edu.agh.georeminder.model.Task

class GeofenceManager(private val context: Context) {

    private val geofencingClient by lazy { LocationServices.getGeofencingClient(context) }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    fun addGeofenceForTask(task: Task) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Fine location permission not granted. Cannot add geofence.")
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(task.id.toString())
            .setCircularRegion(
                task.latitude,
                task.longitude,
                task.radius
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d(TAG, "Geofence added for task: ${task.title}, latitude: ${task.latitude}, longitude: ${task.longitude}")
            }
            addOnFailureListener {
                Log.e(TAG, "Failed to add geofence for task: ${task.title}", it)
            }
        }
    }

    fun removeGeofenceForTask(task: Task) {
        geofencingClient.removeGeofences(listOf(task.id.toString())).run {
            addOnSuccessListener {
                Log.d(TAG, "Geofence removed for task: ${task.title}")
            }
            addOnFailureListener {
                Log.e(TAG, "Failed to remove geofence for task: ${task.title}", it)
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceManager"
    }
}