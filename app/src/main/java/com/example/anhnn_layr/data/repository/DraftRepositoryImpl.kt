package com.example.anhnn_layr.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.example.anhnn_layr.data.local.db.ProjectDraftDao
import com.example.anhnn_layr.data.local.db.ProjectDraftEntity
import com.example.anhnn_layr.data.local.storage.DraftFileStore
import com.example.anhnn_layr.domain.models.DraftSnapshot
import com.example.anhnn_layr.domain.models.DraftSummary
import com.example.anhnn_layr.domain.models.EditorStateSnapshot
import com.example.anhnn_layr.domain.repository.DraftRepository
import com.example.anhnn_layr.utils.BrushMode
import com.example.anhnn_layr.utils.TouchPath
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private data class PointDto(val x: Float, val y: Float)
private data class TouchPathDto(
    val points: List<PointDto>,
    // Bản nháp cũ chỉ có isErase; bản mới dùng mode (ERASE/RESTORE/PAINT) + color.
    val isErase: Boolean? = null,
    val mode: String? = null,
    val color: Int? = null,
    val brushSize: Float,
)

@Singleton
class DraftRepositoryImpl @Inject constructor(
    private val dao: ProjectDraftDao,
    private val fileStore: DraftFileStore,
    private val gson: Gson,
) : DraftRepository {

    override fun observeDrafts(): Flow<List<DraftSummary>> =
        dao.observeAll().map { rows ->
            rows.map {
                DraftSummary(
                    id = it.id,
                    timestamp = it.timestamp,
                    sourceImageUri = Uri.parse(it.sourceImageUri),
                )
            }
        }

    override suspend fun saveDraft(
        sourceImageUri: Uri,
        sourceMimeType: String?,
        processedBitmap: Bitmap,
        editorState: EditorStateSnapshot,
        touchPaths: List<TouchPath>,
    ): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val pngPath = fileStore.saveProcessedPng(id, processedBitmap)
        val dtoList = touchPaths.map { tp ->
            TouchPathDto(
                points = tp.points.map { PointDto(it.x, it.y) },
                mode = tp.mode.name,
                color = tp.color,
                brushSize = tp.brushSize,
            )
        }
        dao.upsert(
            ProjectDraftEntity(
                id = id,
                timestamp = System.currentTimeMillis(),
                sourceImageUri = sourceImageUri.toString(),
                sourceMimeType = sourceMimeType,
                processedPngPath = pngPath,
                serializedEditorState = gson.toJson(editorState),
                serializedTouchPaths = gson.toJson(dtoList),
            ),
        )
        id
    }

    override suspend fun loadDraft(id: String): DraftSnapshot? = withContext(Dispatchers.IO) {
        val row = dao.getById(id) ?: return@withContext null
        val processed = fileStore.readProcessedBitmap(row.processedPngPath)
            ?: return@withContext null
        val state = gson.fromJson(row.serializedEditorState, EditorStateSnapshot::class.java)
        val pathDtoType = object : TypeToken<List<TouchPathDto>>() {}.type
        val pathDtos: List<TouchPathDto> = gson.fromJson(row.serializedTouchPaths, pathDtoType)
        val paths = pathDtos.map { dto ->
            val mode = dto.mode?.let { runCatching { BrushMode.valueOf(it) }.getOrNull() }
                ?: if (dto.isErase != false) BrushMode.ERASE else BrushMode.RESTORE
            TouchPath(
                points = dto.points.map { androidx.compose.ui.geometry.Offset(it.x, it.y) },
                mode = mode,
                color = dto.color ?: android.graphics.Color.BLACK,
                brushSize = dto.brushSize,
            )
        }
        DraftSnapshot(
            id = row.id,
            timestamp = row.timestamp,
            sourceImageUri = Uri.parse(row.sourceImageUri),
            sourceMimeType = row.sourceMimeType,
            processedBitmap = processed,
            editorState = state,
            touchPaths = paths,
        )
    }

    override suspend fun deleteDraft(id: String) = withContext(Dispatchers.IO) {
        val row = dao.getById(id) ?: return@withContext
        fileStore.delete(row.processedPngPath)
        dao.deleteById(id)
    }
}
