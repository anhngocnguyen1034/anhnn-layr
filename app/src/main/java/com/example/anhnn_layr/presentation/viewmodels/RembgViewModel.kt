package com.example.anhnn_layr.presentation.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anhnn_layr.domain.usecases.RemoveBackgroundUseCase
import com.example.anhnn_layr.utils.TouchPath
import com.example.anhnn_layr.utils.applyFeather
import com.example.anhnn_layr.utils.applySubjectEffects
import com.example.anhnn_layr.utils.blurBackground
import com.example.anhnn_layr.utils.buildWorkingBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
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
        val effectedBitmap: Bitmap,
    ) : RembgUiState
    data class Error(val message: String) : RembgUiState
}

enum class EditorTool { BACKGROUND, ERASE, EFFECTS, CROP, TEXT }

data class EditorState(
    val selectedColor: Color = Color.Transparent,
    val activeTool: EditorTool = EditorTool.BACKGROUND,
    val sourceMimeType: String? = null,
    val isEraseMode: Boolean = true,
    val brushSize: Float = 40f,
    val paths: List<TouchPath> = emptyList(),
    val redoStack: List<TouchPath> = emptyList(),
    val featherRadius: Float = 0f,
    val backgroundBitmap: Bitmap? = null,
    val backgroundBlur: Float = 0f,
    val blurredBackgroundBitmap: Bitmap? = null,
    val outlineWidth: Float = 0f,
    val outlineColor: Color = Color.White,
    val shadowRadius: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
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

    private val subjectTrigger = Channel<Boolean>(Channel.CONFLATED)
    private val effectsTrigger = Channel<Unit>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            subjectTrigger.consumeAsFlow().collect { rebuildWorking ->
                runSubjectRecompose(rebuildWorking)
            }
        }
        viewModelScope.launch {
            effectsTrigger.consumeAsFlow().collect {
                runEffectsRecompose()
            }
        }
    }

    fun remove(uri: Uri, model: String = "u2net", sourceMimeType: String? = null) {
        _state.value = RembgUiState.Loading
        _editor.value = EditorState(sourceMimeType = sourceMimeType)
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
                        effectedBitmap = working,
                    )
                }
                .onFailure {
                    _state.value = RembgUiState.Error(it.message ?: "Lỗi không xác định")
                }
        }
    }

    fun setColor(color: Color) = _editor.update { it.copy(selectedColor = color) }

    fun setTool(tool: EditorTool) = _editor.update { it.copy(activeTool = tool) }
    fun setEraseMode(isErase: Boolean) = _editor.update { it.copy(isEraseMode = isErase) }
    fun setBrushSize(size: Float) = _editor.update { it.copy(brushSize = size) }

    fun setFeatherRadius(radius: Float) {
        _editor.update { it.copy(featherRadius = radius) }
        recomposeSubject(rebuildWorking = false)
    }

    fun setOutlineWidth(width: Float) {
        _editor.update { it.copy(outlineWidth = width) }
        recomposeEffected()
    }

    fun setOutlineColor(color: Color) {
        _editor.update { it.copy(outlineColor = color) }
        recomposeEffected()
    }

    fun setShadowRadius(radius: Float) {
        _editor.update { it.copy(shadowRadius = radius) }
        recomposeEffected()
    }

    fun setBrightness(value: Float) {
        _editor.update { it.copy(brightness = value) }
        recomposeEffected()
    }

    fun setContrast(value: Float) {
        _editor.update { it.copy(contrast = value) }
        recomposeEffected()
    }

    fun setSaturation(value: Float) {
        _editor.update { it.copy(saturation = value) }
        recomposeEffected()
    }

    fun useOriginalAsBackground() {
        val orig = originalBitmap ?: return
        setBackgroundImage(orig)
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
        recomposeSubject(rebuildWorking = true)
    }

    fun undo() {
        val cur = _editor.value
        if (cur.paths.isEmpty()) return
        val popped = cur.paths.last()
        val newPaths = cur.paths.dropLast(1)
        _editor.update { it.copy(paths = newPaths, redoStack = it.redoStack + popped) }
        recomposeSubject(rebuildWorking = true)
    }

    fun redo() {
        val cur = _editor.value
        if (cur.redoStack.isEmpty()) return
        val popped = cur.redoStack.last()
        val newPaths = cur.paths + popped
        _editor.update { it.copy(paths = newPaths, redoStack = it.redoStack.dropLast(1)) }
        recomposeSubject(rebuildWorking = true)
    }

    private fun recomposeSubject(rebuildWorking: Boolean) {
        subjectTrigger.trySend(rebuildWorking)
    }

    private fun recomposeEffected() {
        effectsTrigger.trySend(Unit)
    }

    private suspend fun runSubjectRecompose(rebuildWorking: Boolean) {
        val proc = processedBitmap ?: return
        val orig = originalBitmap ?: return
        val ed = _editor.value
        val current = _state.value as? RembgUiState.Success
        val result = withContext(Dispatchers.Default) {
            val working = if (rebuildWorking || current == null) {
                buildWorkingBitmap(proc, orig, ed.paths)
            } else current.workingBitmap
            val display = applyFeather(working, ed.featherRadius)
            val effected = applySubjectEffects(
                subject = display,
                outlineWidth = ed.outlineWidth,
                outlineColor = ed.outlineColor,
                shadowRadius = ed.shadowRadius,
                brightness = ed.brightness,
                contrast = ed.contrast,
                saturation = ed.saturation,
            )
            Triple(working, display, effected)
        }
        val now = _state.value
        if (now is RembgUiState.Success) {
            _state.value = now.copy(
                workingBitmap = result.first,
                displayBitmap = result.second,
                effectedBitmap = result.third,
            )
        }
    }

    private suspend fun runEffectsRecompose() {
        val current = _state.value as? RembgUiState.Success ?: return
        val ed = _editor.value
        val effected = withContext(Dispatchers.Default) {
            applySubjectEffects(
                subject = current.displayBitmap,
                outlineWidth = ed.outlineWidth,
                outlineColor = ed.outlineColor,
                shadowRadius = ed.shadowRadius,
                brightness = ed.brightness,
                contrast = ed.contrast,
                saturation = ed.saturation,
            )
        }
        val now = _state.value
        if (now is RembgUiState.Success) {
            _state.value = now.copy(effectedBitmap = effected)
        }
    }

    fun reset() {
        processedBitmap = null
        originalBitmap = null
        _state.value = RembgUiState.Idle
        _editor.value = EditorState()
    }
}
