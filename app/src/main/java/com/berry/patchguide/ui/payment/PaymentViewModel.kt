package com.berry.patchguide.ui.payment

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.berry.patchguide.data.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = billingManager.isConnected
    val productDetails: StateFlow<List<ProductDetails>> = billingManager.productDetails
    val purchases: StateFlow<List<Purchase>> = billingManager.purchases

    private val _isAdFree = MutableStateFlow(false)
    val isAdFree: StateFlow<Boolean> = _isAdFree

    init {
        purchases
            .onEach { _isAdFree.value = billingManager.isAdFree() }
            .launchIn(viewModelScope)
    }

    fun startConnection() {
        billingManager.startConnection()
    }

    fun endConnection() {
        billingManager.endConnection()
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        billingManager.launchBillingFlow(activity, productDetails)
    }
}
