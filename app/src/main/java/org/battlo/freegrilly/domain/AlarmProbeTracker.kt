package org.battlo.freegrilly.domain

/**
 * Keeps the firmware alarm edge state separate from Android notification code.
 * A probe stays armed until the firmware reports that its alarm stopped.
 */
class AlarmProbeTracker {
    private var activeProbeIds = emptySet<Int>()
    val activeIds: Set<Int> get() = activeProbeIds

    fun update(alarmSounding: Boolean, probes: List<AlarmProbe>): AlarmProbeChange {
        val alarming = if (!alarmSounding) {
            emptySet()
        } else {
            // Only probes the firmware flags are notified: guessing a probe would name the wrong one.
            probes.filter { it.alarm }.mapTo(mutableSetOf()) { it.id }
        }
        val change = AlarmProbeChange(
            started = alarming - activeProbeIds,
            stopped = activeProbeIds - alarming,
        )
        activeProbeIds = alarming
        return change
    }
}

data class AlarmProbe(val id: Int, val alarm: Boolean)
data class AlarmProbeChange(val started: Set<Int>, val stopped: Set<Int>)
