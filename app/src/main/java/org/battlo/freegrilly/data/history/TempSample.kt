package org.battlo.freegrilly.data.history

/** A temperature point in time. [value] is °C (already divided by 10). */
data class TempSample(val tsMs: Long, val value: Float)

/**
 * Reduce a (sorted, ascending-by-time) series to roughly [maxPoints] using min/max
 * bucketing: each bucket contributes its lowest and highest point, so spikes/dips are
 * preserved. Keeps rendering cheap no matter how long the cook.
 */
object Downsample {
    fun minMax(points: List<TempSample>, maxPoints: Int): List<TempSample> {
        if (maxPoints < 4 || points.size <= maxPoints) return points
        val buckets = maxPoints / 2
        val n = points.size
        val result = ArrayList<TempSample>(maxPoints + 2)
        result.add(points.first())
        for (b in 0 until buckets) {
            val start = (b.toLong() * n / buckets).toInt()
            val end = ((b + 1).toLong() * n / buckets).toInt()
            if (start >= end) continue
            var minP = points[start]
            var maxP = points[start]
            for (i in start until end) {
                val v = points[i].value
                if (v < minP.value) minP = points[i]
                if (v > maxP.value) maxP = points[i]
            }
            // Preserve temporal order within the bucket.
            if (minP.tsMs <= maxP.tsMs) {
                result.add(minP); result.add(maxP)
            } else {
                result.add(maxP); result.add(minP)
            }
        }
        result.add(points.last())
        return result
    }
}
