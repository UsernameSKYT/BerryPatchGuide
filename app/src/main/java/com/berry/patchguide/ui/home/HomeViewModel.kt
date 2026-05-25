package com.berry.patchguide.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berry.patchguide.data.model.PatchItem
import com.berry.patchguide.data.repository.PatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val patches: List<PatchItem>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val patchRepository: PatchRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadFeatured()
    }

    private fun loadFeatured() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                patchRepository.getFeatured()
                    .onSuccess { patches ->
                        _uiState.value = HomeUiState.Success(patches)
                    }
                    .onFailure { error ->
                        _uiState.value = HomeUiState.Error(
                            error.message ?: "추천 패치를 불러오는데 실패했습니다"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(
                    e.message ?: "알 수 없는 오류가 발생했습니다"
                )
            }
        }
    }
}
