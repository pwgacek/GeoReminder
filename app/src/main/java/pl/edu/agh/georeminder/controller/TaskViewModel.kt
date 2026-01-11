package pl.edu.agh.georeminder.controller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.edu.agh.georeminder.model.AppDatabase
import pl.edu.agh.georeminder.model.TaskRepository
import pl.edu.agh.georeminder.model.Task
import pl.edu.agh.georeminder.model.RepeatType

class TaskViewModel(app: Application) : AndroidViewModel(app) {

    private val repo =
        TaskRepository (
            AppDatabase.get(app).taskDao()
        )

    val tasks = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val completedTasks = repo.getCompletedTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    val activeTasks = repo.getActiveTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun saveTask(
        title: String,
        address: String,
        latitude: Double,
        longitude: Double,
        radius: Float = 100f,
        activeAfter: Long? = null,
        repeatType: RepeatType = RepeatType.NONE,
        repeatInterval: Int = 1,
        repeatDaysOfWeek: String = "",
        timeWindowStart: Int? = null,
        timeWindowEnd: Int? = null,
        maxActivations: Int? = null,
        onSaved: (Task) -> Unit = {}
    ) {
        val rec = Task(
            id = System.currentTimeMillis(),
            title = title,
            address = address,
            latitude = latitude,
            longitude = longitude,
            radius = radius,
            isCompleted = false,
            activeAfter = activeAfter,
            repeatType = repeatType,
            repeatInterval = repeatInterval,
            repeatDaysOfWeek = repeatDaysOfWeek,
            timeWindowStart = timeWindowStart,
            timeWindowEnd = timeWindowEnd,
            maxActivations = maxActivations,
            currentActivations = 0,
            lastActivatedDate = null
        )

        viewModelScope.launch {
            repo.insert(rec)
            onSaved(rec)
        }
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repo.update(task)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repo.delete(task)
    }

    fun clearAll() = viewModelScope.launch {
        repo.clear()
    }
    
    fun resetTaskActivations(task: Task) = viewModelScope.launch {
        repo.update(task.copy(
            currentActivations = 0,
            isCompleted = false,
            lastActivatedDate = null
        ))
    }
}

