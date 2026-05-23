package com.example.anhnn_layr.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore

private const val RELATIVE_DIR = "Pictures/TayMaySticker"

enum class SaveFormat(val mime: String, val ext: String, val compress: Bitmap.CompressFormat) {
    PNG("image/png", "png", Bitmap.CompressFormat.PNG),
    JPEG("image/jpeg", "jpg", Bitmap.CompressFormat.JPEG),
}

fun saveBitmapToGallery(
    ctx: Context,
    bitmap: Bitmap,
    format: SaveFormat,
    quality: Int = 95,
    displayName: String = "sticker_${System.currentTimeMillis()}.${format.ext}",
) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, format.mime)
        put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_DIR)
    }
    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("Không tạo được file đầu ra")
    ctx.contentResolver.openOutputStream(uri)?.use { out ->
        bitmap.compress(format.compress, quality, out)
    } ?: error("Không mở được output stream")
}

fun saveToGallery(
    ctx: Context,
    pngBytes: ByteArray,
    displayName: String = "rembg_${System.currentTimeMillis()}.png",
) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_DIR)
    }
    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("Không tạo được file đầu ra")
    ctx.contentResolver.openOutputStream(uri)?.use { it.write(pngBytes) }
}
