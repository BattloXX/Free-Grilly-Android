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
    val rows = remember(ui) { DeviceStatusRows.from(ui) }
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

            val identityRows = rows.filter { it.kind in setOf(
                DeviceStatusRow.Kind.NAME, DeviceStatusRow.Kind.FIRMWARE_NAME,
                DeviceStatusRow.Kind.FIRMWARE_VERSION, DeviceStatusRow.Kind.API_VERSION,
                DeviceStatusRow.Kind.UUID, DeviceStatusRow.Kind.MDNS_HOSTNAME,
            ) }
            StatusCard(stringResource(R.string.status_section_identity)) {
                StatusRow(stringResource(R.string.status_state), state)
                identityRows.forEach { StatusRow(statusLabel(it.kind), it.value) }
            }

            val networkRows = rows.filter { it.kind in setOf(
                DeviceStatusRow.Kind.IP_ADDRESS, DeviceStatusRow.Kind.WIFI_CONNECTED, DeviceStatusRow.Kind.WIFI_RSSI,
            ) }
            if (networkRows.isNotEmpty()) StatusCard(stringResource(R.string.status_section_network)) {
                networkRows.forEach { StatusRow(statusLabel(it.kind), statusValue(it)) }
            }

            val powerRows = rows.filter { it.kind in setOf(
                DeviceStatusRow.Kind.BATTERY_PERCENT, DeviceStatusRow.Kind.BATTERY_VOLTAGE,
                DeviceStatusRow.Kind.BATTERY_CHARGING,
            ) }
            if (powerRows.isNotEmpty()) StatusCard(stringResource(R.string.status_section_power)) {
                powerRows.forEach { StatusRow(statusLabel(it.kind), statusValue(it)) }
            }

            // Diagnostics: why the device last reset / powered off. Only shown when the
            // firmware reports it (older firmware leaves these blank).
            val diagnosticsRows = rows.filter { it.kind in setOf(
                DeviceStatusRow.Kind.LAST_RESET_REASON, DeviceStatusRow.Kind.LAST_OFF_REASON,
                DeviceStatusRow.Kind.UPTIME,
            ) }
            if (diagnosticsRows.isNotEmpty()) {
                StatusCard(stringResource(R.string.status_section_diagnostics)) {
                    diagnosticsRows.forEach { StatusRow(statusLabel(it.kind), statusValue(it)) }
                }
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

@Composable
private fun statusLabel(kind: DeviceStatusRow.Kind): String = when (kind) {
    DeviceStatusRow.Kind.NAME -> stringResource(R.string.status_name)
    DeviceStatusRow.Kind.FIRMWARE_NAME -> stringResource(R.string.status_firmware_name)
    DeviceStatusRow.Kind.FIRMWARE_VERSION -> stringResource(R.string.status_firmware_version)
    DeviceStatusRow.Kind.API_VERSION -> stringResource(R.string.status_api_version)
    DeviceStatusRow.Kind.UUID -> stringResource(R.string.status_uuid)
    DeviceStatusRow.Kind.MDNS_HOSTNAME -> stringResource(R.string.status_mdns)
    DeviceStatusRow.Kind.IP_ADDRESS -> stringResource(R.string.status_ip)
    DeviceStatusRow.Kind.WIFI_CONNECTED -> stringResource(R.string.status_wifi)
    DeviceStatusRow.Kind.WIFI_RSSI -> stringResource(R.string.status_wifi_signal)
    DeviceStatusRow.Kind.BATTERY_PERCENT -> stringResource(R.string.status_battery)
    DeviceStatusRow.Kind.BATTERY_VOLTAGE -> stringResource(R.string.status_battery_voltage)
    DeviceStatusRow.Kind.BATTERY_CHARGING -> stringResource(R.string.status_charging)
    DeviceStatusRow.Kind.LAST_RESET_REASON -> stringResource(R.string.status_last_reset)
    DeviceStatusRow.Kind.LAST_OFF_REASON -> stringResource(R.string.status_last_off)
    DeviceStatusRow.Kind.UPTIME -> stringResource(R.string.status_uptime)
}

@Composable
private fun statusValue(row: DeviceStatusRow): String = when (row.kind) {
    DeviceStatusRow.Kind.WIFI_CONNECTED -> if (row.value.toBoolean()) stringResource(R.string.status_connected) else stringResource(R.string.disconnected)
    DeviceStatusRow.Kind.WIFI_RSSI -> "${row.value} dBm (${wifiPercent(row.value.toInt())}%)"
    DeviceStatusRow.Kind.BATTERY_PERCENT -> "${row.value}%"
    DeviceStatusRow.Kind.BATTERY_VOLTAGE -> "%.2f V".format(row.value.toInt() / 1000f)
    DeviceStatusRow.Kind.BATTERY_CHARGING -> if (row.value.toBoolean()) stringResource(R.string.status_charging) else stringResource(R.string.status_not_charging)
    DeviceStatusRow.Kind.LAST_RESET_REASON -> resetReasonText(row.value).orEmpty()
    DeviceStatusRow.Kind.LAST_OFF_REASON -> offReasonText(row.value).orEmpty()
    else -> row.value
}

/** Localized label for a firmware `last_reset_reason` code, or null when unknown/blank. */
@Composable
private fun resetReasonText(code: String): String? = when (code) {
    "poweron" -> stringResource(R.string.reset_poweron)
    "deepsleep" -> stringResource(R.string.reset_deepsleep)
    "brownout" -> stringResource(R.string.reset_brownout)
    "panic" -> stringResource(R.string.reset_panic)
    "int_wdt", "task_wdt", "wdt" -> stringResource(R.string.reset_watchdog)
    "sw" -> stringResource(R.string.reset_sw)
    "factory_reset" -> stringResource(R.string.reset_factory_reset)
    "" -> null
    // Unknown/other codes: show the raw value rather than hiding it (still useful info).
    else -> code
}

/** Localized label for a firmware `last_off_reason` code, or null when unknown/blank. */
@Composable
private fun offReasonText(code: String): String? = when (code) {
    "button" -> stringResource(R.string.off_button)
    "low_battery" -> stringResource(R.string.off_low_battery)
    "boot_gate" -> stringResource(R.string.off_boot_gate)
    "update" -> stringResource(R.string.off_update)
    "factory_reset" -> stringResource(R.string.off_factory_reset)
    "" -> null
    else -> code
}
