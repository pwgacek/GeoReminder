package pl.edu.agh.georeminder.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.edu.agh.georeminder.controller.TaskViewModel
import pl.edu.agh.georeminder.model.Task

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskHistoryScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val completedTasks by viewModel.completedTasks.collectAsState()
    var taskToRestore by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Task History")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (completedTasks.isEmpty()) {
                Text(
                    text = "No completed tasks yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(completedTasks) { task ->
                        TaskItem(
                            task = task,
                            showActiveStatus = false,
                            onClick = { taskToRestore = task }
                        )
                    }
                }
            }
        }
    }

    
    taskToRestore?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToRestore = null },
            title = { Text("Restore Task") },
            text = { Text("Do you want to restore \"${task.title}\" to active tasks?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateTask(task.copy(isCompleted = false))
                        taskToRestore = null
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

