package pl.edu.agh.georeminder.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="Task")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float = 100f, // Radius in meters
    val isCompleted: Boolean = false,
    val activeAfter: Long? = null,
    
    // Repeat settings
    val repeatType: RepeatType = RepeatType.NONE,
    val repeatInterval: Int = 1, // Used for EVERY_N_DAYS - how many days between repeats
    val repeatDaysOfWeek: String = "", // Comma-separated days for WEEKLY (1=Monday, 7=Sunday), e.g., "1,3,5" for Mon/Wed/Fri
    
    // Time window settings (when task is active during the day)
    val timeWindowStart: Int? = null, // Minutes from midnight (e.g., 840 = 14:00)
    val timeWindowEnd: Int? = null, // Minutes from midnight (e.g., 900 = 15:00)
    
    // Activation limits
    val maxActivations: Int? = null, // null = unlimited, otherwise max number of times task can trigger
    val currentActivations: Int = 0, // How many times the task has been triggered
    
    // Tracking for repeating tasks
    val lastActivatedDate: Long? = null // Last date when this task was triggered (used to prevent multiple triggers same day)
) {
    /**
     * Checks if the task is currently within its active time window
     */
    fun isWithinTimeWindow(currentTimeMinutes: Int): Boolean {
        if (timeWindowStart == null || timeWindowEnd == null) {
            return true // No time window set, always active
        }
        
        return if (timeWindowStart <= timeWindowEnd) {
            // Normal case: e.g., 14:00 to 15:00
            currentTimeMinutes in timeWindowStart..timeWindowEnd
        } else {
            // Overnight case: e.g., 23:00 to 01:00
            currentTimeMinutes >= timeWindowStart || currentTimeMinutes <= timeWindowEnd
        }
    }
    
    /**
     * Checks if the task should be active on the given day of week (1=Monday, 7=Sunday)
     */
    fun isActiveOnDayOfWeek(dayOfWeek: Int): Boolean {
        if (repeatType != RepeatType.WEEKLY || repeatDaysOfWeek.isBlank()) {
            return true
        }
        return repeatDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.contains(dayOfWeek)
    }
    
    /**
     * Checks if the task has reached its maximum activation limit
     */
    fun hasReachedMaxActivations(): Boolean {
        return maxActivations != null && currentActivations >= maxActivations
    }
    
    /**
     * Returns a copy of the task with incremented activation count
     */
    fun withIncrementedActivation(currentDate: Long): Task {
        return copy(
            currentActivations = currentActivations + 1,
            lastActivatedDate = currentDate,
            isCompleted = if (repeatType == RepeatType.NONE) true else {
                // For repeating tasks, mark completed only if max activations reached
                maxActivations != null && (currentActivations + 1) >= maxActivations
            }
        )
    }
    
    /**
     * Gets the list of active days as integers
     */
    fun getActiveDays(): List<Int> {
        return if (repeatDaysOfWeek.isBlank()) {
            emptyList()
        } else {
            repeatDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        }
    }
}