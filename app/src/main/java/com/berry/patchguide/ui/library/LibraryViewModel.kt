package com.berry.patchguide.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berry.patchguide.data.model.PatchItem
import com.berry.patchguide.data.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LibraryUiState {
    data object Loading : LibraryUiState()
    data class Success(val favorites: List<PatchItem>) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState

    init {
        favoriteRepository.favorites
            .onEach { favorites ->
                _uiState.value = LibraryUiState.Success(favorites)
            }
            .catch { error ->
                _uiState.value = LibraryUiState.Error(
                    error.message ?: "라이브러리를 불러오는데 실패했습니다"
                )
            }
            .launchIn(viewModelScope)
    }

    fun removeFavorite(patch: PatchItem) {
        viewModelScope.launch {
            try {
                favoriteRepository.removeFavorite(patch)
            } catch (e: Exception) {
                // Silently handle error
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            // TODO: Implement clear all favorites
        }
    }
}
