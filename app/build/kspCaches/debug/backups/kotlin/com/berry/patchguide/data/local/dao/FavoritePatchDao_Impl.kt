package com.berry.patchguide.`data`.local.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.berry.patchguide.`data`.local.entity.FavoritePatchEntity
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class FavoritePatchDao_Impl(
  __db: RoomDatabase,
) : FavoritePatchDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfFavoritePatchEntity: EntityInsertAdapter<FavoritePatchEntity>

  private val __deleteAdapterOfFavoritePatchEntity: EntityDeleteOrUpdateAdapter<FavoritePatchEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfFavoritePatchEntity = object : EntityInsertAdapter<FavoritePatchEntity>()
        {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `favorite_patches` (`id`,`title`,`sourceName`,`url`,`created_at`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FavoritePatchEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.sourceName)
        statement.bindText(4, entity.url)
        statement.bindLong(5, entity.createdAt)
      }
    }
    this.__deleteAdapterOfFavoritePatchEntity = object :
        EntityDeleteOrUpdateAdapter<FavoritePatchEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `favorite_patches` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: FavoritePatchEntity) {
        statement.bindLong(1, entity.id)
      }
    }
  }

  public override suspend fun insert(entity: FavoritePatchEntity): Long = performSuspending(__db,
      false, true) { _connection ->
    val _result: Long = __insertAdapterOfFavoritePatchEntity.insertAndReturnId(_connection, entity)
    _result
  }

  public override suspend fun delete(entity: FavoritePatchEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __deleteAdapterOfFavoritePatchEntity.handle(_connection, entity)
  }

  public override fun getAll(): Flow<List<FavoritePatchEntity>> {
    val _sql: String = "SELECT * FROM favorite_patches ORDER BY created_at DESC"
    return createFlow(__db, false, arrayOf("favorite_patches")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfSourceName: Int = getColumnIndexOrThrow(_stmt, "sourceName")
        val _columnIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: MutableList<FavoritePatchEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: FavoritePatchEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpSourceName: String
          _tmpSourceName = _stmt.getText(_columnIndexOfSourceName)
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_columnIndexOfUrl)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = FavoritePatchEntity(_tmpId,_tmpTitle,_tmpSourceName,_tmpUrl,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM favorite_patches WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
