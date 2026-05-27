package com.example.anhnn_layr.presentation.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anhnn_layr.domain.usecases.UpscaleImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface UpscaleUiState {
    data object Idle : UpscaleUiState
    data class Loading(val sourceUri: Uri) : UpscaleUiState
    data class Success(
        val sourceUri: Uri,
        val sourceMimeType: String?,
        val resultBitmap: Bitmap,
        val resultBytes: ByteArray,
    ) : UpscaleUiState
    data class Error(val message: String) : UpscaleUiState
}

data class UpscaleSettings(
    val model: String = "RealESRGAN_x4plus",
    val outscale: Float = 4f,
    val tile: Int = 0,
)

@HiltViewModel
class UpscaleViewModel @Inject constructor(
    private val upscaleImage: UpscaleImageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<UpscaleUiState>(UpscaleUiState.Idle)
    val state: StateFlow<UpscaleUiState> = _state.asStateFlow()

    private val _settings = MutableStateFlow(UpscaleSettings())
    val settings: StateFlow<UpscaleSettings> = _settings.asStateFlow()

    fun setModel(model: String) {
        _settings.value = _settings.value.copy(model = model)
    }

    fun setOutscale(value: Float) {
        _settings.value = _settings.value.copy(outscale = value.coerceIn(1f, 8f))
    }

    fun setTile(value: Int) {
        _settings.value = _settings.value.copy(tile = value.coerceAtLeast(0))
    }

    fun run(uri: Uri, sourceMimeType: String?) {
        val s = _settings.value
        _state.value = UpscaleUiState.Loading(uri)
        viewModelScope.launch {
            runCatching {
                val result = upscaleImage(
                    imageUri = uri,
                    model = s.model,
                    outscale = s.outscale,
                    tile = s.tile,
                )
                val bmp = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeByteArray(
                        result.processedBytes, 0, result.processedBytes.size,
                    )
                } ?: error("Không đọc được ảnh kết quả")
                bmp to result.processedBytes
            }.onSuccess { (bmp, bytes) ->
                _state.value = UpscaleUiState.Success(
                    sourceUri = uri,
                    sourceMimeType = sourceMimeType,
                    resultBitmap = bmp,
                    resultBytes = bytes,
                )
            }.onFailure {
                _state.value = UpscaleUiState.Error(it.message ?: "Lỗi không xác định")
            }
        }
    }

    fun reset() {
        _state.value = UpscaleUiState.Idle
    }
}
