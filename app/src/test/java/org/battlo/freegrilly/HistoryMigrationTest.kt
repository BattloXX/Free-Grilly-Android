package org.battlo.freegrilly

import java.sql.DriverManager
import org.battlo.freegrilly.data.history.HistoryDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryMigrationTest {
    @Test fun `v1 history data survives v2 firmware session migration`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { db ->
            db.createStatement().use { statement ->
                statement.execute("CREATE TABLE cook_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, deviceId TEXT NOT NULL, deviceName TEXT NOT NULL, startedAt INTEGER NOT NULL, endedAt INTEGER)")
                statement.execute("CREATE TABLE temp_samples (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, probeId INTEGER NOT NULL, tsMs INTEGER NOT NULL, tempCx10 INTEGER NOT NULL)")
                statement.execute("CREATE UNIQUE INDEX index_temp_samples_sessionId_probeId_tsMs ON temp_samples (sessionId, probeId, tsMs)")
                statement.execute("INSERT INTO cook_sessions (id, deviceId, deviceName, startedAt) VALUES (7, 'device-1', 'Grill', 100)")
                statement.execute("INSERT INTO temp_samples (sessionId, probeId, tsMs, tempCx10) VALUES (7, 2, 200, 650)")
                statement.execute(HistoryDatabase.ADD_FIRMWARE_SESSION_ID)
                statement.execute(HistoryDatabase.CREATE_FIRMWARE_SESSION_INDEX)
                statement.executeQuery("SELECT deviceId, firmwareSessionId FROM cook_sessions WHERE id = 7").use { rows ->
                    assertTrue(rows.next())
                    assertEquals("device-1", rows.getString("deviceId"))
                    assertEquals(null, rows.getString("firmwareSessionId"))
                }
                statement.executeQuery("SELECT tempCx10 FROM temp_samples WHERE sessionId = 7").use { rows ->
                    assertTrue(rows.next())
                    assertEquals(650, rows.getInt(1))
                }
                statement.executeQuery("PRAGMA index_info(index_cook_sessions_deviceId_firmwareSessionId)").use { rows ->
                    val columns = generateSequence { if (rows.next()) rows.getString("name") else null }.toList()
                    assertEquals(listOf("deviceId", "firmwareSessionId"), columns)
                }
            }
        }
    }
}
