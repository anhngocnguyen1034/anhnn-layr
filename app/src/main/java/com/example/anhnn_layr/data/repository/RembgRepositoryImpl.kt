package com.example.anhnn_layr.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.anhnn_layr.data.datasource.RembgApi
import com.example.anhnn_layr.domain.repository.RembgRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_DIMENSION_PX = 1500
private const val JPEG_QUALITY = 90

@Singleton
class RembgRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val api: RembgApi,
) : RembgRepository {

    override suspend fun removeBackground(
        imageUri: Uri,
        model: String,
        postProcess: Boolean,
        bgColor: String?,
    ): ByteArray = withContext(Dispatchers.IO) {
        val payload = readAndDownscale(imageUri)

        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = "input.jpg",
            body = payload.toRequestBody("image/jpeg".toMediaTypeOrNull()),
        )

        api.removeBackground(
            file = part,
            model = model,
            postProcessMask = postProcess,
            backgroundColor = bgColor,
        ).bytes()
    }

    private fun readAndDownscale(uri: Uri): ByteArray {
        val resolver = appContext.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri)
            ?: error("Không mở được ảnh đầu vào (content resolver trả về null)")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }

        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) error("Ảnh đầu vào không hợp lệ hoặc định dạng không hỗ trợ")

        val sample = computeInSampleSize(w, h, MAX_DIMENSION_PX)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decodeStream = resolver.openInputStream(uri)
            ?: error("Không mở được ảnh đầu vào ở bước decode")
        val sampled = decodeStream.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
            ?: error("Không decode được ảnh (định dạng không hỗ trợ?)")

        val scaled = scaleToMaxDimension(sampled, MAX_DIMENSION_PX)
        if (scaled !== sampled) sampled.recycle()

        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            scaled.recycle()
            out.toByteArray()
        }
    }

    private fun computeInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxDim && h / 2 >= maxDim) {
            w /= 2; h /= 2; sample *= 2
        }
        return sample
    }

    private fun scaleToMaxDimension(src: Bitmap, maxDim: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= maxDim) return src
        val ratio = maxDim.toFloat() / longest
        val newW = (src.width * ratio).toInt().coerceAtLeast(1)
        val newH = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }
}
