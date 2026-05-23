package com.example.anhnn_layr.domain.repository

import android.graphics.Bitmap
import android.net.Uri

data class RembgResult(
    val originalBitmap: Bitmap,
    val processedBytes: ByteArray,
)

interface RembgRepository {
    suspend fun removeBackground(
        imageUri: Uri,
        model: String = "u2net",
        postProcess: Boolean = true,
        bgColor: String? = null,
    ): RembgResult
}
