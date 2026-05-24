package com.example.anhnn_layr.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import com.example.anhnn_layr.domain.models.DraftSnapshot
import com.example.anhnn_layr.domain.models.DraftSummary
import com.example.anhnn_layr.domain.models.EditorStateSnapshot
import com.example.anhnn_layr.utils.TouchPath
import kotlinx.coroutines.flow.Flow

interface DraftRepository {
    fun observeDrafts(): Flow<List<DraftSummary>>

    suspend fun saveDraft(
        sourceImageUri: Uri,
        sourceMimeType: String?,
        processedBitmap: Bitmap,
        editorState: EditorStateSnapshot,
        touchPaths: List<TouchPath>,
    ): String

    suspend fun loadDraft(id: String): DraftSnapshot?

    suspend fun deleteDraft(id: String)
}
