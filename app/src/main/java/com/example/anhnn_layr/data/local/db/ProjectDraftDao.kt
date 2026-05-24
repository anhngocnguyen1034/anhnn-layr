package com.example.anhnn_layr.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDraftDao {
    @Query("SELECT * FROM project_drafts ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ProjectDraftEntity>>

    @Query("SELECT * FROM project_drafts WHERE id = :id")
    suspend fun getById(id: String): ProjectDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: ProjectDraftEntity)

    @Query("DELETE FROM project_drafts WHERE id = :id")
    suspend fun deleteById(id: String)
}
