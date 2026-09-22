/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.agupta07505.attendsmartly.data.local.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class AppStartupAndMigrationTest {

    @Test
    fun testMigration1To2AddsMissingColumns() {
        val tables = mutableMapOf(
            "subjects" to mutableSetOf("id", "name", "code", "type"),
            "timetable_entries" to mutableSetOf("id", "subjectId", "dayOfWeek"),
            "attendance_sessions" to mutableSetOf("id", "subjectId", "sessionDate")
        )
        val executedQueries = mutableListOf<String>()

        val dbProxy = createMockDb(tables, executedQueries)

        // Run migration
        AppDatabase.MIGRATION_1_2.migrate(dbProxy)

        // Verify all required columns were added
        assertTrue(tables["subjects"]!!.contains("latitude"))
        assertTrue(tables["subjects"]!!.contains("longitude"))
        assertTrue(tables["subjects"]!!.contains("locationRadiusMeters"))

        assertTrue(tables["timetable_entries"]!!.contains("latitude"))
        assertTrue(tables["timetable_entries"]!!.contains("longitude"))
        assertTrue(tables["timetable_entries"]!!.contains("locationRadiusMeters"))

        assertTrue(tables["attendance_sessions"]!!.contains("isRescheduled"))
        assertTrue(tables["attendance_sessions"]!!.contains("originalDate"))
        assertTrue(tables["attendance_sessions"]!!.contains("originalTime"))
        assertTrue(tables["attendance_sessions"]!!.contains("rescheduledToDate"))
        assertTrue(tables["attendance_sessions"]!!.contains("rescheduledToTime"))
        assertTrue(tables["attendance_sessions"]!!.contains("rescheduledReason"))
        assertTrue(tables["attendance_sessions"]!!.contains("autoMarked"))

        // Run migration again to verify idempotency (no queries should be executed)
        val queryCountBefore = executedQueries.size
        AppDatabase.MIGRATION_1_2.migrate(dbProxy)
        assertEquals("No new ALTER TABLE queries should be run on second migration", queryCountBefore, executedQueries.size)
    }

    private fun createMockDb(
        tables: MutableMap<String, MutableSet<String>>,
        executedQueries: MutableList<String>
    ): SupportSQLiteDatabase {
        return Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "query" -> {
                    val sql = args[0] as String
                    val tableName = sql.substringAfter("PRAGMA table_info(`").substringBefore("`)")
                        .ifEmpty { sql.substringAfter("PRAGMA table_info(").substringBefore(")") }
                    val columns = tables[tableName]?.toList() ?: emptyList()
                    createMockCursor(columns)
                }
                "execSQL" -> {
                    val sql = args[0] as String
                    executedQueries.add(sql)
                    if (sql.startsWith("ALTER TABLE", ignoreCase = true)) {
                        val tableName = sql.substringAfter("ALTER TABLE `").substringBefore("`")
                            .ifEmpty { sql.substringAfter("ALTER TABLE ").substringBefore(" ") }
                        val colName = sql.substringAfter("ADD COLUMN `").substringBefore("`")
                            .ifEmpty { sql.substringAfter("ADD COLUMN ").substringBefore(" ") }
                        tables.getOrPut(tableName) { mutableSetOf() }.add(colName)
                    }
                    null
                }
                else -> null
            }
        } as SupportSQLiteDatabase
    }

    private fun createMockCursor(columns: List<String>): Cursor {
        var index = -1
        return Proxy.newProxyInstance(
            Cursor::class.java.classLoader,
            arrayOf(Cursor::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getColumnIndex" -> {
                    val col = args[0] as String
                    if (col == "name") 0 else -1
                }
                "moveToNext" -> {
                    index++
                    index < columns.size
                }
                "getString" -> {
                    columns[index]
                }
                "close" -> null
                else -> null
            }
        } as Cursor
    }
}
