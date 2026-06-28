package org.battlo.freegrilly.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ---------------------------------------------------------------------------
// Custom semantic colors not covered by Material3 ColorScheme
// ---------------------------------------------------------------------------

@Immutable
data class GrillyColors(
    /** Ember orange — primary brand accent (#FF5F00). */
    val emberOrange: Color = EmberOrange,
    /** Success / "in target range" state (#00E676). */
    val successGreen: Color = SuccessGreen,
    /** Alarm / danger state (#FF1744). */
    val criticalRed: Color = CriticalRed,
    /** Warning / approaching limit state (#FFB300). */
    val warningAmber: Color = WarningAmber,
    /** Probe slot with no active probe (#5B4137). */
    val probeInactive: Color = ProbeInactive,
    /** Large temperature readout foreground (#FFDBCB). */
    val tempDisplay: Color = TempDisplay,
)

val LocalGrillyColors = staticCompositionLocalOf { GrillyColors() }

// ---------------------------------------------------------------------------
// Material3 dark color scheme
// ---------------------------------------------------------------------------

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Ember,
    onPrimary = OnEmber,
    primaryContainer = EmberContainer,
    onPrimaryContainer = OnEmberContainer,

    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,

    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,

    error = CriticalRed,
    onError = OnCriticalRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    background = Background,
    onBackground = OnBackground,

    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,

    outline = Outline,
    outlineVariant = OutlineVariant,

    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    inversePrimary = InversePrimary,

    surfaceTint = SurfaceTint,
    scrim = Scrim,

    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainerLowest = SurfaceContainerLowest,
)

// ---------------------------------------------------------------------------
// Theme entry point
// ---------------------------------------------------------------------------

/**
 * Free-Grilly app theme.
 *
 * The app is designed for dark mode only — there is no light-mode variant.
 * The status bar is made transparent and its icons are set to light so they
 * remain visible against the dark [Background].
 *
 * Access custom semantic colors anywhere inside [content] via:
 *   `MaterialTheme.grillyColors.emberOrange`
 */
@Composable
fun FreeGrillyTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            // Transparent status bar; let our background show through.
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(
        LocalGrillyColors provides GrillyColors(),
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = GrillyTypography,
            shapes = GrillyShapes,
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------
// Convenience extension on MaterialTheme
// ---------------------------------------------------------------------------

/** Shortcut to access [GrillyColors] from any composable inside [FreeGrillyTheme]. */
val MaterialTheme.grillyColors: GrillyColors
    @Composable
    get() = LocalGrillyColors.current
