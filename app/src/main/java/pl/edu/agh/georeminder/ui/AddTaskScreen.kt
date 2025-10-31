package pl.edu.agh.georeminder.ui

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pl.edu.agh.georeminder.model.Task
import java.io.IOException
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    onSave: (Task) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var radius by remember { mutableFloatStateOf(100f) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Default location (Krakow, Poland)
    val defaultLocation = LatLng(50.0647, 19.9450)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedLocation ?: defaultLocation, 15f)
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (hasLocationPermission) {
            scope.launch {
                getCurrentLocation(context)?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    selectedLocation = latLng
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                }
            }
        }
    }

    // Request location permission on start
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Update address when location changes
    LaunchedEffect(selectedLocation) {
        selectedLocation?.let { location ->
            val geocodedAddress = getAddressFromLocation(context, location)
            if (geocodedAddress != null) {
                address = geocodedAddress
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Task") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (title.isNotBlank() && selectedLocation != null) {
                                onSave(
                                    Task(
                                        id = System.currentTimeMillis(),
                                        title = title,
                                        address = address.ifBlank { "Location selected" },
                                        latitude = selectedLocation!!.latitude,
                                        longitude = selectedLocation!!.longitude,
                                        radius = radius
                                    )
                                )
                            }
                        },
                        enabled = title.isNotBlank() && selectedLocation != null
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
            // Input fields
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
                    onValueChange = { address = it },
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

                Text(
                    text = "Tap on the map to select a location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Map
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

