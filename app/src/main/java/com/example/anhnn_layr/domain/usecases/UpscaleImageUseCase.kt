package com.example.anhnn_layr.domain.usecases

import android.net.Uri
import com.example.anhnn_layr.domain.repository.UpscaleRepository
import com.example.anhnn_layr.domain.repository.UpscaleResult
import javax.inject.Inject

class UpscaleImageUseCase @Inject constructor(
    private val repository: UpscaleRepository,
) {
    suspend operator fun invoke(
        imageUri: Uri,
        model: String = "RealESRGAN_x4plus",
        outscale: Float = 4f,
        tile: Int = 0,
    ): UpscaleResult = repository.upscale(
        imageUri = imageUri,
        model = model,
        outscale = outscale,
        tile = tile,
    )
}
