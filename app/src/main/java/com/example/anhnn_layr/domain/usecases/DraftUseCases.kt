package com.example.anhnn_layr.domain.usecases

import android.graphics.Bitmap
import android.net.Uri
import com.example.anhnn_layr.domain.models.DraftSnapshot
import com.example.anhnn_layr.domain.models.DraftSummary
import com.example.anhnn_layr.domain.models.EditorStateSnapshot
import com.example.anhnn_layr.domain.repository.DraftRepository
import com.example.anhnn_layr.utils.TouchPath
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDraftsUseCase @Inject constructor(
    private val repository: DraftRepository,
) {
    operator fun invoke(): Flow<List<DraftSummary>> = repository.observeDrafts()
}

class SaveDraftUseCase @Inject constructor(
    private val repository: DraftRepository,
) {
    suspend operator fun invoke(
        sourceImageUri: Uri,
        sourceMimeType: String?,
        processedBitmap: Bitmap,
        editorState: EditorStateSnapshot,
        touchPaths: List<TouchPath>,
    ): String = repository.saveDraft(
        sourceImageUri = sourceImageUri,
        sourceMimeType = sourceMimeType,
        processedBitmap = processedBitmap,
        editorState = editorState,
        touchPaths = touchPaths,
    )
}

class LoadDraftUseCase @Inject constructor(
    private val repository: DraftRepository,
) {
    suspend operator fun invoke(id: String): DraftSnapshot? = repository.loadDraft(id)
}

class DeleteDraftUseCase @Inject constructor(
    private val repository: DraftRepository,
) {
    suspend operator fun invoke(id: String) = repository.deleteDraft(id)
}
