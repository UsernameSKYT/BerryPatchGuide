package com.berry.patchguide

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.berry.patchguide.ui.MainScreen
import com.berry.patchguide.ui.theme.BerryPatchGuideTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 외부 공유 인텐트로 전달된 URI (null이면 일반 실행)
    private var sharedPatchUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedPatchUri = resolveSharedUri(intent)
        setContent {
            BerryPatchGuideTheme {
                MainScreen(sharedPatchUri = sharedPatchUri)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTop 재진입 시에도 공유 인텐트 처리
        sharedPatchUri = resolveSharedUri(intent)
    }

    private fun resolveSharedUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }
    }
}
