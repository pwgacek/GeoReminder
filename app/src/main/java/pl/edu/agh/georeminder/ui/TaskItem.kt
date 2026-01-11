package pl.edu.agh.georeminder.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.edu.agh.georeminder.model.Task
import pl.edu.agh.georeminder.model.RepeatType
import java.util.Locale

@Composable
fun TaskItem(
    task: Task,
    modifier: Modifier = Modifier,
    showActiveStatus: Boolean = true,
    onClick: () -> Unit
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
            
            
            if (task.repeatType != RepeatType.NONE) {
                val repeatText = when (task.repeatType) {
                    RepeatType.DAILY -> "Repeats daily"
                    RepeatType.EVERY_N_DAYS -> "Repeats every ${task.repeatInterval} days"
                    RepeatType.WEEKLY -> {
                        val days = task.getActiveDays()
                        val dayNames = days.map { dayNum ->
                            when (dayNum) {
                                1 -> "Mon"
                                2 -> "Tue"
                                3 -> "Wed"
                                4 -> "Thu"
                                5 -> "Fri"
                                6 -> "Sat"
                                7 -> "Sun"
                                else -> ""
                            }
                        }
                        "Repeats on ${dayNames.joinToString(", ")}"
                    }
                    RepeatType.NONE -> ""
                }
                Text(
                    text = repeatText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            
            if (task.timeWindowStart != null && task.timeWindowEnd != null) {
                Text(
                    text = "Active ${formatMinutesToTime(task.timeWindowStart)} - ${formatMinutesToTime(task.timeWindowEnd)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            
            if (task.maxActivations != null) {
                Text(
                    text = "Activations: ${task.currentActivations}/${task.maxActivations}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            if (showActiveStatus) {
                val activeAfterTimestamp = task.activeAfter
                val isActiveNow = activeAfterTimestamp == null || System.currentTimeMillis() >= activeAfterTimestamp
                val hasReachedLimit = task.hasReachedMaxActivations()
                
                when {
                    hasReachedLimit -> {
                        Text(
                            text = "Max activations reached",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    isActiveNow -> {
                        Text(
                            text = if (task.repeatType != RepeatType.NONE) "Active (repeating)" else "Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    else -> {
                        val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                        Text(
                            text = "Active after: ${formatter.format(java.util.Date(activeAfterTimestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}


private fun formatMinutesToTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format(Locale.getDefault(), "%02d:%02d", hours, mins)
}

