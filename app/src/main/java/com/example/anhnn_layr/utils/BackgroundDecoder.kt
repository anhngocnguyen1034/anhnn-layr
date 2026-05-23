package com.example.anhnn_layr.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

private const val MAX_BG_DIMENSION = 2000

fun decodeBackgroundBitmap(ctx: Context, uri: Uri): Bitmap? {
    val resolver = ctx.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val w = bounds.outWidth
    val h = bounds.outHeight
    if (w <= 0 || h <= 0) return null

    var sample = 1
    var cw = w
    var ch = h
    while (cw / 2 >= MAX_BG_DIMENSION && ch / 2 >= MAX_BG_DIMENSION) {
        cw /= 2; ch /= 2; sample *= 2
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val raw = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    } ?: return null

    val longest = maxOf(raw.width, raw.height)
    if (longest <= MAX_BG_DIMENSION) return raw
    val ratio = MAX_BG_DIMENSION.toFloat() / longest
    val scaled = Bitmap.createScaledBitmap(
        raw,
        (raw.width * ratio).toInt().coerceAtLeast(1),
        (raw.height * ratio).toInt().coerceAtLeast(1),
        true,
    )
    if (scaled !== raw) raw.recycle()
    return scaled
}
