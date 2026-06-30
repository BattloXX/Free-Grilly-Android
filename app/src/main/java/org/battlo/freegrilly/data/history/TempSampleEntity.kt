package org.battlo.freegrilly.data.history

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single temperature reading. Value stored as celsius × 10 (Int) to save space.
 * Unique on (sessionId, probeId, tsMs) so re-seeding the same backfill is idempotent.
 */
@Entity(
    tableName = "temp_samples",
    indices = [Index(value = ["sessionId", "probeId", "tsMs"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = CookSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TempSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val probeId: Int,
    val tsMs: Long,
    val tempCx10: Int,
)
