package org.battlo.freegrilly.data.history

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One cook (grilling session) for a device. Times are derived primarily from the samples;
 * [endedAt] is informational and may be null while a cook is ongoing.
 */
@Entity(
    tableName = "cook_sessions",
    indices = [Index(value = ["deviceId", "firmwareSessionId"])],
)
data class CookSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val deviceName: String = "",
    /** Optional firmware-owned cook identifier. Older firmware does not expose one. */
    val firmwareSessionId: String? = null,
    val startedAt: Long,
    val endedAt: Long? = null,
)
