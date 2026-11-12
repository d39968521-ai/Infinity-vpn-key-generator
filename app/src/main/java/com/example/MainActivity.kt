package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.MainViewModel
import com.example.ui.Routes
import com.example.ui.detail.DetailScreen
import com.example.ui.home.HomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val currentColorTheme by viewModel.colorTheme.collectAsStateWithLifecycle()
            val currentDeviceMode by viewModel.deviceLayoutMode.collectAsStateWithLifecycle()
            val context = androidx.compose.ui.platform.LocalContext.current
            val isTv = com.example.util.DeviceTypeHelper.shouldShowTvLayout(context, currentDeviceMode)

            MyApplicationTheme(
                themeMode = currentThemeMode,
                colorTheme = currentColorTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = Routes.HOME) {
                        composable(Routes.HOME) {
                            if (isTv) {
                                com.example.ui.tv.TvHomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToDetail = { keyId ->
                                        navController.navigate(Routes.detailRoute(keyId))
                                    }
                                )
                            } else {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToDetail = { keyId -> 
                                        navController.navigate(Routes.detailRoute(keyId)) 
                                    }
                                )
                            }
                        }
                        composable(
                            route = Routes.DETAIL,
                            arguments = listOf(navArgument("keyId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val keyId = backStackEntry.arguments?.getString("keyId") ?: return@composable
                            DetailScreen(
                                keyId = keyId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
