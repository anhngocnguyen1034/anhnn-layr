package com.example.anhnn_layr.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

private const val MAX_DIMENSION = 3000

/**
 * Giải mã ảnh từ [uri] và xoay/lật về đúng chiều theo EXIF, đồng thời thu nhỏ nếu
 * ảnh quá lớn (tránh OOM). [BitmapFactory.decodeStream] KHÔNG tự áp EXIF nên ảnh
 * chụp dọc thường bị nằm ngang — hàm này khắc phục cho luồng nạp ảnh trực tiếp.
 */
fun decodeUprightBitmap(ctx: Context, uri: Uri): Bitmap? {
    val resolver = ctx.contentResolver

    // 1. Đọc kích thước trước để tính inSampleSize.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val w = bounds.outWidth
    val h = bounds.outHeight
    if (w <= 0 || h <= 0) return null

    var sample = 1
    while (w / sample / 2 >= MAX_DIMENSION && h / sample / 2 >= MAX_DIMENSION) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    } ?: return null

    // 2. Đọc orientation EXIF (mở stream riêng).
    val orientation = runCatching {
        resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    // 3. Áp ma trận xoay/lật tương ứng.
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f); matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f); matrix.postScale(-1f, 1f)
        }
        else -> return decoded // NORMAL/UNDEFINED: giữ nguyên
    }

    val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    if (upright !== decoded) decoded.recycle()
    return upright
}
