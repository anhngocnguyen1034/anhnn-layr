package com.example.anhnn_layr.utils

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore

fun saveToGallery(
    ctx: Context,
    pngBytes: ByteArray,
    displayName: String = "rembg_${System.currentTimeMillis()}.png",
) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Rembg")
    }
    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("Không tạo được file đầu ra")
    ctx.contentResolver.openOutputStream(uri)?.use { it.write(pngBytes) }
}
