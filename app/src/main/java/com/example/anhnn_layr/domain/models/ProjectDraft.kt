package com.example.anhnn_layr.domain.models

import android.graphics.Bitmap
import android.net.Uri
import com.example.anhnn_layr.utils.TouchPath

data class DraftSummary(
    val id: String,
    val timestamp: Long,
    val sourceImageUri: Uri,
)

data class DraftSnapshot(
    val id: String,
    val timestamp: Long,
    val sourceImageUri: Uri,
    val sourceMimeType: String?,
    val processedBitmap: Bitmap,
    val editorState: EditorStateSnapshot,
    val touchPaths: List<TouchPath>,
)

data class EditorStateSnapshot(
    val selectedColorArgb: Long,
    val sourceMimeType: String?,
    val isEraseMode: Boolean,
    val brushSize: Float,
    val featherRadius: Float,
    val backgroundBlur: Float,
    val outlineWidth: Float,
    val outlineColorArgb: Long,
    val shadowRadius: Float,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
)
