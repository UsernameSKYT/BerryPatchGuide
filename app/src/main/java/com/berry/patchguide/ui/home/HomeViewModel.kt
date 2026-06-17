package com.berry.patchguide.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.Purchase
import com.berry.patchguide.data.ads.AdManager
import com.berry.patchguide.data.billing.BillingManager
import com.berry.patchguide.data.model.PatchItem
import com.berry.patchguide.data.repository.PatchRepository
import com.google.android.gms.ads.nativead.NativeAd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val patches: List<PatchItem>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val patchRepository: PatchRepository,
    private val billingManager: BillingManager,
    private val adManager: AdManager
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    val isAdFree: StateFlow<Boolean> = billingManager.purchases
        .map { purchases ->
            purchases.any { p ->
                p.products.contains("remove_ads") &&
                    p.purchaseState == Purchase.PurchaseState.PURCHASED
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val nativeAd: StateFlow<NativeAd?> = adManager.nativeAd

    init {
        loadFeatured()
        billingManager.startConnection()
    }

    override fun onCleared() {
        super.onCleared()
        adManager.destroyNativeAd()
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
