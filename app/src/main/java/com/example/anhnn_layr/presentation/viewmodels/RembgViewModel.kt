package com.example.anhnn_layr.presentation.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anhnn_layr.domain.usecases.RemoveBackgroundUseCase
import com.example.anhnn_layr.utils.SaveFormat
import com.example.anhnn_layr.utils.TouchPath
import com.example.anhnn_layr.utils.applyFeather
import com.example.anhnn_layr.utils.blurBackground
import com.example.anhnn_layr.utils.buildWorkingBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface RembgUiState {
    data object Idle : RembgUiState
    data object Loading : RembgUiState
    data class Success(
        val originalBitmap: Bitmap,
        val workingBitmap: Bitmap,
        val displayBitmap: Bitmap,
    ) : RembgUiState
    data class Error(val message: String) : RembgUiState
}

enum class EditorTool { BACKGROUND, ERASE, CROP, TEXT }

data class EditorState(
    val selectedColor: Color = Color.Transparent,
    val activeTool: EditorTool = EditorTool.BACKGROUND,
    val format: SaveFormat = SaveFormat.PNG,
    val isEraseMode: Boolean = true,
    val brushSize: Float = 40f,
    val paths: List<TouchPath> = emptyList(),
    val redoStack: List<TouchPath> = emptyList(),
    val featherRadius: Float = 0f,
    val backgroundBitmap: Bitmap? = null,
    val backgroundBlur: Float = 0f,
    val blurredBackgroundBitmap: Bitmap? = null,
)

@HiltViewModel
class RembgViewModel @Inject constructor(
    private val removeBackground: RemoveBackgroundUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<RembgUiState>(RembgUiState.Idle)
    val state: StateFlow<RembgUiState> = _state.asStateFlow()

    private val _editor = MutableStateFlow(EditorState())
    val editor: StateFlow<EditorState> = _editor.asStateFlow()

    private var processedBitmap: Bitmap? = null
    private var originalBitmap: Bitmap? = null

    fun remove(uri: Uri, model: String = "u2net") {
        _state.value = RembgUiState.Loading
        _editor.value = EditorState()
        viewModelScope.launch {
            runCatching { removeBackground(uri, model = model) }
                .onSuccess { result ->
                    val processed = withContext(Dispatchers.IO) {
                        BitmapFactory.decodeByteArray(
                            result.processedBytes, 0, result.processedBytes.size,
                        )
                    } ?: run {
                        _state.value = RembgUiState.Error("Không đọc được ảnh kết quả")
                        return@onSuccess
                    }
                    processedBitmap = processed
                    originalBitmap = result.originalBitmap
                    val working = processed.copy(Bitmap.Config.ARGB_8888, true)
                    _state.value = RembgUiState.Success(
                        originalBitmap = result.originalBitmap,
                        workingBitmap = working,
                        displayBitmap = working,
                    )
                }
                .onFailure {
                    _state.value = RembgUiState.Error(it.message ?: "Lỗi không xác định")
                }
        }
    }

    fun setColor(color: Color) = _editor.update {
        it.copy(
            selectedColor = color,
            format = if (color == Color.Transparent) SaveFormat.PNG else it.format,
        )
    }

    fun setTool(tool: EditorTool) = _editor.update { it.copy(activeTool = tool) }
    fun setFormat(format: SaveFormat) = _editor.update { it.copy(format = format) }
    fun setEraseMode(isErase: Boolean) = _editor.update { it.copy(isEraseMode = isErase) }
    fun setBrushSize(size: Float) = _editor.update { it.copy(brushSize = size) }
    fun setFeatherRadius(radius: Float) {
        _editor.update { it.copy(featherRadius = radius) }
        rebuildDisplay()
    }

    fun setBackgroundImage(bitmap: Bitmap?) {
        _editor.update { it.copy(backgroundBitmap = bitmap) }
        rebuildBlurredBackground()
    }

    fun setBackgroundBlur(intensity: Float) {
        _editor.update { it.copy(backgroundBlur = intensity) }
        rebuildBlurredBackground()
    }

    private fun rebuildBlurredBackground() {
        val current = _editor.value
        val src = current.backgroundBitmap
        if (src == null) {
            _editor.update { it.copy(blurredBackgroundBitmap = null) }
            return
        }
        val intensity = current.backgroundBlur
        viewModelScope.launch {
            val blurred = withContext(Dispatchers.Default) { blurBackground(src, intensity) }
            _editor.update { it.copy(blurredBackgroundBitmap = blurred) }
        }
    }

    fun commitPath(touch: TouchPath) {
        val newPaths = _editor.value.paths + touch
        _editor.update { it.copy(paths = newPaths, redoStack = emptyList()) }
        rebuildWorking(newPaths)
    }

    fun undo() {
        val cur = _editor.value
        if (cur.paths.isEmpty()) return
        val popped = cur.paths.last()
        val newPaths = cur.paths.dropLast(1)
        _editor.update { it.copy(paths = newPaths, redoStack = it.redoStack + popped) }
        rebuildWorking(newPaths)
    }

    fun redo() {
        val cur = _editor.value
        if (cur.redoStack.isEmpty()) return
        val popped = cur.redoStack.last()
        val newPaths = cur.paths + popped
        _editor.update { it.copy(paths = newPaths, redoStack = it.redoStack.dropLast(1)) }
        rebuildWorking(newPaths)
    }

    private fun rebuildWorking(paths: List<TouchPath>) {
        val proc = processedBitmap ?: return
        val orig = originalBitmap ?: return
        val radius = _editor.value.featherRadius
        viewModelScope.launch {
            val (working, display) = withContext(Dispatchers.Default) {
                val w = buildWorkingBitmap(proc, orig, paths)
                w to applyFeather(w, radius)
            }
            val current = _state.value
            if (current is RembgUiState.Success) {
                _state.value = current.copy(workingBitmap = working, displayBitmap = display)
            }
        }
    }

    private fun rebuildDisplay() {
        val current = _state.value as? RembgUiState.Success ?: return
        val radius = _editor.value.featherRadius
        viewModelScope.launch {
            val display = withContext(Dispatchers.Default) {
                applyFeather(current.workingBitmap, radius)
            }
            val now = _state.value
            if (now is RembgUiState.Success) {
                _state.value = now.copy(displayBitmap = display)
            }
        }
    }

    fun reset() {
        processedBitmap = null
        originalBitmap = null
        _state.value = RembgUiState.Idle
        _editor.value = EditorState()
    }
}
