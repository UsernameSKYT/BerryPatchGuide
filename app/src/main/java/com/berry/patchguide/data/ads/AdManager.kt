package com.berry.patchguide.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "AdManager"

    companion object {
        const val NATIVE_AD_UNIT_ID = "ca-app-pub-2046242748505446/6758519030"
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // TODO: 실제 전면광고 단위 ID로 교체
    }

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    // 네이티브 광고
    private val _nativeAd = MutableStateFlow<NativeAd?>(null)
    val nativeAd: StateFlow<NativeAd?> = _nativeAd

    // 전면 광고
    private var interstitialAd: InterstitialAd? = null
    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady

    fun initialize() {
        if (_isInitialized.value) return
        MobileAds.initialize(context) {
            _isInitialized.value = true
            Log.d(TAG, "MobileAds initialized")
            loadNativeAd()
            loadInterstitial()
        }
    }

    fun loadNativeAd() {
        val adLoader = AdLoader.Builder(context, NATIVE_AD_UNIT_ID)
            .forNativeAd { ad ->
                _nativeAd.value?.destroy()
                _nativeAd.value = ad
                Log.d(TAG, "Native ad loaded")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load: ${error.message}")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    _isInterstitialReady.value = true
                    Log.d(TAG, "Interstitial ad loaded")
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            _isInterstitialReady.value = false
                            loadInterstitial()
                        }
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            interstitialAd = null
                            _isInterstitialReady.value = false
                            loadInterstitial()
                        }
                    }
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    _isInterstitialReady.value = false
                    Log.w(TAG, "Interstitial ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit = {}) {
        val ad = interstitialAd
        if (ad != null) {
            val originalCallback = ad.fullScreenContentCallback
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    originalCallback?.onAdDismissedFullScreenContent()
                    onDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    originalCallback?.onAdFailedToShowFullScreenContent(adError)
                    onDismissed()
                }
            }
            ad.show(activity)
        } else {
            onDismissed()
        }
    }

    fun destroyNativeAd() {
        _nativeAd.value?.destroy()
        _nativeAd.value = null
    }
}
