package org.battlo.freegrilly.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.battlo.freegrilly.R
import org.battlo.freegrilly.data.GrillyUiState
import org.battlo.freegrilly.ui.components.AlarmBanner
import org.battlo.freegrilly.ui.components.BatteryBadge
import org.battlo.freegrilly.ui.components.ProbeCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onProbeClick: (Int) -> Unit,
    onNavigateToOnboarding: () -> Unit,
    /** §8 — In-app device switch: navigate to device selector to choose a different device. */
    onNavigateToDeviceSelector: (() -> Unit)? = null,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unit by viewModel.unit.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is GrillyUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.connecting))
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = { viewModel.enableDemoMode() }) {
                            Text(stringResource(R.string.use_demo_mode))
                        }
                    }
                }
            }

            is GrillyUiState.Disconnected -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.disconnected), style = MaterialTheme.typography.headlineSmall)
                        Button(onClick = { viewModel.reconnect() }) { Text(stringResource(R.string.reconnect)) }
                        OutlinedButton(onClick = onNavigateToOnboarding) { Text(stringResource(R.string.add_device)) }
                        TextButton(onClick = { viewModel.enableDemoMode() }) { Text(stringResource(R.string.use_demo_mode)) }
                    }
                }
            }

            is GrillyUiState.Demo, is GrillyUiState.Connected -> {
                val status = (state as? GrillyUiState.Connected)?.status
                val history = (state as? GrillyUiState.Connected)?.history ?: emptyMap()

                if (status?.alarmActive == true) {
                    val alarmProbe = status.probes.firstOrNull { it.alarm }
                    AlarmBanner(
                        probeName = alarmProbe?.name ?: "Sonde",
                        onMute = { viewModel.muteAlarm() },
                    )
                }

                // Compact custom header (lighter than a full-height M3 TopAppBar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_grilly),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        status?.name ?: "Free-Grilly Demo",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    // §8 — Switch device icon (only shown when multiple devices might exist)
                    if (onNavigateToDeviceSelector != null) {
                        IconButton(onClick = onNavigateToDeviceSelector) {
                            Icon(
                                Icons.Default.DeviceHub,
                                contentDescription = stringResource(R.string.switch_device),
                            )
                        }
                    }
                    if (status != null) {
                        BatteryBadge(
                            batteryPercent = status.batteryPercentage,
                            isCharging = status.batteryCharging,
                            wifiDbm = status.wifiSignal,
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                        )
                    }
                }

                val probes = status?.probes ?: emptyList()
                if (probes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(probes) { probe ->
                            ProbeCard(
                                probe = probe,
                                unit = unit,
                                historyData = history[probe.id] ?: emptyList(),
                                onClick = { if (probe.connected) onProbeClick(probe.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
