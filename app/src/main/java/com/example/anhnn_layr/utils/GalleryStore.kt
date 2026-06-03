package com.example.anhnn_layr.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

/** Thư mục chứa ảnh chụp trong app — hiển thị ở màn Thư viện. */
private const val CAMERA_RELATIVE_DIR = "Pictures/AnhnnLayr"
private const val CAMERA_BUCKET = "AnhnnLayr"

/** Một ảnh trong thư viện ảnh đã chụp. */
data class GalleryPhoto(
    val id: Long,
    val uri: Uri,
    val displayName: String,
)

/**
 * Sao chép ảnh vừa chụp (file cache) vào thư viện ảnh của máy, trả về content [Uri].
 * Gọi trên Dispatchers.IO.
 */
fun saveCaptureToGallery(ctx: Context, source: File): Uri {
    val displayName = "anhnn_${System.currentTimeMillis()}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, CAMERA_RELATIVE_DIR)
        }
    }
    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("Không tạo được file trong thư viện")
    ctx.contentResolver.openOutputStream(uri)?.use { out ->
        source.inputStream().use { it.copyTo(out) }
    } ?: error("Không ghi được ảnh vào thư viện")
    return uri
}

/** Lấy danh sách ảnh đã chụp (mới nhất lên đầu). Gọi trên Dispatchers.IO. */
fun queryCapturedPhotos(ctx: Context): List<GalleryPhoto> {
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
    )
    val (selection, args) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?" to arrayOf("%$CAMERA_BUCKET%")
    } else {
        "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?" to arrayOf(CAMERA_BUCKET)
    }
    val sortOrder =
        "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC"

    val result = mutableListOf<GalleryPhoto>()
    ctx.contentResolver.query(collection, projection, selection, args, sortOrder)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            result.add(
                GalleryPhoto(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = cursor.getString(nameCol) ?: "",
                ),
            )
        }
    }
    return result
}

/** Xoá ảnh khỏi thư viện. Gọi trên Dispatchers.IO. */
fun deleteCapturedPhoto(ctx: Context, uri: Uri): Boolean =
    runCatching { ctx.contentResolver.delete(uri, null, null) > 0 }.getOrDefault(false)
