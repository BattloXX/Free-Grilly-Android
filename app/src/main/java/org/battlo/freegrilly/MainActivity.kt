package org.battlo.freegrilly

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.pm.PackageManager
import dagger.hilt.android.AndroidEntryPoint
import org.battlo.freegrilly.ui.MainScreen
import org.battlo.freegrilly.ui.MainViewModel
import org.battlo.freegrilly.ui.theme.FreeGrillyTheme
import org.battlo.freegrilly.util.Permissions

// AppCompatActivity (not ComponentActivity) is required for AppCompatDelegate.setApplicationLocales()
// to actually take effect: AppCompat only rewrites attachBaseContext()'s Resources — and
// auto-recreates on locale change — for its own Activity subclass. On a plain ComponentActivity,
// setApplicationLocales() persists the preference but never changes what strings resolve to.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Registered before STARTED (field initializer) as required by the Activity Result API.
    // We don't gate the UI on the outcome — discovery simply works once Nearby-Wi-Fi is granted.
    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* no-op */ }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRuntimePermissions()
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

    /** Request the dangerous permissions NSD discovery and alarm notifications need (API 33+). */
    private fun requestRuntimePermissions() {
        val toRequest = Permissions.startupRequests().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) requestPermissions.launch(toRequest.toTypedArray())
    }
}
