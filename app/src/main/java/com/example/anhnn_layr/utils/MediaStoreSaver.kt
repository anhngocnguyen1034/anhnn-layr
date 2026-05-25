package com.example.anhnn_layr.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore

private const val RELATIVE_DIR = "Pictures/TayMaySticker"

enum class SaveFormat(val mime: String, val ext: String) {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg"),
    WEBP("image/webp", "webp");

    fun compressFormat(): Bitmap.CompressFormat = when (this) {
        PNG -> Bitmap.CompressFormat.PNG
        JPEG -> Bitmap.CompressFormat.JPEG
        WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSLESS
        } else {
            @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
        }
    }
}

fun pickSaveFormat(sourceMime: String?, hasTransparency: Boolean): SaveFormat {
    if (hasTransparency) {
        return if (sourceMime == "image/webp") SaveFormat.WEBP else SaveFormat.PNG
    }
    return when (sourceMime) {
        "image/jpeg", "image/jpg" -> SaveFormat.JPEG
        "image/webp" -> SaveFormat.WEBP
        "image/png" -> SaveFormat.PNG
        else -> SaveFormat.PNG
    }
}

fun saveBitmapToGallery(
    ctx: Context,
    bitmap: Bitmap,
    format: SaveFormat,
    quality: Int = 100,
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
        bitmap.compress(format.compressFormat(), quality, out)
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
