package com.example.anhnn_layr.domain.repository

import android.graphics.Bitmap
import android.net.Uri

data class RembgResult(
    val originalBitmap: Bitmap,
    val processedBytes: ByteArray,
)

data class NormalizedBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

interface RembgRepository {
    suspend fun removeBackground(
        imageUri: Uri,
        model: String = "u2net",
        postProcess: Boolean = true,
        bgColor: String? = null,
        samBox: NormalizedBox? = null,
    ): RembgResult
}
