package com.berry.patchguide.data.repository

import com.berry.patchguide.data.local.dao.FavoritePatchDao
import com.berry.patchguide.data.local.entity.FavoritePatchEntity
import com.berry.patchguide.data.model.PatchItem
import com.berry.patchguide.data.model.PatchSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val favoritePatchDao: FavoritePatchDao
) {
    val favorites: Flow<List<PatchItem>> = favoritePatchDao.getAll().map { entities ->
        entities.map { it.toPatchItem() }
    }

    suspend fun addFavorite(patch: PatchItem) {
        favoritePatchDao.insert(
            FavoritePatchEntity(
                title = patch.title,
                sourceName = patch.source,
                url = patch.downloadUrl ?: ""
            )
        )
    }

    suspend fun removeFavorite(patch: PatchItem) {
        val entity = favoritePatchDao.getAll().first().find {
            it.title == patch.title && it.sourceName == patch.source
        }
        entity?.let { favoritePatchDao.delete(it) }
    }

    suspend fun removeFavoriteById(id: Long) {
        favoritePatchDao.deleteById(id)
    }

    suspend fun isFavorite(patch: PatchItem): Boolean {
        return favoritePatchDao.getAll().first().any {
            it.title == patch.title && it.sourceName == patch.source
        }
    }

    private fun FavoritePatchEntity.toPatchItem(): PatchItem {
        return PatchItem(
            id = "fav_$id",
            title = title,
            source = sourceName,
            downloadUrl = url
        )
    }
}
