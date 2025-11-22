package pl.edu.agh.georeminder.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouritePlaceDao {
    @Query("SELECT * FROM FavouritePlace ORDER BY name ASC")
    fun getAll(): Flow<List<FavouritePlace>>

    @Query("SELECT * FROM FavouritePlace WHERE id = :id")
    fun getById(id: Long): Flow<FavouritePlace?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favouritePlace: FavouritePlace): Long

    @Query("DELETE FROM FavouritePlace")
    suspend fun clear()

    @Delete
    suspend fun delete(favouritePlace: FavouritePlace)

    @Update
    suspend fun update(favouritePlace: FavouritePlace)
}

