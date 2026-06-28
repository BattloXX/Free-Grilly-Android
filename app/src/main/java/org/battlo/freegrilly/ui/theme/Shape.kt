package org.battlo.freegrilly.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val GrillyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
    // Full / pill shape — 50 % corner radius.
    // Use RoundedCornerShape(50 %) where a circular / pill shape is needed.
    // There is no dedicated "Full" slot in Material3 Shapes; apply
    // RoundedCornerShape(percent = 50) directly at the call site.
)

/** Convenience alias for a fully-rounded (pill) shape. */
val ShapeFull = RoundedCornerShape(percent = 50)
