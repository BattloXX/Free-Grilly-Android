package org.battlo.freegrilly.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.battlo.freegrilly.R
import org.battlo.freegrilly.data.history.CookSessionEntity
import org.battlo.freegrilly.ui.components.TimeSeriesChart
import org.battlo.freegrilly.ui.theme.LocalGrillyColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    onBack: () -> Unit,
    viewModel: SessionHistoryViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionCard(session, viewModel)
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: CookSessionEntity, viewModel: SessionHistoryViewModel) {
    val colors = LocalGrillyColors.current
    var expanded by remember { mutableStateOf(false) }
    val charts by produceState<List<ProbeSeries>?>(initialValue = null, expanded) {
        value = if (expanded) viewModel.loadCharts(session.id) else null
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(formatDate(session.startedAt), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.history_tap_to_expand),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                val current = charts
                if (current == null) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                } else if (current.isEmpty()) {
                    Text(
                        stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    current.forEach { series ->
                        Text(
                            "Sonde ${series.probeId}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.height(4.dp))
                        TimeSeriesChart(
                            data = series.points,
                            targetTemp = null,
                            color = colors.emberOrange,
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            strokeWidth = 2f,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

private fun formatDate(tsMs: Long): String =
    SimpleDateFormat("EEE d. MMM, HH:mm", Locale.getDefault()).format(Date(tsMs))
