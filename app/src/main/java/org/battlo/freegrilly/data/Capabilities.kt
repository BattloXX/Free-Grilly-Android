package org.battlo.freegrilly.data

/**
 * Known capability strings returned by the device in GET /api/info.
 *
 * Convention (established before §8):
 *  - An EMPTY capabilities list means "original / unknown firmware" — legacy features
 *    (history, alarm_mute) are assumed to work (backward compat via [supports]).
 *  - A NON-EMPTY list is authoritative: only listed flags are active.
 *
 * §8 extension-point flags (events, power_saving, ota) are NEW opt-in features that must
 * be EXPLICITLY declared by the firmware. They use [hasFlag], which returns false for the
 * empty-set (original firmware) case — ensuring the old firmware path is never changed.
 */
object Capabilities {
    // ── Existing (backward-compat) ──────────────────────────────────────────
    const val HISTORY     = "history"
    const val ALARM_MUTE  = "alarm_mute"
    const val ETA         = "eta"

    // ── §8 Future extension points ───────────────────────────────────────────
    /** Server-Sent Events stream on GET /api/events. Replaces 1-s polling. */
    const val EVENTS       = "events"
    /** Device power-saving mode via POST /api/settings { power_saving: true }. */
    const val POWER_SAVING = "power_saving"
    /** In-app firmware OTA via PUT /update (ElegantOTA-compatible). */
    const val OTA          = "ota"
    /** Optional device hint that OTA requires the Grilly+ admin password. */
    const val OTA_AUTH     = "ota_auth"
    const val CLEAR_HISTORY = "clear_history"
    const val ALARM_PROBES = "alarm_probes"
    /** Per-probe alarm muting through POST /api/probes/{id}/alarm/mute. */
    const val ALARM_PROBE_MUTE = "alarm_probe_mute"
    const val CALIBRATION_OFFSET = "calibration_offset"
    const val DIAGNOSTICS = "diagnostics"

    /** Firmware-specific capability names normalized to the app's names. */
    fun normalize(flags: List<String>): Set<String> = flags.map {
        when (it) {
            "ota_upload" -> OTA
            "sse" -> EVENTS
            "calibration_offset", "probe_calibration" -> CALIBRATION_OFFSET
            else -> it
        }
    }.toSet()
}

/**
 * Returns true if [flag] is supported by this capability set.
 *
 * Backward-compat semantics: an **empty** set (original/unknown firmware) is treated as
 * "all legacy features available". Use for [Capabilities.HISTORY], [Capabilities.ALARM_MUTE].
 */
fun Set<String>.supports(flag: String): Boolean = isEmpty() || flag in this

/**
 * Returns true only if [flag] is **explicitly declared** in this capability set.
 *
 * Strict opt-in semantics: an **empty** set (original firmware) returns false.
 * Use for §8 extension-point flags ([Capabilities.EVENTS], [Capabilities.POWER_SAVING],
 * [Capabilities.OTA]) that must never silently activate on old firmware.
 */
fun Set<String>.hasFlag(flag: String): Boolean = isNotEmpty() && flag in this
