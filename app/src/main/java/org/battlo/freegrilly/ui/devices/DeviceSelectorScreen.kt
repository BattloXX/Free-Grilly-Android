package org.battlo.freegrilly.ui.devices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.battlo.freegrilly.R
import org.battlo.freegrilly.data.DiscoveryState
import org.battlo.freegrilly.data.KnownDevice
import org.battlo.freegrilly.ui.theme.LocalGrillyColors
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelectorScreen(
    onDeviceConnected: () -> Unit,
    onAddNewDevice: () -> Unit,
    viewModel: DeviceSelectorViewModel = hiltViewModel(),
) {
    val devices by viewModel.knownDevices.collectAsStateWithLifecycle()
    val connectingUuid by viewModel.connectingUuid.collectAsStateWithLifecycle()
    val connectError by viewModel.connectError.collectAsStateWithLifecycle()
    val discoveryState by viewModel.discoveryState.collectAsStateWithLifecycle()
    val colors = LocalGrillyColors.current

    var showManualDialog by remember { mutableStateOf(false) }
    var showScanFab by remember { mutableStateOf(false) }

    LaunchedEffect(discoveryState) {
        if (discoveryState is DiscoveryState.Found) {
            val found = discoveryState as DiscoveryState.Found
            // Auto-add newly discovered device
            viewModel.connectManualIp(found.ip, onDeviceConnected)
        }
    }

    if (showManualDialog) {
        ManualIpDialog(
            isConnecting = connectingUuid == "manual",
            onConnect = { ip ->
                showManualDialog = false
                viewModel.connectManualIp(ip, onDeviceConnected)
            },
            onDismiss = { showManualDialog = false },
        )
    }

    connectError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text(stringResource(R.string.connection_failed)) },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) { Text(stringResource(R.string.ok)) }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.select_device_title)) })
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (showScanFab) {
                    SmallFloatingActionButton(
                        onClick = {
                            showScanFab = false
                            viewModel.startNetworkScan()
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Icon(Icons.Default.NetworkWifi, contentDescription = null)
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            showScanFab = false
                            showManualDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
                FloatingActionButton(
                    onClick = { showScanFab = !showScanFab },
                    containerColor = colors.emberOrange,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            if (discoveryState is DiscoveryState.Searching) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            stringResource(R.string.scanning_network),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { viewModel.stopNetworkScan() }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }

            items(devices, key = { it.uuid }) { device ->
                DeviceCard(
                    device = device,
                    isConnecting = connectingUuid == device.uuid,
                    onConnect = { viewModel.connectDevice(device, onDeviceConnected) },
                    onDelete = { viewModel.removeDevice(device.uuid) },
                )
            }

            item {
                OutlinedButton(
                    onClick = onAddNewDevice,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.add_new_device))
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: KnownDevice,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalGrillyColors.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.remove_device_confirm_title)) },
            text = { Text(stringResource(R.string.remove_device_confirm_body, device.name)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(device.name, style = MaterialTheme.typography.titleMedium)
                    FirmwareBadge(device = device)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    device.ip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (device.lastSeen > 0L) {
                    Text(
                        stringResource(
                            R.string.last_seen,
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                .format(Date(device.lastSeen)),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (device.firmwareVersion.isNotBlank()) {
                    Text(
                        "FW ${device.firmwareVersion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.emberOrange),
                    ) {
                        Text(stringResource(R.string.connect))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FirmwareBadge(device: KnownDevice) {
    val (label, containerColor) = when {
        device.capabilities.isNotEmpty() ->
            "Free-Grilly" to LocalGrillyColors.current.emberOrange.copy(alpha = 0.2f)
        device.firmwareVersion.isNotEmpty() ->
            "Grilleye" to MaterialTheme.colorScheme.surfaceVariant
        else -> return
    }
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = containerColor,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun ManualIpDialog(
    isConnecting: Boolean,
    onConnect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var ip by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_ip_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.manual_ip_hint))
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it.trim() },
                    label = { Text("IP-Adresse") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    placeholder = { Text("192.168.1.100") },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (ip.isNotBlank()) onConnect(ip) },
                enabled = ip.isNotBlank() && !isConnecting,
            ) {
                if (isConnecting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.connect))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
