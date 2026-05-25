package com.example.anhnn_layr.domain.usecases

import android.net.Uri
import com.example.anhnn_layr.domain.repository.NormalizedBox
import com.example.anhnn_layr.domain.repository.RembgRepository
import com.example.anhnn_layr.domain.repository.RembgResult
import javax.inject.Inject

class RemoveBackgroundUseCase @Inject constructor(
    private val repository: RembgRepository,
) {
    suspend operator fun invoke(
        imageUri: Uri,
        model: String = "u2net",
        postProcess: Boolean = true,
        bgColor: String? = null,
        samBox: NormalizedBox? = null,
    ): RembgResult = repository.removeBackground(imageUri, model, postProcess, bgColor, samBox)
}
