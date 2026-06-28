package org.battlo.freegrilly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.battlo.freegrilly.ui.theme.LocalGrillyColors

@Composable
fun StepIndicator(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalGrillyColors.current
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(totalSteps) { idx ->
            val isActive = idx == currentStep
            val isDone = idx < currentStep
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(if (isActive) 10.dp else 8.dp)
                    .background(
                        when {
                            isActive -> colors.emberOrange
                            isDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {}
        }
    }
}
