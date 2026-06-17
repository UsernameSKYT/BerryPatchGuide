package com.berry.patchguide

import android.app.Application
import com.berry.patchguide.data.ads.AdManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BerryPatchGuideApp : Application() {

    @Inject
    lateinit var adManager: AdManager

    override fun onCreate() {
        super.onCreate()
        adManager.initialize()
    }
}
