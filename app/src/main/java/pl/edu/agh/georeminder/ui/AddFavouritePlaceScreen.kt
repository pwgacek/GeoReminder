package pl.edu.agh.georeminder.ui

import android.content.Context
import android.location.Geocoder
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import pl.edu.agh.georeminder.model.FavouritePlace
import java.io.IOException
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFavouritePlaceScreen(
    placeToEdit: FavouritePlace? = null,
    onSave: (FavouritePlace) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(placeToEdit?.name ?: "") }
    var address by remember { mutableStateOf(placeToEdit?.address ?: "") }
    var selectedLocation by remember {
        mutableStateOf(
            placeToEdit?.let { LatLng(it.latitude, it.longitude) }
        )
    }
    var radius by remember { mutableFloatStateOf(placeToEdit?.radius ?: 100f) }

    val context = LocalContext.current

    // Default location (Krakow, Poland)
    val defaultLocation = LatLng(50.0647, 19.9450)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedLocation ?: defaultLocation, 15f)
    }

    // Auto-update address when location changes
    LaunchedEffect(selectedLocation) {
        selectedLocation?.let { location ->
            val geocodedAddress = getAddressFromLocation(context, location)
            if (!geocodedAddress.isNullOrBlank()) {
                address = geocodedAddress
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (placeToEdit == null) "Add Favourite Place"
                            else "Edit Favourite Place"
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            selectedLocation?.let { location ->
                                val place = FavouritePlace(
                                    id = placeToEdit?.id ?: 0L,
                                    name = name,
                                    address = address,
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    radius = radius
                                )
                                onSave(place)
                            }
                        },
                        enabled = name.isNotBlank() && selectedLocation != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save"
                        )
                    }
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
        ) {
            // Input fields section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Place Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Address: ${address.ifBlank { "Select location on map" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Radius: ${radius.roundToInt()} meters",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 50f..1000f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Map section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                        val markerState = rememberMarkerState(position = location)

                        // Update marker position when location changes
                        LaunchedEffect(location) {
                            markerState.position = location
                        }

                        Marker(
                            state = markerState,
                            title = name.ifBlank { "Selected Location" }
                        )
                        Circle(
                            center = location,
                            radius = radius.toDouble(),
                            strokeColor = MaterialTheme.colorScheme.primary,
                            fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
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



