package pl.edu.agh.georeminder.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM Task")
    fun getAll(): Flow<List<Task>>

    @Query("SELECT * FROM Task WHERE isCompleted = 1 ORDER BY id DESC")
    fun getCompletedTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM Task WHERE isCompleted = 0")
    fun getActiveTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM Task WHERE isCompleted = 0")
    suspend fun getActiveTasksSync(): List<Task>

    @Query("SELECT * FROM Task WHERE id = :id")
    fun getById(id: Long): Flow<Task?>
    
    @Query("SELECT * FROM Task WHERE id = :id")
    suspend fun getByIdSync(id: Long): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Query("DELETE FROM Task")
    suspend fun clear()

    @Delete
    suspend fun delete(task: Task)

    @Update
    suspend fun update(task: Task)
    
    @Query("UPDATE Task SET currentActivations = :count, lastActivatedDate = :lastDate, isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateActivationStatus(taskId: Long, count: Int, lastDate: Long?, isCompleted: Boolean)
}