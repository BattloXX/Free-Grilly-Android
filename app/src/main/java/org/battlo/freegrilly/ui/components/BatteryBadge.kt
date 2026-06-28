package org.battlo.freegrilly.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.battlo.freegrilly.domain.TempUtils

@Composable
fun BatteryBadge(
    batteryPercent: Int,
    isCharging: Boolean,
    wifiDbm: Int,
    modifier: Modifier = Modifier,
) {
    val wifiPct = TempUtils.dBmToPercent(wifiDbm)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (wifiPct >= 33) Icons.Default.Wifi else Icons.Default.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = if (isCharging) Icons.Default.BatteryChargingFull
            else when {
                batteryPercent > 60 -> Icons.Default.BatteryFull
                batteryPercent > 20 -> Icons.Default.Battery4Bar
                else -> Icons.Default.Battery1Bar
            },
            contentDescription = null,
            tint = when {
                batteryPercent <= 20 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.width(2.dp))
        Text(
            "$batteryPercent%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
