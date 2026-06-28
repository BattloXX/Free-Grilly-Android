package org.battlo.freegrilly.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Design-system spacing and sizing tokens for Free-Grilly.
 *
 * Use these constants instead of hard-coded dp values so that the layout
 * adapts consistently if the grid ever changes.
 */
object GrillyDimens {

    // ---------------------------------------------------------------------------
    // Spacing scale
    // ---------------------------------------------------------------------------

    /** 4 dp — micro gap, icon padding */
    val spaceXs = 4.dp

    /** 8 dp — tight spacing between related elements */
    val spaceSm = 8.dp

    /** 16 dp — standard content padding */
    val spaceMd = 16.dp

    /** 24 dp — section gap */
    val spaceLg = 24.dp

    /** 32 dp — large vertical rhythm break */
    val spaceXl = 32.dp

    /** 48 dp — hero / display section breathing room */
    val spaceXxl = 48.dp

    // ---------------------------------------------------------------------------
    // Component sizes
    // ---------------------------------------------------------------------------

    /** Default internal padding for cards */
    val cardPadding = 16.dp

    /** Horizontal margin from the screen edge */
    val screenMargin = 16.dp

    /** Fixed height of a probe / temperature card */
    val probeCardHeight = 180.dp

    /** Width of the navigation rail on tablets */
    val navRailWidth = 80.dp

    /** Width of the navigation drawer on large screens */
    val navDrawerWidth = 320.dp

    /** Height of the temperature graph / chart component */
    val graphHeight = 200.dp
}
