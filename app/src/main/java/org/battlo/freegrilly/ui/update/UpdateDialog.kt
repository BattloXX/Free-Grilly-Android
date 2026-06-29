package org.battlo.freegrilly.ui.update

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.battlo.freegrilly.R
import org.battlo.freegrilly.ui.theme.LocalGrillyColors

@Composable
fun UpdateSection(
    state: UpdateState,
    currentVersion: String,
    onCheckUpdate: () -> Unit,
    onDownload: () -> Unit,
    onInstall: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalGrillyColors.current
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.settings_updates),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(R.string.current_version, currentVersion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (state) {
                is UpdateState.Checking -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else -> OutlinedButton(
                    onClick = onCheckUpdate,
                    enabled = state !is UpdateState.Downloading,
                ) {
                    Text(stringResource(R.string.check_for_update))
                }
            }
        }

        when (state) {
            is UpdateState.UpToDate -> {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        stringResource(R.string.up_to_date),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is UpdateState.Available -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = colors.emberOrange.copy(alpha = 0.12f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.update_available, state.info.version),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (state.info.releaseNotes.isNotBlank()) {
                            Text(
                                state.info.releaseNotes.lines().take(6).joinToString("\n"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val sizeMb = if (state.info.assetSizeBytes > 0) {
                            " (${state.info.assetSizeBytes / 1_048_576} MB)"
                        } else ""
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val canInstall = context.packageManager.canRequestPackageInstalls()
                                    if (!canInstall) {
                                        context.startActivity(
                                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                        )
                                    } else {
                                        onDownload()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.emberOrange),
                            ) {
                                Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.download_update) + sizeMb)
                            }
                            if (state.info.releasePageUrl.isNotBlank()) {
                                IconButton(onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(state.info.releasePageUrl)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                    )
                                }) {
                                    Icon(Icons.Default.OpenInBrowser, null)
                                }
                            }
                        }
                    }
                }
            }

            is UpdateState.Downloading -> {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.downloading_update, state.info.version, state.progress),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.emberOrange,
                        )
                    }
                }
            }

            is UpdateState.ReadyToInstall -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.ready_to_install, state.info.version),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onInstall(state.downloadId) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.emberOrange),
                        ) {
                            Icon(Icons.Default.InstallMobile, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.install))
                        }
                    }
                }
            }

            is UpdateState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                }
            }

            else -> {}
        }
    }
}
