package org.battlo.freegrilly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.battlo.freegrilly.ui.MainScreen
import org.battlo.freegrilly.ui.MainViewModel
import org.battlo.freegrilly.ui.theme.FreeGrillyTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FreeGrillyTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                val viewModel: MainViewModel = hiltViewModel()
                val startDestination by viewModel.startDestination.collectAsState()
                MainScreen(
                    windowSizeClass = windowSizeClass,
                    startDestination = startDestination,
                )
            }
        }
    }
}
