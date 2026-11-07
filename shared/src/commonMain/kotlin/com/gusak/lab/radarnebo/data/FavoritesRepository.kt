package com.gusak.lab.radarnebo.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer

/** Favorite locations, persisted in [KeyValueStorage] and observable as [favorites]. */
class FavoritesRepository(private val storage: KeyValueStorage) {
    private val _favorites = MutableStateFlow(load())
    val favorites: StateFlow<List<FavoriteLocation>> = _favorites.asStateFlow()

    fun add(favorite: FavoriteLocation) {
        save(_favorites.value + favorite)
    }

    internal fun remove(id: String) {
        save(_favorites.value.filterNot { it.id == id })
    }

    private fun load(): List<FavoriteLocation> {
        val stored = storage.getString(K_FAVORITES) ?: return emptyList()
        return try {
            RadarNeboApi.json.decodeFromString(serializer, stored)
        } catch (_: IllegalArgumentException) {
            // Unreadable JSON (kotlinx.serialization's SerializationException is an IllegalArgumentException).
            emptyList()
        }
    }

    private fun save(favorites: List<FavoriteLocation>) {
        storage.putString(K_FAVORITES, RadarNeboApi.json.encodeToString(serializer, favorites))
        _favorites.value = favorites
    }

    private companion object {
        val serializer = ListSerializer(FavoriteLocation.serializer())
        const val K_FAVORITES = "favorites"
    }
}
