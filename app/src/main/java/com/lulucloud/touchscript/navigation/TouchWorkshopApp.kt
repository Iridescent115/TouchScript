package com.lulucloud.touchscript.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lulucloud.touchscript.app.AppViewModelFactory
import com.lulucloud.touchscript.feature.editor.EditorScreen
import com.lulucloud.touchscript.feature.editor.EditorViewModel
import com.lulucloud.touchscript.feature.runner.RunnerScreen
import com.lulucloud.touchscript.feature.runner.RunnerViewModel

@Composable
fun TouchWorkshopApp(
    appViewModelFactory: AppViewModelFactory
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val destinations = listOf(
        AppDestination.Editor,
        AppDestination.Runner
    )
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = { navController.navigate(destination.route) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Editor.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.Editor.route) {
                val viewModel: EditorViewModel = viewModel(factory = appViewModelFactory)
                EditorScreen(viewModel = viewModel, context = context)
            }
            composable(AppDestination.Runner.route) {
                val viewModel: RunnerViewModel = viewModel(factory = appViewModelFactory)
                RunnerScreen(viewModel = viewModel, context = context)
            }
        }
    }
}

private sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    data object Editor : AppDestination(
        route = "editor",
        label = "编辑器",
        icon = Icons.AutoMirrored.Outlined.Article
    )

    data object Runner : AppDestination(
        route = "runner",
        label = "执行",
        icon = Icons.Outlined.PlayCircle
    )
}
