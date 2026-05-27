package com.example.anhnn_layr.domain.repository

import android.net.Uri

data class UpscaleResult(
    val processedBytes: ByteArray,
)

interface UpscaleRepository {
    suspend fun upscale(
        imageUri: Uri,
        model: String = "RealESRGAN_x4plus",
        outscale: Float = 4f,
        half: Boolean = false,
        tile: Int = 0,
        tilePad: Int = 10,
        prePad: Int = 0,
    ): UpscaleResult
}
