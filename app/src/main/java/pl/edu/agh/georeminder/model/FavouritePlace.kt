package pl.edu.agh.georeminder.model

import androidx.room.PrimaryKey
import androidx.room.Entity
@Entity(tableName = "FavouritePlace")
data class FavouritePlace(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float = 100f 
)




