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
    // Bản nháp cũ chỉ có isEraseMode; bản mới dùng brushMode (ERASE/RESTORE/PAINT).
    val isEraseMode: Boolean? = null,
    val brushMode: String? = null,
    val brushColorArgb: Long? = null,
    val brushSize: Float,
    val featherRadius: Float,
    val backgroundBlur: Float,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val textStickers: List<TextStickerSnapshot>? = null,
    val selectedTextStickerId: String? = null,
    // null = bản nháp cũ (trước khi có luồng "Chỉnh ảnh") → coi như đã xoá nền.
    val isBackgroundRemoved: Boolean? = null,
    // Chỉnh mặt — null = bản nháp cũ (trước khi lưu nhóm này) → mặc định 0 / màu mặc định.
    val eyeEnlarge: Float? = null,
    val lipColor: Float? = null,
    val lipShadeArgb: Long? = null,
    val teethWhiten: Float? = null,
    val blush: Float? = null,
    val faceSlim: Float? = null,
    val skinSmooth: Float? = null,
    val skinBrighten: Float? = null,
)

data class TextStickerSnapshot(
    val id: String,
    val text: String,
    val centerX: Float,
    val centerY: Float,
    val textColorArgb: Long,
    val outlineColorArgb: Long,
    val outlineWidth: Float,
    val shadowRadius: Float,
    val fontSize: Float,
    val rotation: Float,
    val scale: Float,
    val font: String,
)
