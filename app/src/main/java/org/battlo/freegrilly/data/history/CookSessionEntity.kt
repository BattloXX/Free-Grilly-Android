package org.battlo.freegrilly.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One cook (grilling session) for a device. Times are derived primarily from the samples;
 * [endedAt] is informational and may be null while a cook is ongoing.
 */
@Entity(tableName = "cook_sessions")
data class CookSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val deviceName: String = "",
    val startedAt: Long,
    val endedAt: Long? = null,
)
