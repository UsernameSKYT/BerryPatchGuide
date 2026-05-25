package com.berry.patchguide.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
) {
    data object Home : AppDestination("home", com.berry.patchguide.R.string.tab_home, Icons.Default.Home)
    data object Search : AppDestination("search", com.berry.patchguide.R.string.tab_search, Icons.Default.Search)
    data object Library : AppDestination("library", com.berry.patchguide.R.string.tab_library, Icons.Default.Bookmarks)
    data object Settings : AppDestination("settings", com.berry.patchguide.R.string.tab_settings, Icons.Default.Settings)
    data object Payment : AppDestination("payment", com.berry.patchguide.R.string.tab_payment, Icons.Default.Settings)
    data object ApplyPatch : AppDestination("apply/{patchId}", com.berry.patchguide.R.string.tab_home, Icons.Default.Home) {
        fun createRoute(patchId: String, patchTitle: String = "", downloadUrl: String = "") =
            "apply/$patchId?title=${Uri.encode(patchTitle)}&url=${Uri.encode(downloadUrl)}"
    }

    companion object {
        val bottomNavEntries = listOf(Home, Search, Library, Settings)
    }
}
