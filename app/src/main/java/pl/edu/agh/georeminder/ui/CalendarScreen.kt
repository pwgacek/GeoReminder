package pl.edu.agh.georeminder.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.edu.agh.georeminder.model.Task
import pl.edu.agh.georeminder.model.RepeatType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    tasks: List<Task>,
    onBack: () -> Unit,
    onTaskClick: (Task) -> Unit
) {
    var currentWeekStart by remember {
        mutableStateOf(LocalDate.now().with(java.time.DayOfWeek.MONDAY))
    }

    val weekDays = remember(currentWeekStart) {
        (0..6).map { currentWeekStart.plusDays(it.toLong()) }
    }

    // Build tasks for each day, including repeating tasks
    val tasksByDay = remember(tasks, currentWeekStart) {
        buildTasksByDay(tasks, weekDays)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Week navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentWeekStart = currentWeekStart.minusWeeks(1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Week")
                }
                
                val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                Text(
                    text = currentWeekStart.format(monthYearFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { currentWeekStart = currentWeekStart.plusWeeks(1) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Week")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(weekDays) { day ->
                    DaySection(
                        day = day,
                        taskEntries = tasksByDay[day] ?: emptyList(),
                        onTaskClick = onTaskClick
                    )
                }
            }
        }
    }
}

@Composable
fun DaySection(
    day: LocalDate,
    taskEntries: List<CalendarTaskEntry>,
    onTaskClick: (Task) -> Unit
) {
    val isToday = day == LocalDate.now()
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = day.format(dayFormatter),
            style = MaterialTheme.typography.titleSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        if (taskEntries.isEmpty()) {
            Text(
                text = "No tasks for this day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        } else {
            taskEntries.sortedBy { it.displayTime ?: "99:99" }.forEach { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { onTaskClick(entry.task) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.displayTime ?: "All day",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(60.dp)
                        )
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = entry.task.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                if (entry.isRepeating) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = "Repeating",
                                        modifier = Modifier
                                            .padding(start = 4.dp)
                                            .size(16.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Text(
                                text = entry.task.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            // Show time window if set
                            if (entry.task.timeWindowStart != null && entry.task.timeWindowEnd != null) {
                                Text(
                                    text = "Window: ${formatMinutes(entry.task.timeWindowStart)} - ${formatMinutes(entry.task.timeWindowEnd)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            // Show activation progress if limited
                            if (entry.task.maxActivations != null) {
                                Text(
                                    text = "${entry.task.currentActivations}/${entry.task.maxActivations} activations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

/**
 * Data class to hold task display info for calendar
 */
data class CalendarTaskEntry(
    val task: Task,
    val isRepeating: Boolean,
    val displayTime: String? // Time to show, null if no specific time
)

/**
 * Formats minutes from midnight to HH:mm
 */
private fun formatMinutes(minutes: Int): String {
    return String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60)
}

/**
 * Builds a map of tasks for each day in the week, including repeating tasks
 * Respects activation limits for repeating tasks - counts from the task's start date
 */
private fun buildTasksByDay(tasks: List<Task>, weekDays: List<LocalDate>): Map<LocalDate, List<CalendarTaskEntry>> {
    val result = mutableMapOf<LocalDate, MutableList<CalendarTaskEntry>>()
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    
    // Initialize empty lists for all days
    weekDays.forEach { day ->
        result[day] = mutableListOf()
    }
    
    val today = LocalDate.now()
    
    tasks.filter { !it.isCompleted }.forEach { task ->
        // For tasks with activation limits, we need to count occurrences from the start date
        val remainingActivations = if (task.maxActivations != null) {
            task.maxActivations - task.currentActivations
        } else {
            Int.MAX_VALUE
        }
        
        // Determine the task's effective start date
        // For DAILY/EVERY_N_DAYS with maxActivations, activeAfter should be set (enforced by UI)
        // For WEEKLY with maxActivations but no activeAfter, use today as implicit start
        val taskStartDate = if (task.activeAfter != null) {
            Instant.ofEpochMilli(task.activeAfter)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        } else if (task.maxActivations != null) {
            // For tasks with limits but no explicit start, use today
            today
        } else {
            // For unlimited tasks without activeAfter, use today as reference
            today
        }
        
        // Track occurrence count across the week to properly limit activations
        var occurrenceCount = 0
        
        // For repeating tasks with limits, count how many occurrences happen 
        // between taskStartDate and each day in the week view
        weekDays.sorted().forEach { day ->
            if (shouldShowTaskOnDay(task, day, taskStartDate)) {
                // For tasks with activation limits, check if this occurrence is within limit
                if (task.maxActivations != null && task.repeatType != RepeatType.NONE) {
                    // Count total occurrences from start date to this day
                    val totalOccurrences = countOccurrencesBetween(task, taskStartDate, day)
                    if (totalOccurrences > remainingActivations) {
                        return@forEach // Skip this day, limit reached
                    }
                    occurrenceCount = totalOccurrences
                }
                
                val displayTime = when {
                    // For tasks with time window, show the window
                    task.timeWindowStart != null -> {
                        String.format(Locale.getDefault(), "%02d:%02d", task.timeWindowStart / 60, task.timeWindowStart % 60)
                    }
                    // For tasks with activeAfter on this specific day, show that time
                    task.activeAfter != null -> {
                        val activeDate = Instant.ofEpochMilli(task.activeAfter)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        if (activeDate == day) {
                            Instant.ofEpochMilli(task.activeAfter)
                                .atZone(ZoneId.systemDefault())
                                .toLocalTime()
                                .format(timeFormatter)
                        } else {
                            null // Repeating instance, no specific time
                        }
                    }
                    else -> null
                }
                
                result[day]?.add(CalendarTaskEntry(
                    task = task,
                    isRepeating = task.repeatType != RepeatType.NONE,
                    displayTime = displayTime
                ))
            }
        }
    }
    
    return result
}

/**
 * Counts how many times a repeating task would occur between startDate (inclusive) and endDate (inclusive)
 */
private fun countOccurrencesBetween(task: Task, startDate: LocalDate, endDate: LocalDate): Int {
    if (startDate.isAfter(endDate)) return 0
    
    var count = 0
    var current = startDate
    
    while (!current.isAfter(endDate)) {
        if (isTaskScheduledOnDay(task, current)) {
            count++
        }
        current = current.plusDays(1)
    }
    
    return count
}

/**
 * Checks if a task is scheduled on a specific day (ignoring activation limits)
 * Used for counting occurrences between dates
 */
private fun isTaskScheduledOnDay(task: Task, day: LocalDate): Boolean {
    val today = LocalDate.now()
    
    return when (task.repeatType) {
        RepeatType.NONE -> false
        RepeatType.DAILY -> true
        RepeatType.EVERY_N_DAYS -> {
            val startDate = when {
                task.activeAfter != null -> Instant.ofEpochMilli(task.activeAfter)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                task.lastActivatedDate != null -> Instant.ofEpochMilli(task.lastActivatedDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                else -> today // Use today as implicit start for unlimited tasks
            }
            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, day)
            daysBetween >= 0 && daysBetween % task.repeatInterval == 0L
        }
        RepeatType.WEEKLY -> {
            val dayOfWeek = day.dayOfWeek.value
            task.isActiveOnDayOfWeek(dayOfWeek)
        }
    }
}

/**
 * Determines if a task should be shown on a specific day
 * @param task The task to check
 * @param day The day to check
 * @param taskStartDate The calculated start date for the task (for EVERY_N_DAYS calculation)
 */
private fun shouldShowTaskOnDay(task: Task, day: LocalDate, taskStartDate: LocalDate): Boolean {
    val today = LocalDate.now()
    
    // Check if task has reached max activations
    if (task.hasReachedMaxActivations()) {
        return false
    }
    
    // If task has an activeAfter date, check if we've passed it or if it's on this day
    if (task.activeAfter != null) {
        val activeDate = Instant.ofEpochMilli(task.activeAfter)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        
        // Non-repeating task: only show on its specific activeAfter date
        if (task.repeatType == RepeatType.NONE) {
            return day == activeDate
        }
        
        // Repeating task: don't show before the activeAfter date
        if (day.isBefore(activeDate)) {
            return false
        }
    }
    
    // For non-repeating tasks without activeAfter, show on today only
    if (task.repeatType == RepeatType.NONE && task.activeAfter == null) {
        return day == today
    }
    
    // Handle repeating tasks
    return when (task.repeatType) {
        RepeatType.NONE -> false // Already handled above
        
        RepeatType.DAILY -> {
            // For DAILY tasks without activeAfter and without maxActivations, show from today onwards
            // For DAILY tasks with maxActivations but no activeAfter, don't show (UI should prevent this)
            if (task.activeAfter == null) {
                if (task.maxActivations != null) {
                    false // Should not happen - UI enforces activeAfter for limited tasks
                } else {
                    !day.isBefore(today)
                }
            } else {
                true
            }
        }
        
        RepeatType.EVERY_N_DAYS -> {
            // EVERY_N_DAYS needs a reference point to calculate intervals
            val startDate = if (task.activeAfter != null) {
                Instant.ofEpochMilli(task.activeAfter)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            } else if (task.lastActivatedDate != null) {
                // Fallback: Use last activated date as reference for existing tasks
                Instant.ofEpochMilli(task.lastActivatedDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            } else if (task.maxActivations == null) {
                // No limit set and no explicit start - use today as implicit start
                // This allows infinite repeating tasks to work without requiring a start date
                today
            } else {
                // Has activation limit but no start date - shouldn't happen (UI enforces this)
                return false
            }
            
            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, day)
            daysBetween >= 0 && daysBetween % task.repeatInterval == 0L
        }
        
        RepeatType.WEEKLY -> {
            // WEEKLY: Check if this day of week is in the selected days
            val dayOfWeek = day.dayOfWeek.value
            if (!task.isActiveOnDayOfWeek(dayOfWeek)) {
                return false
            }
            
            // If there's an explicit start date, don't show before it
            if (task.activeAfter != null) {
                val startDate = Instant.ofEpochMilli(task.activeAfter)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                if (day.isBefore(startDate)) {
                    return false
                }
            } else if (task.maxActivations != null) {
                // For WEEKLY with activation limits but no explicit start date,
                // use today as implicit start - don't show before today
                if (day.isBefore(today)) {
                    return false
                }
            }
            
            true
        }
    }
}
