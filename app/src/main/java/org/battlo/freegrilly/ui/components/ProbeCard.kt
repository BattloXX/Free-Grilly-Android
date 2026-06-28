package org.battlo.freegrilly.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.battlo.freegrilly.data.api.models.ProbeStatus
import org.battlo.freegrilly.domain.EtaFormatter
import org.battlo.freegrilly.domain.TempUtils
import org.battlo.freegrilly.ui.theme.LocalGrillyColors

@Composable
fun ProbeCard(
    probe: ProbeStatus,
    unit: String,
    historyData: List<Float>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalGrillyColors.current
    val borderColor = when {
        probe.alarm -> colors.criticalRed
        probe.connected -> colors.emberOrange
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = probe.name.ifEmpty { "Sonde ${probe.id}" },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!probe.connected) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            "—",
                            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (probe.connected) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = TempUtils.format(probe.temperature, unit, 1),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (probe.alarm) colors.criticalRed else colors.emberOrange,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "→ ${TempUtils.format(probe.targetTemperature, unit, 0)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (probe.etaSeconds >= 0) {
                            Text(
                                EtaFormatter.format(probe.etaSeconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.successGreen,
                            )
                        }
                    }
                }
                if (historyData.size > 2) {
                    Spacer(Modifier.height(8.dp))
                    Sparkline(
                        data = historyData,
                        targetTemp = probe.targetTemperature,
                        color = if (probe.alarm) colors.criticalRed else colors.emberOrange,
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                    )
                }
            } else {
                Text(
                    "—",
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
