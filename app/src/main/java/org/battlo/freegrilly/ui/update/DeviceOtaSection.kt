package org.battlo.freegrilly.ui.update

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.battlo.freegrilly.R
import org.battlo.freegrilly.ui.theme.LocalGrillyColors
import org.battlo.freegrilly.ui.update.DeviceOtaViewModel.OtaState

/**
 * §8 — Device OTA firmware update section for SettingsScreen.
 *
 * Only rendered when the device reports the `ota` capability.
 * All state is managed by [DeviceOtaViewModel].
 */
@Composable
fun DeviceOtaSection(
    modifier: Modifier = Modifier,
    viewModel: DeviceOtaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val supportsOta by viewModel.supportsOta.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val colors = LocalGrillyColors.current

    if (!supportsOta) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = colors.emberOrange,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                stringResource(R.string.settings_device_ota),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        when (val s = state) {
            is OtaState.Idle -> {
                Button(
                    onClick = { viewModel.checkForFirmwareUpdate() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.emberOrange),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.check_firmware_update))
                }
            }

            is OtaState.Checking -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.checking_firmware),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            is OtaState.UpToDate -> {
                AssistChip(
                    onClick = { viewModel.dismissError() },
                    label = { Text(stringResource(R.string.firmware_up_to_date)) },
                )
            }

            is OtaState.Available -> {
                Card {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(R.string.firmware_available, s.info.version),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (s.info.releasePageUrl.isNotEmpty()) {
                                IconButton(onClick = { uriHandler.openUri(s.info.releasePageUrl) }) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                                }
                            }
                        }
                        if (s.info.releaseNotes.isNotBlank()) {
                            Text(
                                s.info.releaseNotes.take(300),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            stringResource(
                                R.string.current_firmware_version,
                                s.info.currentDeviceVersion.ifBlank { "?" },
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { viewModel.downloadFirmware(s.info) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.emberOrange),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.download_firmware))
                        }
                    }
                }
            }

            is OtaState.Downloading -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.downloading_firmware, s.info.version, s.progress),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    LinearProgressIndicator(
                        progress = { s.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            is OtaState.ReadyToUpload -> {
                Card {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.firmware_ready_to_upload, s.info.version),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.firmware_upload_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { viewModel.uploadFirmware(s.file, s.info) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.emberOrange),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.upload_firmware))
                        }
                    }
                }
            }

            is OtaState.Uploading -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.uploading_firmware, s.info.version),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            is OtaState.Done -> {
                AssistChip(
                    onClick = { viewModel.dismissError() },
                    label = { Text(stringResource(R.string.firmware_update_done, s.info.version)) },
                )
            }

            is OtaState.Error -> {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            s.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                }
            }
        }
    }
}
