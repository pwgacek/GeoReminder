package pl.edu.agh.georeminder.model

import kotlinx.coroutines.flow.Flow

class FavouritePlaceRepository(private val dao: FavouritePlaceDao) {

    fun getAll(): Flow<List<FavouritePlace>> = dao.getAll()

    fun getById(id: Long): Flow<FavouritePlace?> = dao.getById(id)

    suspend fun insert(favouritePlace: FavouritePlace): Long {
        return dao.insert(favouritePlace)
    }

    suspend fun update(favouritePlace: FavouritePlace) {
        dao.update(favouritePlace)
    }

    suspend fun delete(favouritePlace: FavouritePlace) {
        dao.delete(favouritePlace)
    }

    suspend fun clear() {
        dao.clear()
    }
}

