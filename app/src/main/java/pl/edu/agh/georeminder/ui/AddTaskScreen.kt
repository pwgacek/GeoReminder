package pl.edu.agh.georeminder.ui

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pl.edu.agh.georeminder.model.FavouritePlace
import pl.edu.agh.georeminder.model.Task
import pl.edu.agh.georeminder.model.RepeatType
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    taskToEdit: Task? = null,
    favouritePlaces: List<FavouritePlace> = emptyList(),
    onSave: (Task) -> Unit,
    onDelete: (() -> Unit)? = null,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var address by remember { mutableStateOf(taskToEdit?.address ?: "") }
    var isAddressEdited by remember { mutableStateOf(taskToEdit != null) }
    var selectedLocation by remember {
        mutableStateOf(
            taskToEdit?.let { LatLng(it.latitude, it.longitude) }
        )
    }
    var radius by remember { mutableFloatStateOf(taskToEdit?.radius ?: 100f) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var showFavouritePlacesDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var activeAfter by remember { mutableStateOf(taskToEdit?.activeAfter) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    
    
    var repeatType by remember { mutableStateOf(taskToEdit?.repeatType ?: RepeatType.NONE) }
    var repeatInterval by remember { mutableIntStateOf(taskToEdit?.repeatInterval ?: 2) }
    var selectedDaysOfWeek by remember { 
        mutableStateOf(taskToEdit?.getActiveDays()?.toSet() ?: emptySet<Int>()) 
    }
    var timeWindowStart by remember { mutableStateOf(taskToEdit?.timeWindowStart) }
    var timeWindowEnd by remember { mutableStateOf(taskToEdit?.timeWindowEnd) }
    var maxActivations by remember { mutableStateOf(taskToEdit?.maxActivations) }
    var showTimeWindowStartPicker by remember { mutableStateOf(false) }
    var showTimeWindowEndPicker by remember { mutableStateOf(false) }
    var showAdvancedOptions by remember { mutableStateOf(
        taskToEdit?.let { 
            it.repeatType != RepeatType.NONE || it.timeWindowStart != null || it.maxActivations != null 
        } ?: false
    ) }
    
    
    
    
    
    
    
    val isStartingDateRequired = remember(repeatType, maxActivations) {
        maxActivations != null && repeatType != RepeatType.NONE
    }
    
    
    val isStartingDateValid = remember(isStartingDateRequired, activeAfter) {
        !isStartingDateRequired || activeAfter != null
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    
    val defaultLocation = LatLng(50.0647, 19.9450)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedLocation ?: defaultLocation, 15f)
    }

    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        
        if (hasLocationPermission && taskToEdit == null) {
            scope.launch {
                getCurrentLocation(context)?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    selectedLocation = latLng
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(selectedLocation) {
        if (!isAddressEdited) { 
            selectedLocation?.let { location ->
                val geocodedAddress = getAddressFromLocation(context, location)
                if (!geocodedAddress.isNullOrBlank()) {
                    address = geocodedAddress
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (taskToEdit == null) "Add New Task"
                        else "Edit Task"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            if (title.isNotBlank() && selectedLocation != null && isStartingDateValid) {
                                onSave(
                                    Task(
                                        id = taskToEdit?.id ?: System.currentTimeMillis(),
                                        title = title,
                                        address = address.ifBlank { "Location selected" },
                                        latitude = selectedLocation!!.latitude,
                                        longitude = selectedLocation!!.longitude,
                                        radius = radius,
                                        activeAfter = activeAfter,
                                        repeatType = repeatType,
                                        repeatInterval = repeatInterval,
                                        repeatDaysOfWeek = selectedDaysOfWeek.sorted().joinToString(","),
                                        timeWindowStart = timeWindowStart,
                                        timeWindowEnd = timeWindowEnd,
                                        maxActivations = maxActivations,
                                        currentActivations = taskToEdit?.currentActivations ?: 0,
                                        lastActivatedDate = taskToEdit?.lastActivatedDate,
                                        isCompleted = taskToEdit?.isCompleted ?: false
                                    )
                                )
                            }
                        },
                        enabled = title.isNotBlank() && selectedLocation != null && isStartingDateValid
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        isAddressEdited = true 
                    },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Radius: ${radius.roundToInt()} meters",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 50f..500f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isStartingDateRequired) "Starting date (required):" else "Active after (optional):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isStartingDateRequired && activeAfter == null) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = activeAfter?.let { dateFormatter.format(Date(it)) } ?: "Always active",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (activeAfter != null) MaterialTheme.colorScheme.primary 
                                   else if (isStartingDateRequired) MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (isStartingDateRequired && activeAfter == null) {
                            Text(
                                text = "⚠ Starting date is required when activation limit is set",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Row {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = "Set active time",
                                tint = if (isStartingDateRequired && activeAfter == null) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.primary
                            )
                        }
                        if (activeAfter != null) {
                            IconButton(onClick = { activeAfter = null }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear time",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                
                if (favouritePlaces.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showFavouritePlacesDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose from Favourite Places")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                
                OutlinedButton(
                    onClick = { showAdvancedOptions = !showAdvancedOptions },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (showAdvancedOptions) "Hide Scheduling Options" else "Show Scheduling Options")
                }
                
                
                if (showAdvancedOptions) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HorizontalDivider()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Repeat Settings",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    
                    var repeatTypeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = repeatTypeExpanded,
                        onExpandedChange = { repeatTypeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = when (repeatType) {
                                RepeatType.NONE -> "No repeat (one-time)"
                                RepeatType.DAILY -> "Daily"
                                RepeatType.EVERY_N_DAYS -> "Every $repeatInterval days"
                                RepeatType.WEEKLY -> "Weekly (specific days)"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Repeat") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatTypeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = repeatTypeExpanded,
                            onDismissRequest = { repeatTypeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("No repeat (one-time)") },
                                onClick = {
                                    repeatType = RepeatType.NONE
                                    maxActivations = null 
                                    repeatTypeExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Daily") },
                                onClick = {
                                    repeatType = RepeatType.DAILY
                                    repeatTypeExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Every N days") },
                                onClick = {
                                    repeatType = RepeatType.EVERY_N_DAYS
                                    repeatTypeExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Weekly (specific days)") },
                                onClick = {
                                    repeatType = RepeatType.WEEKLY
                                    repeatTypeExpanded = false
                                }
                            )
                        }
                    }
                    
                    
                    if (repeatType == RepeatType.EVERY_N_DAYS) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Every", modifier = Modifier.padding(end = 8.dp))
                            OutlinedTextField(
                                value = repeatInterval.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let { 
                                        if (it in 1..365) repeatInterval = it 
                                    }
                                },
                                modifier = Modifier.width(80.dp),
                                singleLine = true
                            )
                            Text(" days", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    
                    
                    if (repeatType == RepeatType.WEEKLY) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Active on days:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val days = listOf("M" to 1, "T" to 2, "W" to 3, "T" to 4, "F" to 5, "S" to 6, "S" to 7)
                            days.forEach { (label, dayNum) ->
                                FilterChip(
                                    selected = selectedDaysOfWeek.contains(dayNum),
                                    onClick = {
                                        selectedDaysOfWeek = if (selectedDaysOfWeek.contains(dayNum)) {
                                            selectedDaysOfWeek - dayNum
                                        } else {
                                            selectedDaysOfWeek + dayNum
                                        }
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Time Window",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Task only triggers if visited during this time window",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        
                        OutlinedButton(
                            onClick = { showTimeWindowStartPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = timeWindowStart?.let { formatMinutesToTime(it) } ?: "Start time"
                            )
                        }
                        
                        Text(" - ", modifier = Modifier.padding(horizontal = 8.dp))
                        
                        
                        OutlinedButton(
                            onClick = { showTimeWindowEndPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = timeWindowEnd?.let { formatMinutesToTime(it) } ?: "End time"
                            )
                        }
                        
                        if (timeWindowStart != null || timeWindowEnd != null) {
                            IconButton(onClick = { 
                                timeWindowStart = null
                                timeWindowEnd = null
                            }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear time window",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    
                    if (repeatType != RepeatType.NONE) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Activation Limit",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = maxActivations != null,
                                onCheckedChange = { checked ->
                                    maxActivations = if (checked) 3 else null
                                }
                            )
                            Text("Limit activations", modifier = Modifier.padding(end = 16.dp))
                            
                            if (maxActivations != null) {
                                OutlinedTextField(
                                    value = maxActivations?.toString() ?: "",
                                    onValueChange = { value ->
                                        value.toIntOrNull()?.let { 
                                            if (it in 1..999) maxActivations = it 
                                        }
                                    },
                                    modifier = Modifier.width(80.dp),
                                    singleLine = true,
                                    label = { Text("Max") }
                                )
                                Text(" times", modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        
                        
                        if (taskToEdit != null && taskToEdit.currentActivations > 0) {
                            Text(
                                text = "Already triggered ${taskToEdit.currentActivations} time(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 48.dp, top = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HorizontalDivider()
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap on the map to select a location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        selectedLocation = latLng
                        isAddressEdited = false
                    }
                ) {
                    selectedLocation?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = title.ifBlank { "Selected Location" }
                        )

                        Circle(
                            center = location,
                            radius = radius.toDouble(),
                            strokeColor = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2f,
                            fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    }
                }

                if (!hasLocationPermission) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Location permission required",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    
    if (showFavouritePlacesDialog) {
        AlertDialog(
            onDismissRequest = { showFavouritePlacesDialog = false },
            title = { Text("Choose Favourite Place") },
            text = {
                LazyColumn {
                    items(favouritePlaces.size) { index ->
                        val place = favouritePlaces[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    
                                    address = place.name
                                    selectedLocation = LatLng(place.latitude, place.longitude)
                                    radius = place.radius
                                    isAddressEdited = true

                                    
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(
                                                LatLng(place.latitude, place.longitude),
                                                15f
                                            )
                                        )
                                    }

                                    showFavouritePlacesDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = place.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = place.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Radius: ${place.radius.toInt()}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFavouritePlacesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = activeAfter ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = millis
                            showDatePicker = false
                            showTimePicker = true
                        }
                    }
                ) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val calendar = Calendar.getInstance()
        activeAfter?.let { calendar.timeInMillis = it }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE)
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate?.let { dateMillis ->
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = dateMillis
                            cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            cal.set(Calendar.MINUTE, timePickerState.minute)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            activeAfter = cal.timeInMillis
                        }
                        showTimePicker = false
                        selectedDate = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showTimePicker = false
                    selectedDate = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete?.invoke()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    
    if (showTimeWindowStartPicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = timeWindowStart?.div(60) ?: 9,
            initialMinute = timeWindowStart?.rem(60) ?: 0
        )
        
        AlertDialog(
            onDismissRequest = { showTimeWindowStartPicker = false },
            title = { Text("Select Start Time") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        timeWindowStart = timePickerState.hour * 60 + timePickerState.minute
                        showTimeWindowStartPicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeWindowStartPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    
    if (showTimeWindowEndPicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = timeWindowEnd?.div(60) ?: 17,
            initialMinute = timeWindowEnd?.rem(60) ?: 0
        )
        
        AlertDialog(
            onDismissRequest = { showTimeWindowEndPicker = false },
            title = { Text("Select End Time") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        timeWindowEnd = timePickerState.hour * 60 + timePickerState.minute
                        showTimeWindowEndPicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeWindowEndPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private suspend fun getCurrentLocation(context: Context): Location? {
    return try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.await()
    } catch (e: SecurityException) {
        null
    } catch (e: Exception) {
        null
    }
}

private fun getAddressFromLocation(context: Context, latLng: LatLng): String? {
    return try {
        val geocoder = Geocoder(context)
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

        if (addresses != null && addresses.isNotEmpty()) {
            val address = addresses[0]
            buildString {
                address.thoroughfare?.let { append(it) }
                address.subThoroughfare?.let {
                    if (isNotEmpty()) append(" ")
                    append(it)
                }
                if (isEmpty()) {
                    address.locality?.let { append(it) }
                }
                if (isEmpty()) {
                    address.subAdminArea?.let { append(it) }
                }
            }.ifBlank { null }
        } else {
            null
        }
    } catch (e: IOException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}


private fun formatMinutesToTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format(Locale.getDefault(), "%02d:%02d", hours, mins)
}

