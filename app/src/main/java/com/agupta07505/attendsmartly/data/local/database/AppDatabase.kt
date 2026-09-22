/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.agupta07505.attendsmartly.data.local.dao.AttendanceDao
import com.agupta07505.attendsmartly.data.local.dao.HolidayDao
import com.agupta07505.attendsmartly.data.local.dao.SubjectDao
import com.agupta07505.attendsmartly.data.local.dao.TimetableDao
import com.agupta07505.attendsmartly.data.local.entity.AttendanceSessionEntity
import com.agupta07505.attendsmartly.data.local.entity.AttendanceUnitEntity
import com.agupta07505.attendsmartly.data.local.entity.HolidayEntity
import com.agupta07505.attendsmartly.data.local.entity.SubjectEntity
import com.agupta07505.attendsmartly.data.local.entity.TimetableEntryEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SubjectEntity::class,
        TimetableEntryEntity::class,
        AttendanceSessionEntity::class,
        AttendanceUnitEntity::class,
        HolidayEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun timetableDao(): TimetableDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun holidayDao(): HolidayDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun addColumnIfNotExists(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            columnDefinition: String
        ) {
            var exists = false
            try {
                val cursor = db.query("PRAGMA table_info(`$tableName`)")
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex != -1 && cursor.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                        exists = true
                        break
                    }
                }
                cursor.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (!exists) {
                try {
                    db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinition")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure subjects table has all columns
                addColumnIfNotExists(db, "subjects", "latitude", "REAL DEFAULT NULL")
                addColumnIfNotExists(db, "subjects", "longitude", "REAL DEFAULT NULL")
                addColumnIfNotExists(db, "subjects", "locationRadiusMeters", "INTEGER NOT NULL DEFAULT 50")

                // Ensure timetable_entries table has all columns
                addColumnIfNotExists(db, "timetable_entries", "latitude", "REAL DEFAULT NULL")
                addColumnIfNotExists(db, "timetable_entries", "longitude", "REAL DEFAULT NULL")
                addColumnIfNotExists(db, "timetable_entries", "locationRadiusMeters", "INTEGER NOT NULL DEFAULT 50")

                // Ensure attendance_sessions table has all columns (including those added in 1.x without version bump)
                addColumnIfNotExists(db, "attendance_sessions", "isRescheduled", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists(db, "attendance_sessions", "originalDate", "TEXT DEFAULT NULL")
                addColumnIfNotExists(db, "attendance_sessions", "originalTime", "TEXT DEFAULT NULL")
                addColumnIfNotExists(db, "attendance_sessions", "rescheduledToDate", "TEXT DEFAULT NULL")
                addColumnIfNotExists(db, "attendance_sessions", "rescheduledToTime", "TEXT DEFAULT NULL")
                addColumnIfNotExists(db, "attendance_sessions", "rescheduledReason", "TEXT NOT NULL DEFAULT ''")
                addColumnIfNotExists(db, "attendance_sessions", "autoMarked", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "AttendSmartly_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
