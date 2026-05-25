package com.berry.patchguide.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.berry.patchguide.data.local.entity.FavoritePatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePatchDao {
    @Query("SELECT * FROM favorite_patches ORDER BY created_at DESC")
    fun getAll(): Flow<List<FavoritePatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoritePatchEntity): Long

    @Delete
    suspend fun delete(entity: FavoritePatchEntity)

    @Query("DELETE FROM favorite_patches WHERE id = :id")
    suspend fun deleteById(id: Long)
}
