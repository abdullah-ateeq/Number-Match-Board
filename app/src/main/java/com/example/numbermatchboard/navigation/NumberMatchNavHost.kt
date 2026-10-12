package com.example.numbermatchboard.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.numbermatchboard.ui.NumberMatchViewModel
import com.example.numbermatchboard.ui.screens.AboutScreen
import com.example.numbermatchboard.ui.screens.GameScreen
import com.example.numbermatchboard.ui.screens.HomeScreen
import com.example.numbermatchboard.ui.screens.ResultScreen
import com.example.numbermatchboard.ui.screens.SettingsScreen
import com.example.numbermatchboard.ui.screens.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val GAME = "game"
    const val RESULT = "result"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

private data class TopLevelDestination(val route: String, val label: String, val icon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, "Home", Icons.Filled.Home),
    TopLevelDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
    TopLevelDestination(Routes.ABOUT, "About", Icons.Filled.Info)
)

/** Navigates only if [from] is the resumed destination, which drops double taps mid-transition. */
private fun NavHostController.navigateFrom(
    from: NavBackStackEntry,
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    if (from.lifecycle.currentState == Lifecycle.State.RESUMED) navigate(route, builder)
}

private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun NumberMatchNavHost(viewModel: NumberMatchViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in topLevelDestinations.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute != destination.route) navController.navigateToTopLevel(destination.route)
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.fillMaxSize().padding(padding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(Routes.SPLASH) { entry ->
                SplashScreen(
                    viewModel = viewModel,
                    onFinished = {
                        navController.navigateFrom(entry, Routes.HOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME) { entry ->
                HomeScreen(
                    viewModel = viewModel,
                    onPlay = {
                        if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                            viewModel.startNewGame()
                            navController.navigate(Routes.GAME)
                        }
                    },
                    onOpenSettings = { navController.navigateToTopLevel(Routes.SETTINGS) },
                    onOpenAbout = { navController.navigateToTopLevel(Routes.ABOUT) }
                )
            }
            composable(Routes.GAME) { entry ->
                GameScreen(
                    viewModel = viewModel,
                    onExit = {
                        if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) navController.popBackStack()
                    },
                    onGameFinished = {
                        navController.navigateFrom(entry, Routes.RESULT) {
                            popUpTo(Routes.GAME) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.RESULT) { entry ->
                ResultScreen(
                    viewModel = viewModel,
                    onPlayAgain = {
                        if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                            viewModel.startNewGame()
                            navController.navigate(Routes.GAME) {
                                popUpTo(Routes.RESULT) { inclusive = true }
                            }
                        }
                    },
                    onHome = {
                        if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                            if (!navController.popBackStack(Routes.HOME, inclusive = false)) {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                )
            }
            composable(Routes.SETTINGS) { SettingsScreen(viewModel = viewModel) }
            composable(Routes.ABOUT) { AboutScreen() }
        }
    }
}
