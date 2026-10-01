package org.battlo.freegrilly.data.history

import org.battlo.freegrilly.data.device.model.HistoryPoint
import org.battlo.freegrilly.data.device.model.HistorySeries
import org.battlo.freegrilly.data.device.model.HistoryTier

/** Removes coarse/fine overlap and duplicate timestamps before idempotent Room insertion. */
internal fun mergeHistorySeries(series: List<HistorySeries>): List<Pair<Int, HistoryPoint>> =
    series.groupBy { it.probeId }.flatMap { (probeId, probeSeries) ->
        val fineStart = probeSeries.filter { it.tier == HistoryTier.FINE }
            .flatMap { it.points }.minOfOrNull { it.timestampMs } ?: Long.MAX_VALUE
        probeSeries.flatMap { item -> item.points.map { item.tier to it } }
            .filter { (tier, point) -> tier != HistoryTier.COARSE || point.timestampMs < fineStart }
            // Fine wins if a firmware response contains an exact tier-boundary duplicate.
            .sortedBy { (tier, _) -> if (tier == HistoryTier.COARSE) 0 else 1 }
            .associateBy { (_, point) -> point.timestampMs }
            .values.sortedBy { it.second.timestampMs }
            .map { (_, point) -> probeId to point }
    }
