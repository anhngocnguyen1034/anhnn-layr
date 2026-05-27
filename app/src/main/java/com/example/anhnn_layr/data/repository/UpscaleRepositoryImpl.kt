package com.example.anhnn_layr.data.repository

import android.content.Context
import android.net.Uri
import com.example.anhnn_layr.data.datasource.RembgApi
import com.example.anhnn_layr.domain.repository.UpscaleRepository
import com.example.anhnn_layr.domain.repository.UpscaleResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpscaleRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val api: RembgApi,
) : UpscaleRepository {

    override suspend fun upscale(
        imageUri: Uri,
        model: String,
        outscale: Float,
        half: Boolean,
        tile: Int,
        tilePad: Int,
        prePad: Int,
    ): UpscaleResult = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        val mime = resolver.getType(imageUri) ?: "image/*"
        val bytes = resolver.openInputStream(imageUri)?.use { it.readBytes() }
            ?: error("Không mở được ảnh đầu vào")

        val filename = "input." + when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            else -> "jpg"
        }
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = filename,
            body = bytes.toRequestBody(mime.toMediaTypeOrNull()),
        )

        val response = api.upscale(
            file = part,
            model = model,
            outscale = outscale,
            half = half,
            tile = tile,
            tilePad = tilePad,
            prePad = prePad,
        )
        if (!response.isSuccessful) {
            val errBody = response.errorBody()?.string().orEmpty()
            error("HTTP ${response.code()} từ /api/upscale: $errBody")
        }
        val processed = response.body()?.bytes()
            ?: error("Server trả body rỗng")

        UpscaleResult(processedBytes = processed)
    }
}
