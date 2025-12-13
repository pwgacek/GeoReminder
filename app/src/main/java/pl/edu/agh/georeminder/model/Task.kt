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
    val activeAfter: Long? = null
)