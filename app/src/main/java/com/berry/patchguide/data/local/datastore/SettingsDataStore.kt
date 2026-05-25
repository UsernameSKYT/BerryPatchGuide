package com.berry.patchguide.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val AUTO_UPDATE = booleanPreferencesKey("auto_update")
        val USE_CLOUD_SERVER = booleanPreferencesKey("use_cloud_server")
    }

    val darkMode: Flow<Boolean> = dataStore.data.map { it[DARK_MODE] ?: false }
    val notifications: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS] ?: true }
    val autoUpdate: Flow<Boolean> = dataStore.data.map { it[AUTO_UPDATE] ?: true }
    val useCloudServer: Flow<Boolean> = dataStore.data.map { it[USE_CLOUD_SERVER] ?: false }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun setNotifications(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS] = enabled }
    }

    suspend fun setAutoUpdate(enabled: Boolean) {
        dataStore.edit { it[AUTO_UPDATE] = enabled }
    }

    suspend fun setUseCloudServer(enabled: Boolean) {
        dataStore.edit { it[USE_CLOUD_SERVER] = enabled }
    }
}
