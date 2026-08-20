/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.data.local.dao

import androidx.room.*
import com.agupta07505.attendsmartly.data.local.entity.TimetableEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_entries WHERE (isActive = 1 OR isActive IS NULL) ORDER BY startTime ASC")
    fun getAllActiveEntries(): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries ORDER BY startTime ASC")
    fun getAllEntries(): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :dayOfWeek AND (isActive = 1 OR isActive IS NULL) ORDER BY startTime ASC")
    fun getEntriesForDay(dayOfWeek: Int): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :dayOfWeek AND (startDate IS NULL OR trim(startDate) = '' OR startDate <= :date) AND (endDate IS NULL OR trim(endDate) = '' OR endDate >= :date) AND (isActive = 1 OR isActive IS NULL) ORDER BY startTime ASC")
    fun getEntriesForDayAndDate(dayOfWeek: Int, date: String): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE subjectId = :subjectId AND (isActive = 1 OR isActive IS NULL) ORDER BY dayOfWeek ASC, startTime ASC")
    fun getEntriesForSubject(subjectId: Long): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): TimetableEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimetableEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<TimetableEntryEntity>): List<Long>

    @Update
    suspend fun updateEntry(entry: TimetableEntryEntity)

    @Update
    suspend fun updateEntries(entries: List<TimetableEntryEntity>)

    @Delete
    suspend fun deleteEntry(entry: TimetableEntryEntity)

    @Query("DELETE FROM timetable_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("UPDATE timetable_entries SET endDate = :endDate, updatedAt = :updatedAt WHERE isActive = 1 AND (endDate = '' OR endDate > :endDate)")
    suspend fun endActiveTimetableEntries(endDate: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE timetable_entries SET endDate = :endDate, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setEntryEndDate(id: Long, endDate: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM timetable_entries")
    suspend fun deleteAllEntries()
}
