package org.battlo.freegrilly.ui.probedetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.battlo.freegrilly.R
import org.battlo.freegrilly.domain.EtaFormatter
import org.battlo.freegrilly.domain.TempUtils
import org.battlo.freegrilly.ui.components.Sparkline
import org.battlo.freegrilly.ui.theme.LocalGrillyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProbeDetailScreen(
    probeId: Int,
    onBack: () -> Unit,
    onOpenLibrary: (Int) -> Unit,
    viewModel: ProbeDetailViewModel = hiltViewModel(),
) {
    val probe by viewModel.probe.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val unit by viewModel.unit.collectAsStateWithLifecycle()
    val colors = LocalGrillyColors.current
    var showTargetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(probe?.name ?: "Sonde $probeId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (probe?.alarm == true) {
                        IconButton(onClick = { viewModel.muteAlarm() }) {
                            Icon(Icons.Default.NotificationsOff, contentDescription = null)
                        }
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
            probe?.let { p ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(
                        Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            TempUtils.format(p.temperature, unit, 1),
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (p.alarm) colors.criticalRed else colors.emberOrange,
                        )
                        Text(
                            "→ ${TempUtils.format(p.targetTemperature, unit, 0)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (p.etaSeconds >= 0) {
                            Text(
                                EtaFormatter.format(p.etaSeconds),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.successGreen,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                if (history.size > 2) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.temperature_history), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            Sparkline(
                                data = history,
                                targetTemp = p.targetTemperature,
                                color = if (p.alarm) colors.criticalRed else colors.emberOrange,
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                strokeWidth = 2f,
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showTargetDialog = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.edit_target))
                    }
                    Button(onClick = { onOpenLibrary(probeId) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.MenuBook, null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.assign_food))
                    }
                }
            } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    if (showTargetDialog) {
        var targetInput by remember { mutableStateOf(probe?.targetTemperature?.toInt()?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text(stringResource(R.string.edit_target)) },
            text = {
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) targetInput = it },
                    label = { Text("Zieltemperatur (°C)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                Button(onClick = {
                    targetInput.toFloatOrNull()?.let { viewModel.setTarget(it) }
                    showTargetDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
