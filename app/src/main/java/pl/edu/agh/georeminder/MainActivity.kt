package pl.edu.agh.georeminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import pl.edu.agh.georeminder.controller.TaskViewModel
import pl.edu.agh.georeminder.location.GeofenceManager
import pl.edu.agh.georeminder.model.Task
import pl.edu.agh.georeminder.ui.AddTaskScreen
import pl.edu.agh.georeminder.ui.theme.GeoReminderTheme
import androidx.lifecycle.viewmodel.compose.viewModel
class MainActivity : ComponentActivity() {

    private val geofenceManager by lazy { GeofenceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoReminderTheme {
                val taskViewModel: TaskViewModel = viewModel()
                val context = LocalContext.current

                // Podstawowe uprawnienia (bez ACCESS_BACKGROUND_LOCATION)
                val basicPermissions = buildList {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }.toTypedArray()

                // Sprawdź czy podstawowe uprawnienia są już przyznane
                val areBasicGranted = basicPermissions.all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }

                val permissionsGranted = remember { mutableStateOf(areBasicGranted) }
                val backgroundPermissionGranted = remember { mutableStateOf(false) }

                // Launcher dla dostępu w tle
                val backgroundLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    backgroundPermissionGranted.value = isGranted
                }

                // Launcher dla podstawowych uprawnień
                val basicLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissionsResult ->
                    permissionsGranted.value = permissionsResult.values.all { it }
                    if (permissionsGranted.value) {
                        // Po przyznaniu podstawowych, żądaj dostępu w tle
                        backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                }

                LaunchedEffect(Unit) {
                    if (!permissionsGranted.value) {
                        basicLauncher.launch(basicPermissions)
                    } else {
                        // Jeśli podstawowe są już przyznane, sprawdź i żądaj dostępu w tle
                        val isBackgroundGranted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        backgroundPermissionGranted.value = isBackgroundGranted
                        if (!isBackgroundGranted) {
                            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    }
                }

                if (permissionsGranted.value && backgroundPermissionGranted.value) {
                    MainScreen(
                        viewModel = taskViewModel,
                        addGeofence = geofenceManager::addGeofenceForTask,
                        removeGeofence = geofenceManager::removeGeofenceForTask,
                    )
                } else {
                    Text("Permissions not granted. Please grant permissions to use the app.")
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: TaskViewModel,
    addGeofence: (Task) -> Unit,
    removeGeofence: (Task) -> Unit
) {
    var showAddTaskScreen by remember { mutableStateOf(false) }
    var taskBeingEdited by remember { mutableStateOf<Task?>(null) }

    val tasks by viewModel.tasks.collectAsState()

    // Add geofences for initial tasks
    LaunchedEffect(tasks) {
        tasks.forEach { addGeofence(it) }
    }

    if (showAddTaskScreen) {
        AddTaskScreen(
            taskToEdit = taskBeingEdited,
            onSave = { newTask ->
                if (taskBeingEdited == null) {
                    viewModel.saveTask(
                        newTask.title,
                        newTask.address,
                        newTask.latitude,
                        newTask.longitude,
                        newTask.radius
                    ) { saved ->
                        addGeofence(saved)
                    }
                } else {
                    viewModel.updateTask(newTask)
                    removeGeofence(taskBeingEdited!!)
                    addGeofence(newTask)
                }

                showAddTaskScreen = false
                taskBeingEdited = null
            },
            onCancel = {
                showAddTaskScreen = false
                taskBeingEdited = null
            }
        )
    } else {
        TaskListScreen(
            tasks = tasks,
            onAddTask = {
                taskBeingEdited = null
                showAddTaskScreen = true
            },
            onTaskClick = { task ->
                taskBeingEdited = task
                showAddTaskScreen = true
            },
            onDeleteTask = { task ->
                viewModel.deleteTask(task)
                removeGeofence(task)
            }
        )
    }
}

@Composable
fun TaskListScreen(
    tasks: List<Task>,
    onAddTask: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTask,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new task"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Welcome to GeoReminder",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Your location-based reminders",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (tasks.isEmpty()) {
                Text(
                    text = "No tasks yet. Tap + to add a new reminder!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(tasks.filter { !it.isCompleted }) { task ->
                        TaskItem(task = task,
                            onClick = { onTaskClick(task) },
                            onDelete = { onDeleteTask(task) })
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, // title/address left, button right
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f) // take available space
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = task.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
