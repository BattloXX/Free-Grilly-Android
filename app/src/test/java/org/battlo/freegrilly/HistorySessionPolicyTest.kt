package org.battlo.freegrilly

import org.battlo.freegrilly.data.device.model.HistoryPoint
import org.battlo.freegrilly.data.device.model.HistorySeries
import org.battlo.freegrilly.data.device.model.HistoryTier
import org.battlo.freegrilly.data.history.SessionAction
import org.battlo.freegrilly.data.history.decideSession
import org.battlo.freegrilly.data.history.mergeHistorySeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistorySessionPolicyTest {
    @Test fun `new firmware id closes current and creates a new session`() {
        val decision = decideSession(10, "cook-a", "cook-b", null, true)
        assertEquals(SessionAction.CREATE_NEW, decision.action)
        assertTrue(decision.closeCurrent)
    }

    @Test fun `same firmware id keeps current session`() {
        assertEquals(SessionAction.KEEP_CURRENT, decideSession(10, "cook-a", "cook-a", null, false).action)
    }

    @Test fun `missing firmware id retains sixty minute resume behavior`() {
        assertEquals(SessionAction.RESUME_EXISTING, decideSession(null, null, null, null, true).action)
        assertEquals(SessionAction.CREATE_NEW, decideSession(null, null, null, null, false).action)
    }

    @Test fun `history merge removes overlapping coarse points and deduplicates`() {
        val merged = mergeHistorySeries(listOf(
            HistorySeries(1, HistoryTier.COARSE, listOf(HistoryPoint(10, 10f), HistoryPoint(20, 20f))),
            HistorySeries(1, HistoryTier.FINE, listOf(HistoryPoint(20, 21f), HistoryPoint(30, 30f))),
            HistorySeries(1, HistoryTier.FINE, listOf(HistoryPoint(30, 30f))),
        ))
        assertEquals(listOf(10L, 20L, 30L), merged.map { it.second.timestampMs })
        assertEquals(21f, merged[1].second.temperatureC)
    }
}
