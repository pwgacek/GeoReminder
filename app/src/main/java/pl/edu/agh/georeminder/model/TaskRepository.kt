package pl.edu.agh.georeminder.model

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {

    fun getAll(): Flow<List<Task>> = dao.getAll()

    fun getCompletedTasks(): Flow<List<Task>> = dao.getCompletedTasks()

    fun getById(id: Long): Flow<Task?> = dao.getById(id)

    suspend fun insert(task: Task) {
        dao.insert(task)
    }

    suspend fun update(task: Task) {
        dao.update(task)
    }

    suspend fun delete(task: Task) {
        dao.delete(task)
    }

    suspend fun clear() {
        dao.clear()
    }
}


