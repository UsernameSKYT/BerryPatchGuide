package com.berry.patchguide.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.berry.patchguide.ui.apply.ApplyPatchScreen
import com.berry.patchguide.ui.guide.GuideScreen
import com.berry.patchguide.ui.home.HomeScreen
import com.berry.patchguide.ui.library.LibraryScreen
import com.berry.patchguide.ui.modinstall.ModInstallScreen
import com.berry.patchguide.ui.navigation.AppDestination
import com.berry.patchguide.ui.payment.PaymentScreen
import com.berry.patchguide.ui.search.SearchScreen
import com.berry.patchguide.ui.settings.SettingsScreen

@Composable
fun MainScreen(sharedPatchUri: Uri? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in AppDestination.bottomNavEntries.map { it.route }

    // 외부 공유 인텐트가 있으면 ApplyPatch 화면으로 바로 이동
    LaunchedEffect(sharedPatchUri) {
        if (sharedPatchUri != null) {
            navController.navigate(
                AppDestination.ApplyPatch.createRoute(
                    patchId = "shared",
                    patchTitle = "공유된 패치 파일",
                    downloadUrl = ""
                )
            ) {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    AppDestination.bottomNavEntries.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            icon = { Icon(destination.icon, contentDescription = stringResource(destination.labelRes)) },
                            label = { Text(stringResource(destination.labelRes)) },
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.Home.route) {
                HomeScreen(
                    onNavigateToApply = { patchId, patchTitle, downloadUrl ->
                        navController.navigate(
                            AppDestination.ApplyPatch.createRoute(patchId, patchTitle, downloadUrl)
                        )
                    },
                    onNavigateToModInstall = {
                        navController.navigate(AppDestination.ModInstall.route)
                    }
                )
            }
            composable(AppDestination.Search.route) {
                SearchScreen(
                    onNavigateToApply = { patchId, patchTitle, downloadUrl ->
                        navController.navigate(
                            AppDestination.ApplyPatch.createRoute(patchId, patchTitle, downloadUrl)
                        )
                    }
                )
            }
            composable(AppDestination.Library.route) { LibraryScreen() }
            composable(AppDestination.Settings.route) {
                SettingsScreen(
                    onNavigateToPayment = {
                        navController.navigate(AppDestination.Payment.route)
                    }
                )
            }
            composable(AppDestination.Payment.route) { PaymentScreen() }
            composable(AppDestination.ModInstall.route) {
                ModInstallScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "apply/{patchId}?title={title}&url={url}",
                arguments = listOf(
                    navArgument("patchId") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("url") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val patchId = backStackEntry.arguments?.getString("patchId") ?: ""
                val patchTitle = backStackEntry.arguments?.getString("title") ?: ""
                val downloadUrl = backStackEntry.arguments?.getString("url") ?: ""
                ApplyPatchScreen(
                    patchId = patchId,
                    patchTitle = patchTitle,
                    downloadUrl = downloadUrl.ifBlank { null },
                    sharedPatchUri = if (patchId == "shared") sharedPatchUri else null,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToGuide = { outputPath, appliedFormat ->
                        navController.navigate(
                            AppDestination.Guide.createRoute(outputPath, appliedFormat)
                        )
                    }
                )
            }
            composable(
                route = "guide?outputPath={outputPath}&format={format}",
                arguments = listOf(
                    navArgument("outputPath") { type = NavType.StringType; defaultValue = "" },
                    navArgument("format") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val outputPath = backStackEntry.arguments?.getString("outputPath") ?: ""
                val format = backStackEntry.arguments?.getString("format") ?: ""
                GuideScreen(
                    outputPath = outputPath.ifBlank { null },
                    appliedFormat = format.ifBlank { null },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
