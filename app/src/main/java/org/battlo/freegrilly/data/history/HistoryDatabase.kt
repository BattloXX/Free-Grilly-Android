package org.battlo.freegrilly.data.history

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Separate from FoodDatabase on purpose: keeps temperature history independent of the
 * food library so neither schema's migrations can endanger the other's data.
 */
@Database(
    entities = [CookSessionEntity::class, TempSampleEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        const val ADD_FIRMWARE_SESSION_ID =
            "ALTER TABLE cook_sessions ADD COLUMN firmwareSessionId TEXT"
        const val CREATE_FIRMWARE_SESSION_INDEX =
            "CREATE INDEX IF NOT EXISTS index_cook_sessions_deviceId_firmwareSessionId " +
                "ON cook_sessions (deviceId, firmwareSessionId)"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(ADD_FIRMWARE_SESSION_ID)
                db.execSQL(CREATE_FIRMWARE_SESSION_INDEX)
            }
        }
    }
}
