package com.example.anhnn_layr.domain.usecases

import android.net.Uri
import com.example.anhnn_layr.domain.repository.RembgRepository
import javax.inject.Inject

class RemoveBackgroundUseCase @Inject constructor(
    private val repository: RembgRepository,
) {
    suspend operator fun invoke(
        imageUri: Uri,
        model: String = "u2net",
        postProcess: Boolean = true,
        bgColor: String? = null,
    ): ByteArray = repository.removeBackground(imageUri, model, postProcess, bgColor)
}
