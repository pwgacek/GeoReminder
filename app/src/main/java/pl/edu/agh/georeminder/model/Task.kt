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
    val radius: Float = 100f, 
    val isCompleted: Boolean = false,
    val activeAfter: Long? = null,
    
    
    val repeatType: RepeatType = RepeatType.NONE,
    val repeatInterval: Int = 1, 
    val repeatDaysOfWeek: String = "", 
    
    
    val timeWindowStart: Int? = null, 
    val timeWindowEnd: Int? = null, 
    
    
    val maxActivations: Int? = null, 
    val currentActivations: Int = 0, 
    
    
    val lastActivatedDate: Long? = null 
) {
    
    fun isWithinTimeWindow(currentTimeMinutes: Int): Boolean {
        if (timeWindowStart == null || timeWindowEnd == null) {
            return true 
        }
        
        return if (timeWindowStart <= timeWindowEnd) {
            
            currentTimeMinutes in timeWindowStart..timeWindowEnd
        } else {
            
            currentTimeMinutes >= timeWindowStart || currentTimeMinutes <= timeWindowEnd
        }
    }
    
    
    fun isActiveOnDayOfWeek(dayOfWeek: Int): Boolean {
        if (repeatType != RepeatType.WEEKLY || repeatDaysOfWeek.isBlank()) {
            return true
        }
        return repeatDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.contains(dayOfWeek)
    }
    
    
    fun hasReachedMaxActivations(): Boolean {
        return maxActivations != null && currentActivations >= maxActivations
    }
    
    
    fun withIncrementedActivation(currentDate: Long): Task {
        return copy(
            currentActivations = currentActivations + 1,
            lastActivatedDate = currentDate,
            isCompleted = if (repeatType == RepeatType.NONE) true else {
                
                maxActivations != null && (currentActivations + 1) >= maxActivations
            }
        )
    }
    
    
    fun getActiveDays(): List<Int> {
        return if (repeatDaysOfWeek.isBlank()) {
            emptyList()
        } else {
            repeatDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        }
    }
}