package com.lulucloud.touchscript.navigation

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lulucloud.touchscript.app.AppViewModelFactory
import com.lulucloud.touchscript.feature.editor.EditorScreen
import com.lulucloud.touchscript.feature.editor.EditorViewModel
import com.lulucloud.touchscript.feature.home.HomeScreen
import com.lulucloud.touchscript.feature.home.HomeViewModel
import com.lulucloud.touchscript.feature.settings.SettingsScreen

@Composable
fun TouchWorkshopApp(
    appViewModelFactory: AppViewModelFactory
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val destinations = listOf(
        AppDestination.Home,
        AppDestination.Editor,
        AppDestination.Settings
    )

    Scaffold(
        bottomBar = {
            Surface(
                tonalElevation = 0.dp,
                shadowElevation = 14.dp
            ) {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = Color.Transparent
                ) {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(destination.icon, contentDescription = destination.label)
                            },
                            label = { Text(destination.label) }
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
                val viewModel: HomeViewModel = viewModel(factory = appViewModelFactory)
                HomeScreen(viewModel = viewModel, context = context)
            }
            composable(AppDestination.Editor.route) {
                val viewModel: EditorViewModel = viewModel(factory = appViewModelFactory)
                EditorScreen(viewModel = viewModel)
            }
            composable(AppDestination.Settings.route) {
                SettingsScreen(context = context)
            }
        }
    }
}

private sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : AppDestination("home", "首页", Icons.Outlined.Home)
    data object Editor : AppDestination("editor", "编辑器", Icons.AutoMirrored.Outlined.Article)
    data object Settings : AppDestination("settings", "设置", Icons.Outlined.Settings)
}
