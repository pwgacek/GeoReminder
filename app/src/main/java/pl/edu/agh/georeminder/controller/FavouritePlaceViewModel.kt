package pl.edu.agh.georeminder.controller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.edu.agh.georeminder.model.AppDatabase
import pl.edu.agh.georeminder.model.FavouritePlace
import pl.edu.agh.georeminder.model.FavouritePlaceRepository

class FavouritePlaceViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FavouritePlaceRepository(
        AppDatabase.get(app).favouritePlaceDao()
    )

    val favouritePlaces = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun saveFavouritePlace(
        name: String,
        address: String,
        latitude: Double,
        longitude: Double,
        radius: Float = 100f,
        onSaved: (FavouritePlace) -> Unit = {}
    ) {
        val place = FavouritePlace(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            radius = radius
        )

        viewModelScope.launch {
            val id = repo.insert(place)
            onSaved(place.copy(id = id))
        }
    }

    fun updateFavouritePlace(place: FavouritePlace) = viewModelScope.launch {
        repo.update(place)
    }

    fun deleteFavouritePlace(place: FavouritePlace) = viewModelScope.launch {
        repo.delete(place)
    }

    fun clearAll() = viewModelScope.launch {
        repo.clear()
    }
}


