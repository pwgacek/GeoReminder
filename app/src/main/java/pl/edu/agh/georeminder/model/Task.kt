package pl.edu.agh.georeminder.model

data class Task(
    val id: Long,
    val title: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float = 100f, // Radius in meters
    val isCompleted: Boolean = false
)