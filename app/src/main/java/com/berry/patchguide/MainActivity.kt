package com.berry.patchguide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.berry.patchguide.ui.MainScreen
import com.berry.patchguide.ui.theme.BerryPatchGuideTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BerryPatchGuideTheme {
                MainScreen()
            }
        }
    }
}
