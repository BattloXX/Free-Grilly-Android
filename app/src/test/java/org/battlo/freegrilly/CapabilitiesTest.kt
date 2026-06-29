package org.battlo.freegrilly

import org.battlo.freegrilly.data.Capabilities
import org.battlo.freegrilly.data.hasFlag
import org.battlo.freegrilly.data.supports
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [Capabilities] extension functions [supports] and [hasFlag].
 *
 * Key invariant: an EMPTY set (original/unknown firmware) must never activate §8 opt-in
 * features ([hasFlag] returns false), but must keep backward-compat features working
 * ([supports] returns true).
 */
class CapabilitiesTest {

    // ── supports() — backward-compat (empty = all legacy features available) ───────────────

    @Test
    fun `supports returns true for empty set (original firmware fallback)`() {
        val caps = emptySet<String>()
        assertTrue(caps.supports(Capabilities.HISTORY))
        assertTrue(caps.supports(Capabilities.ALARM_MUTE))
        assertTrue(caps.supports(Capabilities.ETA))
    }

    @Test
    fun `supports returns true when flag is explicitly present`() {
        val caps = setOf(Capabilities.HISTORY, Capabilities.ALARM_MUTE)
        assertTrue(caps.supports(Capabilities.HISTORY))
        assertTrue(caps.supports(Capabilities.ALARM_MUTE))
    }

    @Test
    fun `supports returns false when non-empty set does not contain flag`() {
        val caps = setOf(Capabilities.HISTORY) // alarm_mute not listed
        assertFalse(caps.supports(Capabilities.ALARM_MUTE))
    }

    // ── hasFlag() — strict opt-in (empty = never activates new features) ─────────────────

    @Test
    fun `hasFlag returns false for empty set (must not activate on original firmware)`() {
        val caps = emptySet<String>()
        assertFalse(caps.hasFlag(Capabilities.EVENTS))
        assertFalse(caps.hasFlag(Capabilities.POWER_SAVING))
        assertFalse(caps.hasFlag(Capabilities.OTA))
    }

    @Test
    fun `hasFlag returns true only when flag is explicitly present`() {
        val caps = setOf(Capabilities.EVENTS, Capabilities.OTA)
        assertTrue(caps.hasFlag(Capabilities.EVENTS))
        assertTrue(caps.hasFlag(Capabilities.OTA))
        assertFalse(caps.hasFlag(Capabilities.POWER_SAVING))
    }

    @Test
    fun `hasFlag returns false when non-empty set does not contain flag`() {
        val caps = setOf(Capabilities.HISTORY, Capabilities.ALARM_MUTE, Capabilities.ETA)
        assertFalse(caps.hasFlag(Capabilities.EVENTS))
        assertFalse(caps.hasFlag(Capabilities.POWER_SAVING))
        assertFalse(caps.hasFlag(Capabilities.OTA))
    }

    @Test
    fun `full firmware with all flags - supports and hasFlag both return true`() {
        val caps = setOf(
            Capabilities.HISTORY, Capabilities.ALARM_MUTE, Capabilities.ETA,
            Capabilities.EVENTS, Capabilities.POWER_SAVING, Capabilities.OTA,
        )
        assertTrue(caps.supports(Capabilities.HISTORY))
        assertTrue(caps.supports(Capabilities.ALARM_MUTE))
        assertTrue(caps.hasFlag(Capabilities.EVENTS))
        assertTrue(caps.hasFlag(Capabilities.POWER_SAVING))
        assertTrue(caps.hasFlag(Capabilities.OTA))
    }
}
