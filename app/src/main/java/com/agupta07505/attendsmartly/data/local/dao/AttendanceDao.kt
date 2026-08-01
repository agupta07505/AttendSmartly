/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.data.local.dao

import androidx.room.*
import com.agupta07505.attendsmartly.data.local.entity.AttendanceSessionEntity
import com.agupta07505.attendsmartly.data.local.entity.AttendanceUnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_sessions WHERE sessionDate = :date")
    fun getSessionsForDate(date: String): Flow<List<AttendanceSessionEntity>>

    @Query("SELECT * FROM attendance_sessions WHERE sessionDate = :date AND subjectId = :subjectId LIMIT 1")
    suspend fun getSessionForSubjectAndDate(subjectId: Long, date: String): AttendanceSessionEntity?

    @Query("SELECT * FROM attendance_sessions WHERE sessionDate = :date AND timetableEntryId = :timetableEntryId LIMIT 1")
    suspend fun getSessionForTimetableAndDate(timetableEntryId: Long, date: String): AttendanceSessionEntity?

    @Query("SELECT * FROM attendance_sessions WHERE subjectId = :subjectId ORDER BY sessionDate DESC, startTime DESC")
    fun getSessionsForSubject(subjectId: Long): Flow<List<AttendanceSessionEntity>>

    @Query("SELECT * FROM attendance_sessions ORDER BY sessionDate DESC, startTime DESC")
    fun getAllSessions(): Flow<List<AttendanceSessionEntity>>

    @Query("SELECT * FROM attendance_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): AttendanceSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AttendanceSessionEntity): Long

    @Update
    suspend fun updateSession(session: AttendanceSessionEntity)

    @Delete
    suspend fun deleteSession(session: AttendanceSessionEntity)

    @Query("DELETE FROM attendance_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    // Attendance Units
    @Query("SELECT * FROM attendance_units WHERE sessionId = :sessionId ORDER BY unitIndex ASC")
    fun getUnitsForSessionFlow(sessionId: Long): Flow<List<AttendanceUnitEntity>>

    @Query("SELECT * FROM attendance_units WHERE sessionId = :sessionId ORDER BY unitIndex ASC")
    suspend fun getUnitsForSession(sessionId: Long): List<AttendanceUnitEntity>

    @Query("SELECT u.* FROM attendance_units u INNER JOIN attendance_sessions s ON u.sessionId = s.id WHERE s.subjectId = :subjectId")
    fun getAllUnitsForSubject(subjectId: Long): Flow<List<AttendanceUnitEntity>>

    @Query("SELECT * FROM attendance_units WHERE id = :unitId")
    suspend fun getUnitById(unitId: Long): AttendanceUnitEntity?

    @Query("SELECT * FROM attendance_units")
    fun getAllUnits(): Flow<List<AttendanceUnitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: AttendanceUnitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<AttendanceUnitEntity>)

    @Update
    suspend fun updateUnit(unit: AttendanceUnitEntity)

    @Update
    suspend fun updateUnits(units: List<AttendanceUnitEntity>)

    @Query("DELETE FROM attendance_units WHERE sessionId = :sessionId")
    suspend fun deleteUnitsForSession(sessionId: Long)

    @Transaction
    suspend fun createOrUpdateSessionWithUnits(
        session: AttendanceSessionEntity,
        units: List<AttendanceUnitEntity>
    ): Long {
        val existingSession = getSessionById(session.id)
        val sessionId = if (existingSession != null) {
            updateSession(session)
            session.id
        } else {
            insertSession(session)
        }

        // Replace units
        deleteUnitsForSession(sessionId)
        val preparedUnits = units.map { it.copy(sessionId = sessionId) }
        insertUnits(preparedUnits)
        return sessionId
    }
}
