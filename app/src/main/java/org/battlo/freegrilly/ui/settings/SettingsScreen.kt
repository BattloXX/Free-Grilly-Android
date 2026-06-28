package org.battlo.freegrilly.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.battlo.freegrilly.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val unit by viewModel.unit.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val isDemoMode by viewModel.isDemoMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.settings_unit),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = unit == "celcius",
                    onClick = { viewModel.setUnit("celcius") },
                    label = { Text("°C (Celsius)") },
                )
                FilterChip(
                    selected = unit == "fahrenheit",
                    onClick = { viewModel.setUnit("fahrenheit") },
                    label = { Text("°F (Fahrenheit)") },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(
                stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = language == "system",
                    onClick = { viewModel.setLanguage("system") },
                    label = { Text(stringResource(R.string.lang_system)) },
                )
                FilterChip(
                    selected = language == "de",
                    onClick = { viewModel.setLanguage("de") },
                    label = { Text("Deutsch") },
                )
                FilterChip(
                    selected = language == "en",
                    onClick = { viewModel.setLanguage("en") },
                    label = { Text("English") },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(
                stringResource(R.string.settings_device),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            if (isDemoMode) {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.demo_mode_active))
                        Button(onClick = { viewModel.exitDemoMode(); onNavigateToOnboarding() }) {
                            Text(stringResource(R.string.exit_demo_mode))
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onNavigateToOnboarding,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.add_new_device)) }
            }
        }
    }
}
