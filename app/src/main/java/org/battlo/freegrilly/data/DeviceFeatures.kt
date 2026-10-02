package org.battlo.freegrilly.data

/**
 * Capability-gated device features exposed to the UI.
 *
 * These are all opt-in extensions: an empty capability set describes legacy
 * Free-Grilly firmware and therefore enables none of them. Legacy behavior
 * such as normal history and alarm muting continues to use [supports] at its
 * call site.
 */
data class DeviceFeatures(
    val calibration: Boolean = false,
    val longTermHistory: Boolean = false,
    val ota: Boolean = false,
    val events: Boolean = false,
    val clearHistory: Boolean = false,
    val diagnostics: Boolean = false,
    val alarmProbes: Boolean = false,
    val perProbeMute: Boolean = false,
) {
    companion object {
        fun from(capabilities: Set<String>): DeviceFeatures {
            val normalized = Capabilities.normalize(capabilities.toList())
            return DeviceFeatures(
                calibration = normalized.hasFlag(Capabilities.CALIBRATION_OFFSET),
                // `history` on legacy firmware is the existing short history API;
                // only an explicit declaration opts into long-term history UI.
                longTermHistory = normalized.hasFlag(Capabilities.HISTORY),
                ota = normalized.hasFlag(Capabilities.OTA),
                events = normalized.hasFlag(Capabilities.EVENTS),
                clearHistory = normalized.hasFlag(Capabilities.CLEAR_HISTORY),
                diagnostics = normalized.hasFlag(Capabilities.DIAGNOSTICS),
                alarmProbes = normalized.hasFlag(Capabilities.ALARM_PROBES),
                perProbeMute = normalized.hasFlag(Capabilities.ALARM_PROBE_MUTE),
            )
        }
    }
}
