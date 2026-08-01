package com.agupta07505.attendsmartly.data.local.dao

import androidx.room.*
import com.agupta07505.attendsmartly.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE isArchived = 0 ORDER BY name ASC")
    fun getActiveSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY isArchived ASC, name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun getSubjectByIdFlow(id: Long): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("UPDATE subjects SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchivedStatus(id: Long, isArchived: Boolean)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: Long)
}
