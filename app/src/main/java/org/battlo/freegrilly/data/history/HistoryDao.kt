package org.battlo.freegrilly.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<TempSampleEntity>)

    @Insert
    suspend fun insertSession(session: CookSessionEntity): Long

    @Query("SELECT * FROM cook_sessions WHERE deviceId = :deviceId ORDER BY startedAt DESC LIMIT 1")
    suspend fun latestSession(deviceId: String): CookSessionEntity?

    @Query("SELECT MAX(tsMs) FROM temp_samples WHERE sessionId = :sessionId")
    suspend fun lastSampleTs(sessionId: Long): Long?

    @Query("SELECT COUNT(*) FROM temp_samples WHERE sessionId = :sessionId")
    suspend fun sampleCount(sessionId: Long): Int

    /** Live stream for the probe-detail chart (whole session; window filtered client-side). */
    @Query("SELECT * FROM temp_samples WHERE sessionId = :sessionId AND probeId = :probeId AND tsMs >= :fromTs ORDER BY tsMs ASC")
    fun observeSamples(sessionId: Long, probeId: Int, fromTs: Long): Flow<List<TempSampleEntity>>

    @Query("SELECT * FROM temp_samples WHERE sessionId = :sessionId AND probeId = :probeId ORDER BY tsMs ASC")
    suspend fun samplesForProbe(sessionId: Long, probeId: Int): List<TempSampleEntity>

    @Query("SELECT DISTINCT probeId FROM temp_samples WHERE sessionId = :sessionId ORDER BY probeId ASC")
    suspend fun probeIdsForSession(sessionId: Long): List<Int>

    @Query("SELECT * FROM cook_sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<CookSessionEntity>>

    /** Retention: drop cooks older than the cutoff (samples cascade-delete). */
    @Query("DELETE FROM cook_sessions WHERE startedAt < :cutoff")
    suspend fun deleteSessionsBefore(cutoff: Long)
}
