package pl.edu.agh.georeminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import pl.edu.agh.georeminder.location.GeofenceManager
import pl.edu.agh.georeminder.model.Task
import pl.edu.agh.georeminder.ui.AddTaskScreen
import pl.edu.agh.georeminder.ui.theme.GeoReminderTheme

class MainActivity : ComponentActivity() {

    private val geofenceManager by lazy { GeofenceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoReminderTheme {
                val context = LocalContext.current
                val permissions = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
                val permissionsGranted = remember {
                    mutableStateOf(permissions.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    })
                }

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissionsResult ->
                    permissionsGranted.value = permissionsResult.values.all { it }
                }

                LaunchedEffect(Unit) {
                    if (!permissionsGranted.value) {
                        launcher.launch(permissions)
                    }
                }

                if (permissionsGranted.value) {
                    MainScreen(
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
    addGeofence: (Task) -> Unit,
    removeGeofence: (Task) -> Unit,
) {
    var showAddTaskScreen by remember { mutableStateOf(false) }
    val tasks = remember {
        mutableStateListOf(
            Task(1L, "Buy groceries", "Supermarket on Main St", 50.0647, 19.9450, 150f),
            Task(2L, "Pick up package", "Post Office", 50.0612, 19.9380, 100f),
            Task(3L, "Return books", "City Library", 50.0680, 19.9560, 200f)
        )
    }

    // Add geofences for initial tasks
    LaunchedEffect(tasks) {
        tasks.forEach { addGeofence(it) }
    }

    if (showAddTaskScreen) {
        AddTaskScreen(
            onSave = { newTask ->
                tasks.add(newTask)
                addGeofence(newTask)
                showAddTaskScreen = false
            },
            onCancel = {
                showAddTaskScreen = false
            }
        )
    } else {
        TaskListScreen(
            tasks = tasks,
            onAddTask = { showAddTaskScreen = true }
        )
    }
}

@Composable
fun TaskListScreen(
    tasks: List<Task>,
    onAddTask: () -> Unit
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
                        TaskItem(task = task)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    GeoReminderTheme {
        MainScreen(addGeofence = {}, removeGeofence = {})
    }
}
