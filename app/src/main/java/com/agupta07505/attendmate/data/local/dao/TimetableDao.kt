package com.agupta07505.attendmate.data.local.dao

import androidx.room.*
import com.agupta07505.attendmate.data.local.entity.TimetableEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_entries WHERE isActive = 1 ORDER BY startTime ASC")
    fun getAllActiveEntries(): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :dayOfWeek AND isActive = 1 ORDER BY startTime ASC")
    fun getEntriesForDay(dayOfWeek: Int): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE subjectId = :subjectId AND isActive = 1 ORDER BY dayOfWeek ASC, startTime ASC")
    fun getEntriesForSubject(subjectId: Long): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): TimetableEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimetableEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: TimetableEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: TimetableEntryEntity)

    @Query("DELETE FROM timetable_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)
}
