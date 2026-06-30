package org.battlo.freegrilly.ui.status

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.battlo.freegrilly.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceStatusScreen(
    onBack: () -> Unit,
    viewModel: DeviceStatusViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val dash = stringResource(R.string.status_value_unknown)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.status_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.status_refresh))
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val state = when {
                ui.demo -> stringResource(R.string.status_demo)
                ui.connected -> stringResource(R.string.status_connected)
                else -> stringResource(R.string.disconnected)
            }

            StatusCard(stringResource(R.string.status_section_identity)) {
                StatusRow(stringResource(R.string.status_state), state)
                StatusRow(stringResource(R.string.status_name), ui.name.ifBlank { dash })
                StatusRow(stringResource(R.string.status_firmware), ui.firmware.ifBlank { dash })
                StatusRow(stringResource(R.string.status_uuid), ui.uuid.ifBlank { dash })
                StatusRow(stringResource(R.string.status_mdns), ui.mdnsHostname.ifBlank { dash })
            }

            StatusCard(stringResource(R.string.status_section_network)) {
                StatusRow(stringResource(R.string.status_ip), ui.ipAddress.ifBlank { dash })
                StatusRow(
                    stringResource(R.string.status_wifi),
                    if (ui.wifiConnected) stringResource(R.string.status_connected)
                    else stringResource(R.string.disconnected),
                )
                StatusRow(
                    stringResource(R.string.status_wifi_signal),
                    if (ui.connected) "${ui.wifiSignalDbm} dBm (${wifiPercent(ui.wifiSignalDbm)}%)" else dash,
                )
            }

            StatusCard(stringResource(R.string.status_section_power)) {
                StatusRow(
                    stringResource(R.string.status_battery),
                    if (ui.connected) {
                        val charging = if (ui.batteryCharging) " · ${stringResource(R.string.status_charging)}" else ""
                        "${ui.batteryPercent}%$charging"
                    } else dash,
                )
            }

            StatusCard(stringResource(R.string.status_section_probes)) {
                StatusRow(
                    stringResource(R.string.status_probes_connected),
                    if (ui.connected) "${ui.probesConnected} / ${ui.probesTotal}" else dash,
                )
                StatusRow(
                    stringResource(R.string.status_unit),
                    if (ui.temperatureUnit == "fahrenheit") "°F" else "°C",
                )
            }

            if (ui.capabilities.isNotEmpty()) {
                StatusCard(stringResource(R.string.status_section_capabilities)) {
                    Text(
                        ui.capabilities.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f),
        )
    }
}

/** dBm → rough percentage per the firmware API guide: percent = 140 + dBm, clamped 0–100. */
private fun wifiPercent(dbm: Int): Int = (140 + dbm).coerceIn(0, 100)
