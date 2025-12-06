package pl.edu.agh.georeminder

import android.Manifest
import android.content.Intent
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import pl.edu.agh.georeminder.controller.FavouritePlaceViewModel
import pl.edu.agh.georeminder.controller.TaskViewModel
import pl.edu.agh.georeminder.location.GeofenceManager
import pl.edu.agh.georeminder.location.LocationUpdateService
import pl.edu.agh.georeminder.model.Task
import pl.edu.agh.georeminder.ui.AddFavouritePlaceScreen
import pl.edu.agh.georeminder.ui.AddTaskScreen
import pl.edu.agh.georeminder.ui.FavouritePlacesScreen
import pl.edu.agh.georeminder.ui.theme.GeoReminderTheme
class MainActivity : ComponentActivity() {

    private val geofenceManager by lazy { GeofenceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val intent = Intent(this, LocationUpdateService::class.java)
        startService(intent)
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
                    val favouritePlaceViewModel: FavouritePlaceViewModel = viewModel()
                    MainScreen(
                        viewModel = taskViewModel,
                        favouritePlaceViewModel = favouritePlaceViewModel,
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
    favouritePlaceViewModel: FavouritePlaceViewModel,
    addGeofence: (Task) -> Unit,
    removeGeofence: (Task) -> Unit
) {
    var showAddTaskScreen by remember { mutableStateOf(false) }
    var taskBeingEdited by remember { mutableStateOf<Task?>(null) }
    var showFavouritePlacesScreen by remember { mutableStateOf(false) }
    var showAddFavouritePlaceScreen by remember { mutableStateOf(false) }
    var placeBeingEdited by remember { mutableStateOf<pl.edu.agh.georeminder.model.FavouritePlace?>(null) }

    val tasks by viewModel.tasks.collectAsState()
    val favouritePlaces by favouritePlaceViewModel.favouritePlaces.collectAsState()

    // Add geofences for initial tasks
    LaunchedEffect(tasks) {
        tasks.filter { !it.isCompleted }.forEach { addGeofence(it) }
    }

    when {
        showAddFavouritePlaceScreen -> {
            AddFavouritePlaceScreen(
                placeToEdit = placeBeingEdited,
                onSave = { place ->
                    if (placeBeingEdited == null) {
                        favouritePlaceViewModel.saveFavouritePlace(
                            place.name,
                            place.address,
                            place.latitude,
                            place.longitude,
                            place.radius
                        )
                    } else {
                        favouritePlaceViewModel.updateFavouritePlace(place)
                    }
                    showAddFavouritePlaceScreen = false
                    placeBeingEdited = null
                    showFavouritePlacesScreen = true
                },
                onCancel = {
                    showAddFavouritePlaceScreen = false
                    placeBeingEdited = null
                    showFavouritePlacesScreen = true
                }
            )
        }
        showFavouritePlacesScreen -> {
            FavouritePlacesScreen(
                viewModel = favouritePlaceViewModel,
                onBack = { showFavouritePlacesScreen = false },
                onAddPlace = {
                    placeBeingEdited = null
                    showAddFavouritePlaceScreen = true
                },
                onEditPlace = { place ->
                    placeBeingEdited = place
                    showAddFavouritePlaceScreen = true
                }
            )
        }
        showAddTaskScreen -> {
            AddTaskScreen(
                taskToEdit = taskBeingEdited,
                favouritePlaces = favouritePlaces,
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
        }
        else -> {
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
                },
                onNavigateToFavouritePlaces = {
                    showFavouritePlacesScreen = true
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    tasks: List<Task>,
    onAddTask: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onNavigateToFavouritePlaces: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        drawerShape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 0.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 0.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "GeoReminder",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                label = { Text("Favourite Places") },
                                selected = false,
                                onClick = {
                                    scope.launch {
                                        drawerState.close()
                                    }
                                    onNavigateToFavouritePlaces()
                                }
                            )
                        }
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = { Text("GeoReminder") },
                            actions = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    },
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
                                    TaskItem(
                                        task = task,
                                        onClick = { onTaskClick(task) },
                                        onDelete = { onDeleteTask(task) }
                                    )
                                }
                            }
                        }
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
