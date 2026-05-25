package com.berry.patchguide.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berry.patchguide.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SettingsUiState {
    data object Loading : SettingsUiState()
    data class Success(
        val darkMode: Boolean,
        val notifications: Boolean,
        val autoUpdate: Boolean,
        val useCloudServer: Boolean
    ) : SettingsUiState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {
    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        combine(
            settingsDataStore.darkMode,
            settingsDataStore.notifications,
            settingsDataStore.autoUpdate,
            settingsDataStore.useCloudServer
        ) { darkMode, notifications, autoUpdate, useCloudServer ->
            SettingsUiState.Success(darkMode, notifications, autoUpdate, useCloudServer)
        }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setDarkMode(enabled)
        }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setNotifications(enabled)
        }
    }

    fun setAutoUpdate(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAutoUpdate(enabled)
        }
    }

    fun setUseCloudServer(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setUseCloudServer(enabled)
        }
    }
}
