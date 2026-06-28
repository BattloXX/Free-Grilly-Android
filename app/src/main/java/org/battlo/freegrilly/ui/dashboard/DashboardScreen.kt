package org.battlo.freegrilly.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
                        Button(onClick = { viewModel.muteAlarm() }) { Text(stringResource(R.string.retry)) }
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

                TopAppBar(
                    title = { Text(status?.name ?: "Free-Grilly Demo") },
                    actions = {
                        if (status != null) {
                            BatteryBadge(
                                batteryPercent = status.batteryPercentage,
                                isCharging = status.batteryCharging,
                                wifiDbm = status.wifiSignal,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                        }
                    },
                )

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
