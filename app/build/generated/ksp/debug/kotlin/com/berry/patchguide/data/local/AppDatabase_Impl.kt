package com.berry.patchguide.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.berry.patchguide.`data`.local.dao.FavoritePatchDao
import com.berry.patchguide.`data`.local.dao.FavoritePatchDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _favoritePatchDao: Lazy<FavoritePatchDao> = lazy {
    FavoritePatchDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1,
        "0a0b1dd396ed9a1de750afa090d5288a", "a7574e81675916c596d234419441d528") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `favorite_patches` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `sourceName` TEXT NOT NULL, `url` TEXT NOT NULL, `created_at` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '0a0b1dd396ed9a1de750afa090d5288a')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `favorite_patches`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsFavoritePatches: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFavoritePatches.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFavoritePatches.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFavoritePatches.put("sourceName", TableInfo.Column("sourceName", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFavoritePatches.put("url", TableInfo.Column("url", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFavoritePatches.put("created_at", TableInfo.Column("created_at", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFavoritePatches: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFavoritePatches: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFavoritePatches: TableInfo = TableInfo("favorite_patches", _columnsFavoritePatches,
            _foreignKeysFavoritePatches, _indicesFavoritePatches)
        val _existingFavoritePatches: TableInfo = read(connection, "favorite_patches")
        if (!_infoFavoritePatches.equals(_existingFavoritePatches)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |favorite_patches(com.berry.patchguide.data.local.entity.FavoritePatchEntity).
              | Expected:
              |""".trimMargin() + _infoFavoritePatches + """
              |
              | Found:
              |""".trimMargin() + _existingFavoritePatches)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "favorite_patches")
  }

  public override fun clearAllTables() {
    super.performClear(false, "favorite_patches")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(FavoritePatchDao::class, FavoritePatchDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun favoritePatchDao(): FavoritePatchDao = _favoritePatchDao.value
}
