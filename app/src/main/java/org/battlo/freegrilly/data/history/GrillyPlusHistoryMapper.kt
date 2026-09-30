package org.battlo.freegrilly.data.history

import org.battlo.freegrilly.data.api.models.GrillyPlusHistoryTier

/** Converts the Grilly+ tier format into time-stamped 1/10 °C samples. */
object GrillyPlusHistoryMapper {
    fun samples(probeId: Int, tier: GrillyPlusHistoryTier?, nowMs: Long): List<TempSampleEntity> {
        if (tier == null || tier.interval <= 0) return emptyList()
        val size = tier.values.size
        return tier.values.mapIndexedNotNull { index, value ->
            value?.let {
                TempSampleEntity(
                    sessionId = 0L,
                    probeId = probeId,
                    tsMs = nowMs - tier.age * 1000L - (size - 1 - index) * tier.interval * 1000L,
                    tempCx10 = it,
                )
            }
        }
    }
}
