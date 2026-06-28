package org.battlo.freegrilly.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.battlo.freegrilly.R
import org.battlo.freegrilly.ui.theme.LocalGrillyColors

@Composable
fun AlarmBanner(
    probeName: String,
    onMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalGrillyColors.current
    val infinite = rememberInfiniteTransition(label = "alarm_pulse")
    val alpha by infinite.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.criticalRed)
            .alpha(alpha)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "${stringResource(R.string.alarm_ready)}: $probeName",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onError,
        )
        IconButton(onClick = onMute) {
            Icon(Icons.Default.NotificationsOff, null, tint = MaterialTheme.colorScheme.onError)
        }
    }
}
