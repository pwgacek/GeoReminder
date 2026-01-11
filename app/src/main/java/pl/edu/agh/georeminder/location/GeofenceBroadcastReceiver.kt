package pl.edu.agh.georeminder.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.edu.agh.georeminder.R
import pl.edu.agh.georeminder.model.AppDatabase
import pl.edu.agh.georeminder.model.RepeatType
import pl.edu.agh.georeminder.model.Task
import java.util.Calendar

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent?.errorCode ?: 0)
            Log.e(TAG, errorMessage)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            triggeringGeofences?.forEach { geofence ->
                val taskId = geofence.requestId.toLongOrNull() ?: return@forEach

                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.get(context)
                    val taskDao = db.taskDao()
                    val task = taskDao.getById(taskId).first()

                    if (task != null && !task.isCompleted) {
                        if (shouldTriggerTask(task)) {
                            val updatedTask = task.withIncrementedActivation(System.currentTimeMillis())
                            taskDao.update(updatedTask)
                            sendNotification(context, task.title)
                            Log.d(TAG, "Task triggered: ${task.title}, activations: ${updatedTask.currentActivations}/${task.maxActivations ?: "∞"}")
                        } else {
                            Log.d(TAG, "Task not triggered due to schedule constraints: ${task.title}")
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Determines if a task should trigger based on all its scheduling constraints
     */
    private fun shouldTriggerTask(task: Task): Boolean {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        // Check if task has a start date and we haven't reached it yet
        if (task.activeAfter != null && now < task.activeAfter) {
            Log.d(TAG, "Task ${task.title}: Not active yet (activeAfter constraint)")
            return false
        }
        
        // Check if max activations reached
        if (task.hasReachedMaxActivations()) {
            Log.d(TAG, "Task ${task.title}: Max activations reached")
            return false
        }
        
        // Check time window
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        if (!task.isWithinTimeWindow(currentMinutes)) {
            Log.d(TAG, "Task ${task.title}: Outside time window (current: $currentMinutes, window: ${task.timeWindowStart}-${task.timeWindowEnd})")
            return false
        }
        
        // For non-repeating tasks, simple activeAfter check is enough
        if (task.repeatType == RepeatType.NONE) {
            return true
        }
        
        // For repeating tasks, check if already triggered today
        if (task.lastActivatedDate != null) {
            val lastActivatedCalendar = Calendar.getInstance().apply { timeInMillis = task.lastActivatedDate }
            val todayCalendar = Calendar.getInstance()
            
            val sameDay = lastActivatedCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                    lastActivatedCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)
            
            if (sameDay) {
                Log.d(TAG, "Task ${task.title}: Already triggered today")
                return false
            }
        }
        
        // Check repeat type specific constraints
        return when (task.repeatType) {
            RepeatType.NONE -> true
            
            RepeatType.DAILY -> true
            
            RepeatType.EVERY_N_DAYS -> {
                // For EVERY_N_DAYS, we need to check if today falls on a valid interval day
                // Reference point priority: lastActivatedDate > activeAfter > now (first trigger)
                val referenceTime = task.lastActivatedDate ?: task.activeAfter ?: now
                
                if (task.lastActivatedDate == null && task.activeAfter == null) {
                    // First activation ever, no reference point - allow it
                    true
                } else if (task.lastActivatedDate != null) {
                    // Check days since last activation
                    val daysSinceLastActivation = ((now - task.lastActivatedDate) / (24 * 60 * 60 * 1000)).toInt()
                    daysSinceLastActivation >= task.repeatInterval
                } else {
                    // No lastActivatedDate but has activeAfter - check if today is a valid interval day
                    val daysSinceStart = ((now - task.activeAfter!!) / (24 * 60 * 60 * 1000)).toInt()
                    daysSinceStart >= 0 && daysSinceStart % task.repeatInterval == 0
                }
            }
            
            RepeatType.WEEKLY -> {
                // Calendar uses 1=Sunday, 2=Monday... but we use 1=Monday, 7=Sunday
                val calendarDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val ourDayOfWeek = if (calendarDayOfWeek == Calendar.SUNDAY) 7 else calendarDayOfWeek - 1
                task.isActiveOnDayOfWeek(ourDayOfWeek)
            }
        }
    }

    private fun sendNotification(context: Context, taskDescription: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "GeoReminder Task Alerts",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("GeoReminder")
            .setContentText("Task reminder: $taskDescription")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val TAG = "GeofenceBroadcastReceiver"
        private const val CHANNEL_ID = "georeminder_task_alerts"
    }
}