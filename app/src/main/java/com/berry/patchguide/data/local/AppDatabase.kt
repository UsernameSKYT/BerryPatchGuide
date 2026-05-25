package com.berry.patchguide.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.berry.patchguide.data.local.dao.FavoritePatchDao
import com.berry.patchguide.data.local.entity.FavoritePatchEntity

@Database(
    entities = [FavoritePatchEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritePatchDao(): FavoritePatchDao
}
