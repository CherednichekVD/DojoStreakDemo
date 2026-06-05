package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.theme.AppTheme
import com.example.ui.viewmodels.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val settings by viewModel.userSettings.collectAsState()
                    val gyms by viewModel.allGyms.collectAsState()
                    
                    val navController = rememberNavController()

                    // Simple routing based on settings
                    val startDestination = if (settings?.selectedGymId != null) "home" else "setup"

                    if (settings != null || gyms.isNotEmpty()) {
                        NavHost(navController = navController, startDestination = startDestination) {
                            composable("setup") {
                                SetupScreen(
                                    viewModel = viewModel,
                                    gyms = gyms,
                                    onSetupComplete = {
                                        navController.navigate("home") {
                                            popUpTo(0) // clear backstack
                                        }
                                    }
                                )
                            }
                            composable("home") {
                                if (settings != null) {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        userSettings = settings!!,
                                        onNavigateToSetup = {
                                            navController.navigate("setup")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
