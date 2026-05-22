package com.example.anhnn_layr.domain.repository

import android.net.Uri

interface RembgRepository {
    suspend fun removeBackground(
        imageUri: Uri,
        model: String = "u2net",
        postProcess: Boolean = true,
        bgColor: String? = null,
    ): ByteArray
}
