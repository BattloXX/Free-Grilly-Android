package org.battlo.freegrilly.data.device

import org.battlo.freegrilly.data.Capabilities
import org.battlo.freegrilly.data.hasFlag

enum class AlarmMuteEndpoint { GLOBAL, PROBE }

/** Chooses the API contract advertised by the connected firmware. */
fun alarmMuteEndpoint(capabilities: Set<String>, probeId: Int?): AlarmMuteEndpoint =
    if (probeId != null && capabilities.hasFlag(Capabilities.ALARM_PROBE_MUTE)) {
        AlarmMuteEndpoint.PROBE
    } else {
        AlarmMuteEndpoint.GLOBAL
    }
