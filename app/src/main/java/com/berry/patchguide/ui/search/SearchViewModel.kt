package com.berry.patchguide.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berry.patchguide.data.model.PatchItem
import com.berry.patchguide.data.repository.FavoriteRepository
import com.berry.patchguide.data.repository.PatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchUiState {
    data object Idle : SearchUiState()
    data object Loading : SearchUiState()
    data class Success(val results: List<PatchItem>, val favorites: Set<String>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val patchRepository: PatchRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    init {
        _query
            .debounce(500)
            .filter { it.isNotBlank() }
            .distinctUntilChanged()
            .onEach { searchTerm ->
                performSearch(searchTerm)
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank()) {
            _uiState.value = SearchUiState.Idle
        }
    }

    fun toggleFavorite(patch: PatchItem) {
        viewModelScope.launch {
            try {
                val isFav = favoriteRepository.isFavorite(patch)
                if (isFav) {
                    favoriteRepository.removeFavorite(patch)
                } else {
                    favoriteRepository.addFavorite(patch)
                }
                // Refresh current state
                val currentState = _uiState.value
                if (currentState is SearchUiState.Success) {
                    val updatedFavorites = if (isFav) {
                        currentState.favorites - patch.id
                    } else {
                        currentState.favorites + patch.id
                    }
                    _uiState.value = currentState.copy(favorites = updatedFavorites)
                }
            } catch (e: Exception) {
                // Silently handle
            }
        }
    }

    private fun performSearch(term: String) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val result = patchRepository.searchAll(term)
                val favorites = favoriteRepository.favorites.first().map { it.id }.toSet()
                result
                    .onSuccess { results ->
                        _uiState.value = SearchUiState.Success(results, favorites)
                    }
                    .onFailure { error ->
                        _uiState.value = SearchUiState.Error(
                            error.message ?: "검색 중 오류가 발생했습니다"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(
                    e.message ?: "알 수 없는 오류가 발생했습니다"
                )
            }
        }
    }
}
