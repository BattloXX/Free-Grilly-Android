package org.battlo.freegrilly.data.history

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Separate from FoodDatabase on purpose: keeps temperature history independent of the
 * food library so neither schema's migrations can endanger the other's data.
 */
@Database(
    entities = [CookSessionEntity::class, TempSampleEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
