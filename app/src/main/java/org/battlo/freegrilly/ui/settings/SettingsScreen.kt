package org.battlo.freegrilly.ui.settings

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.battlo.freegrilly.R
import org.battlo.freegrilly.ui.components.CompactHeader
import org.battlo.freegrilly.ui.update.DeviceOtaSection
import org.battlo.freegrilly.ui.update.UpdateSection
import org.battlo.freegrilly.ui.update.UpdateViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToStatus: () -> Unit = {},
    onNavigateToDeviceSelector: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val unit by viewModel.unit.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val isDemoMode by viewModel.isDemoMode.collectAsStateWithLifecycle()
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()
    val supportsPowerSaving by viewModel.supportsPowerSaving.collectAsStateWithLifecycle()
    val powerSaving by viewModel.powerSaving.collectAsStateWithLifecycle()

    // Load current power-saving state from device when screen enters composition
    LaunchedEffect(supportsPowerSaving) {
        if (supportsPowerSaving) viewModel.loadPowerSavingState()
    }

    Column(Modifier.fillMaxSize()) {
        CompactHeader(stringResource(R.string.nav_settings), onBack = onBack)
        Column(
            modifier = Modifier
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
                // Recreating the activity is required for the new locale's strings to take
                // effect immediately (MainActivity is a plain ComponentActivity, so unlike
                // AppCompatActivity it isn't auto-recreated by AppCompat on language change).
                FilterChip(
                    selected = language == "system",
                    onClick = { viewModel.setLanguage("system"); (context as? Activity)?.recreate() },
                    label = { Text(stringResource(R.string.lang_system)) },
                )
                FilterChip(
                    selected = language == "de",
                    onClick = { viewModel.setLanguage("de"); (context as? Activity)?.recreate() },
                    label = { Text("Deutsch") },
                )
                FilterChip(
                    selected = language == "en",
                    onClick = { viewModel.setLanguage("en"); (context as? Activity)?.recreate() },
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
                    onClick = onNavigateToStatus,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.settings_device_status)) }
                // Connect to a Grilly already on the Wi-Fi (mDNS scan / manual IP) — the
                // DeviceSelector handles both. This is the path for adopting an existing device
                // without going through AP re-provisioning.
                OutlinedButton(
                    onClick = onNavigateToDeviceSelector,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.connect_existing_device)) }
                OutlinedButton(
                    onClick = onNavigateToOnboarding,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.add_new_device)) }
            }

            // §8 — Power-saving (only visible when device declares power_saving capability)
            if (supportsPowerSaving) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    stringResource(R.string.settings_power_saving),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.power_saving_label),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            stringResource(R.string.power_saving_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = powerSaving ?: false,
                        onCheckedChange = { viewModel.setPowerSaving(it) },
                        enabled = powerSaving != null,
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            UpdateSection(
                state = updateState,
                currentVersion = updateViewModel.currentVersion,
                onCheckUpdate = { updateViewModel.checkForUpdate() },
                onDownload = {
                    val info = (updateState as? org.battlo.freegrilly.ui.update.UpdateState.Available)?.info
                    if (info != null) updateViewModel.startDownload(info)
                },
                onInstall = { downloadId -> updateViewModel.installUpdate(downloadId) },
                onDismiss = { updateViewModel.dismissError() },
            )

            // §8 — Device OTA firmware update (only visible when device declares ota capability)
            DeviceOtaSection()
        }
    }
}
