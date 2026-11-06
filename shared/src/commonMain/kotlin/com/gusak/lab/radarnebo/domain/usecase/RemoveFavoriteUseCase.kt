package com.gusak.lab.radarnebo.domain.usecase

import com.gusak.lab.radarnebo.data.FavoritesRepository

class RemoveFavoriteUseCase(private val favorites: FavoritesRepository) {
    operator fun invoke(id: String) {
        favorites.remove(id)
    }
}
