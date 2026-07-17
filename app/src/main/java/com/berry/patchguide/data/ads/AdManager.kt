package com.berry.patchguide.data.ads

import android.content.Context
import android.util.Log
import com.berry.patchguide.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import android.os.Handler
import android.os.Looper
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
    private val retryHandler = Handler(Looper.getMainLooper())
    private var retryRunnable: Runnable? = null

    companion object {
        // 디버그 빌드: 항상 테스트 ID. 릴리즈 빌드: local.properties에 설정한 실제 광고 ID
        // (app/build.gradle.kts의 buildConfigField 참고)
        const val BANNER_AD_UNIT_ID = BuildConfig.BANNER_AD_UNIT_ID
        const val NATIVE_AD_UNIT_ID = BuildConfig.NATIVE_AD_UNIT_ID
    }

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    private val _nativeAd = MutableStateFlow<NativeAd?>(null)
    val nativeAd: StateFlow<NativeAd?> = _nativeAd

    fun initialize() {
        if (_isInitialized.value) return
        MobileAds.initialize(context) {
            _isInitialized.value = true
            Log.d(TAG, "MobileAds initialized")
            loadNativeAd()
        }
    }

    fun loadNativeAd() {
        retryRunnable?.let { retryHandler.removeCallbacks(it) }
        AdLoader.Builder(context, NATIVE_AD_UNIT_ID)
            .forNativeAd { ad ->
                _nativeAd.value?.destroy()
                _nativeAd.value = ad
                retryRunnable = null
                Log.d(TAG, "Native ad loaded")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load: ${error.message}. Code: ${error.code}")
                    retryRunnable = Runnable { loadNativeAd() }.also {
                        retryHandler.postDelayed(it, 30_000L)
                    }
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    fun destroyNativeAd() {
        retryRunnable?.let { retryHandler.removeCallbacks(it) }
        retryRunnable = null
        _nativeAd.value?.destroy()
        _nativeAd.value = null
    }
}
