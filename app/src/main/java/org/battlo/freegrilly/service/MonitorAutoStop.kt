package org.battlo.freegrilly.service

object MonitorAutoStop {
    const val TIMEOUT_MS = 30 * 60_000L

    data class State(
        val disconnectedSince: Long? = null,
        val noProbesSince: Long? = null,
    )

    fun update(previous: State, now: Long, disconnected: Boolean, hasConnectedProbe: Boolean): State = State(
        disconnectedSince = if (disconnected) previous.disconnectedSince ?: now else null,
        noProbesSince = if (!hasConnectedProbe) previous.noProbesSince ?: now else null,
    )

    fun shouldStop(state: State, now: Long): Boolean =
        state.disconnectedSince?.let { now - it >= TIMEOUT_MS } == true ||
            state.noProbesSince?.let { now - it >= TIMEOUT_MS } == true
}
