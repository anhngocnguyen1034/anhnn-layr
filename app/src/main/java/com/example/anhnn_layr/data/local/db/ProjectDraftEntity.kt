package com.example.anhnn_layr.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_drafts")
data class ProjectDraftEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val sourceImageUri: String,
    val sourceMimeType: String?,
    val processedPngPath: String,
    val serializedEditorState: String,
    val serializedTouchPaths: String,
)
