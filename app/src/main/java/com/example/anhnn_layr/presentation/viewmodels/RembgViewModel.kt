package com.example.anhnn_layr.presentation.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anhnn_layr.domain.models.DraftSummary
import com.example.anhnn_layr.domain.models.EditorStateSnapshot
import com.example.anhnn_layr.domain.models.TextStickerSnapshot
import com.example.anhnn_layr.domain.usecases.DeleteDraftUseCase
import com.example.anhnn_layr.domain.usecases.LoadDraftUseCase
import com.example.anhnn_layr.domain.usecases.ObserveDraftsUseCase
import com.example.anhnn_layr.domain.usecases.RemoveBackgroundUseCase
import com.example.anhnn_layr.domain.usecases.SaveDraftUseCase
import com.example.anhnn_layr.utils.TouchPath
import com.example.anhnn_layr.utils.TextSticker
import com.example.anhnn_layr.utils.TextStickerFont
import com.example.anhnn_layr.utils.applyFeather
import com.example.anhnn_layr.utils.applySubjectEffects
import com.example.anhnn_layr.utils.blurBackground
import com.example.anhnn_layr.utils.buildWorkingBitmap
import com.example.anhnn_layr.utils.centerCropBounds
import com.example.anhnn_layr.utils.crop
import com.example.anhnn_layr.utils.translatedAfterCrop
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

sealed interface RembgUiState {
    data object Idle : RembgUiState
    data class Loading(val sourceUri: Uri? = null) : RembgUiState
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
    val textStickers: List<TextSticker> = emptyList(),
    val selectedTextStickerId: String? = null,
)

@HiltViewModel
class RembgViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val removeBackground: RemoveBackgroundUseCase,
    private val saveDraft: SaveDraftUseCase,
    private val loadDraft: LoadDraftUseCase,
    private val deleteDraftUseCase: DeleteDraftUseCase,
    observeDrafts: ObserveDraftsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<RembgUiState>(RembgUiState.Idle)
    val state: StateFlow<RembgUiState> = _state.asStateFlow()

    private val _editor = MutableStateFlow(EditorState())
    val editor: StateFlow<EditorState> = _editor.asStateFlow()

    val drafts: StateFlow<List<DraftSummary>> = observeDrafts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var processedBitmap: Bitmap? = null
    private var originalBitmap: Bitmap? = null
    private var currentSourceUri: Uri? = null

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

    fun remove(
        uri: Uri,
        model: String = "isnet-general-use",
        sourceMimeType: String? = null,
    ) {
        _state.value = RembgUiState.Loading(sourceUri = uri)
        _editor.value = EditorState(sourceMimeType = sourceMimeType)
        currentSourceUri = uri
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

    fun saveCurrentDraft() {
        val proc = processedBitmap ?: return
        val uri = currentSourceUri ?: return
        val ed = _editor.value
        viewModelScope.launch {
            runCatching {
                saveDraft(
                    sourceImageUri = uri,
                    sourceMimeType = ed.sourceMimeType,
                    processedBitmap = proc,
                    editorState = ed.toSnapshot(),
                    touchPaths = ed.paths,
                )
            }.onSuccess { _messages.tryEmit("Đã lưu bản nháp") }
                .onFailure { _messages.tryEmit("Lưu nháp thất bại: ${it.message}") }
        }
    }

    fun openDraft(id: String) {
        _state.value = RembgUiState.Loading()
        viewModelScope.launch {
            val snapshot = runCatching { loadDraft(id) }.getOrNull()
            if (snapshot == null) {
                _state.value = RembgUiState.Error("Không mở được bản nháp")
                return@launch
            }
            val original = withContext(Dispatchers.IO) {
                appContext.contentResolver.openInputStream(snapshot.sourceImageUri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            }
            if (original == null) {
                _state.value = RembgUiState.Error("Không đọc được ảnh gốc của bản nháp")
                return@launch
            }
            processedBitmap = snapshot.processedBitmap
            originalBitmap = original
            currentSourceUri = snapshot.sourceImageUri
            val working = snapshot.processedBitmap.copy(Bitmap.Config.ARGB_8888, true)
            _state.value = RembgUiState.Success(
                originalBitmap = original,
                workingBitmap = working,
                displayBitmap = working,
                effectedBitmap = working,
            )
            _editor.value = snapshot.editorState.toEditorState(paths = snapshot.touchPaths)
            subjectTrigger.trySend(true)
        }
    }

    fun deleteDraft(id: String) {
        viewModelScope.launch {
            runCatching { deleteDraftUseCase(id) }
                .onFailure { _messages.tryEmit("Xoá nháp thất bại: ${it.message}") }
        }
    }

    fun setColor(color: Color) = _editor.update { it.copy(selectedColor = color) }

    fun setTool(tool: EditorTool) {
        if (tool == EditorTool.TEXT) {
            ensureTextStickerSelected()
        } else {
            _editor.update { it.copy(activeTool = tool) }
        }
    }
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

    fun addTextSticker() {
        val success = _state.value as? RembgUiState.Success ?: return
        val sticker = createDefaultTextSticker(success.effectedBitmap)
        _editor.update {
            it.copy(
                activeTool = EditorTool.TEXT,
                textStickers = it.textStickers + sticker,
                selectedTextStickerId = sticker.id,
            )
        }
    }

    private fun ensureTextStickerSelected() {
        val success = _state.value as? RembgUiState.Success
        if (success == null) {
            _editor.update { it.copy(activeTool = EditorTool.TEXT) }
            return
        }

        _editor.update { editor ->
            val selectedExists = editor.selectedTextStickerId != null &&
                editor.textStickers.any { it.id == editor.selectedTextStickerId }
            when {
                editor.textStickers.isEmpty() -> {
                    val sticker = createDefaultTextSticker(success.effectedBitmap)
                    editor.copy(
                        activeTool = EditorTool.TEXT,
                        textStickers = listOf(sticker),
                        selectedTextStickerId = sticker.id,
                    )
                }
                selectedExists -> editor.copy(activeTool = EditorTool.TEXT)
                else -> editor.copy(
                    activeTool = EditorTool.TEXT,
                    selectedTextStickerId = editor.textStickers.last().id,
                )
            }
        }
    }

    private fun createDefaultTextSticker(bitmap: Bitmap): TextSticker = TextSticker(
        id = UUID.randomUUID().toString(),
        text = "Chữ mới",
        center = Offset(
            x = bitmap.width / 2f,
            y = bitmap.height / 2f,
        ),
    )

    fun selectTextSticker(id: String) {
        _editor.update { it.copy(selectedTextStickerId = id) }
    }

    fun deleteSelectedTextSticker() {
        val selectedId = _editor.value.selectedTextStickerId ?: return
        _editor.update { editor ->
            val remaining = editor.textStickers.filterNot { it.id == selectedId }
            editor.copy(
                textStickers = remaining,
                selectedTextStickerId = remaining.lastOrNull()?.id,
            )
        }
    }

    fun setSelectedText(value: String) = updateSelectedTextSticker { it.copy(text = value) }
    fun setSelectedTextFont(font: TextStickerFont) = updateSelectedTextSticker { it.copy(font = font) }
    fun setSelectedTextColor(color: Color) = updateSelectedTextSticker { it.copy(textColor = color) }
    fun setSelectedTextOutlineColor(color: Color) = updateSelectedTextSticker {
        it.copy(outlineColor = color)
    }
    fun setSelectedTextOutlineWidth(width: Float) = updateSelectedTextSticker {
        it.copy(outlineWidth = width)
    }
    fun setSelectedTextShadowRadius(radius: Float) = updateSelectedTextSticker {
        it.copy(shadowRadius = radius)
    }
    fun setSelectedTextFontSize(size: Float) = updateSelectedTextSticker { it.copy(fontSize = size) }

    fun transformTextSticker(id: String, pan: Offset, zoom: Float, rotation: Float) {
        _editor.update { editor ->
            editor.copy(
                textStickers = editor.textStickers.map { sticker ->
                    if (sticker.id != id) return@map sticker
                    sticker.copy(
                        center = sticker.center + pan,
                        scale = (sticker.scale * zoom).coerceIn(0.25f, 5f),
                        rotation = sticker.rotation + Math.toDegrees(rotation.toDouble()).toFloat(),
                    )
                },
                selectedTextStickerId = id,
            )
        }
    }

    private fun updateSelectedTextSticker(block: (TextSticker) -> TextSticker) {
        val selectedId = _editor.value.selectedTextStickerId ?: return
        _editor.update { editor ->
            editor.copy(
                textStickers = editor.textStickers.map { sticker ->
                    if (sticker.id == selectedId) block(sticker) else sticker
                },
            )
        }
    }

    fun cropToAspect(aspectWidth: Int, aspectHeight: Int) {
        val proc = processedBitmap ?: return
        val orig = originalBitmap ?: return
        val current = _state.value as? RembgUiState.Success ?: return
        val ed = _editor.value

        viewModelScope.launch {
            val cropped = withContext(Dispatchers.Default) {
                val bounds = centerCropBounds(proc.width, proc.height, aspectWidth, aspectHeight)
                val croppedProcessed = proc.crop(bounds)
                val croppedOriginal = orig.crop(bounds)
                val croppedPaths = ed.paths.translatedAfterCrop(bounds)
                val croppedRedoStack = ed.redoStack.translatedAfterCrop(bounds)
                val croppedTextStickers = ed.textStickers.map { sticker ->
                    sticker.copy(
                        center = Offset(
                            x = sticker.center.x - bounds.left,
                            y = sticker.center.y - bounds.top,
                        ),
                    )
                }
                val working = buildWorkingBitmap(croppedProcessed, croppedOriginal, croppedPaths)
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
                CroppedEditorState(
                    original = croppedOriginal,
                    processed = croppedProcessed,
                    working = working,
                    display = display,
                    effected = effected,
                    paths = croppedPaths,
                    redoStack = croppedRedoStack,
                    textStickers = croppedTextStickers,
                )
            }
            if (_state.value !== current) return@launch
            processedBitmap = cropped.processed
            originalBitmap = cropped.original
            _editor.update {
                it.copy(
                    paths = cropped.paths,
                    redoStack = cropped.redoStack,
                    textStickers = cropped.textStickers,
                )
            }
            _state.value = RembgUiState.Success(
                originalBitmap = cropped.original,
                workingBitmap = cropped.working,
                displayBitmap = cropped.display,
                effectedBitmap = cropped.effected,
            )
        }
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
        currentSourceUri = null
        _state.value = RembgUiState.Idle
        _editor.value = EditorState()
    }
}

private fun EditorState.toSnapshot(): EditorStateSnapshot = EditorStateSnapshot(
    selectedColorArgb = selectedColor.toArgb().toLong() and 0xFFFFFFFFL,
    sourceMimeType = sourceMimeType,
    isEraseMode = isEraseMode,
    brushSize = brushSize,
    featherRadius = featherRadius,
    backgroundBlur = backgroundBlur,
    outlineWidth = outlineWidth,
    outlineColorArgb = outlineColor.toArgb().toLong() and 0xFFFFFFFFL,
    shadowRadius = shadowRadius,
    brightness = brightness,
    contrast = contrast,
    saturation = saturation,
    textStickers = textStickers.map { it.toSnapshot() },
    selectedTextStickerId = selectedTextStickerId,
)

private fun EditorStateSnapshot.toEditorState(paths: List<TouchPath>): EditorState = EditorState(
    selectedColor = Color(selectedColorArgb.toInt()),
    activeTool = EditorTool.BACKGROUND,
    sourceMimeType = sourceMimeType,
    isEraseMode = isEraseMode,
    brushSize = brushSize,
    paths = paths,
    redoStack = emptyList(),
    featherRadius = featherRadius,
    backgroundBitmap = null,
    backgroundBlur = backgroundBlur,
    blurredBackgroundBitmap = null,
    outlineWidth = outlineWidth,
    outlineColor = Color(outlineColorArgb.toInt()),
    shadowRadius = shadowRadius,
    brightness = brightness,
    contrast = contrast,
    saturation = saturation,
    textStickers = textStickers.orEmpty().map { it.toTextSticker() },
    selectedTextStickerId = selectedTextStickerId,
)

private fun TextSticker.toSnapshot(): TextStickerSnapshot = TextStickerSnapshot(
    id = id,
    text = text,
    centerX = center.x,
    centerY = center.y,
    textColorArgb = textColor.toArgb().toLong() and 0xFFFFFFFFL,
    outlineColorArgb = outlineColor.toArgb().toLong() and 0xFFFFFFFFL,
    outlineWidth = outlineWidth,
    shadowRadius = shadowRadius,
    fontSize = fontSize,
    rotation = rotation,
    scale = scale,
    font = font.name,
)

private fun TextStickerSnapshot.toTextSticker(): TextSticker = TextSticker(
    id = id,
    text = text,
    center = Offset(centerX, centerY),
    textColor = Color(textColorArgb.toInt()),
    outlineColor = Color(outlineColorArgb.toInt()),
    outlineWidth = outlineWidth,
    shadowRadius = shadowRadius,
    fontSize = fontSize,
    rotation = rotation,
    scale = scale,
    font = runCatching { TextStickerFont.valueOf(font) }.getOrDefault(TextStickerFont.SANS),
)

private data class CroppedEditorState(
    val original: Bitmap,
    val processed: Bitmap,
    val working: Bitmap,
    val display: Bitmap,
    val effected: Bitmap,
    val paths: List<TouchPath>,
    val redoStack: List<TouchPath>,
    val textStickers: List<TextSticker>,
)
