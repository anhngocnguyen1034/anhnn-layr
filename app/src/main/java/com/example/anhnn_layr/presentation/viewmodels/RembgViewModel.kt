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
import com.example.anhnn_layr.utils.BrushMode
import com.example.anhnn_layr.utils.ColorPreset
import com.example.anhnn_layr.utils.GalleryPhoto
import com.example.anhnn_layr.utils.TouchPath
import com.example.anhnn_layr.utils.TextSticker
import com.example.anhnn_layr.utils.TextStickerFont
import com.example.anhnn_layr.utils.applyEyeEnlarge
import com.example.anhnn_layr.utils.applyFaceSlim
import com.example.anhnn_layr.utils.applySkinBrighten
import com.example.anhnn_layr.utils.applySkinSmoothing
import com.example.anhnn_layr.utils.applyBlush
import com.example.anhnn_layr.utils.applyFeather
import com.example.anhnn_layr.utils.applyLipstick
import com.example.anhnn_layr.utils.applyTeethWhiten
import com.example.anhnn_layr.utils.BLUSH_PINK
import com.example.anhnn_layr.utils.CropFrame
import com.example.anhnn_layr.utils.FaceLandmarks
import com.example.anhnn_layr.utils.LIP_PINK
import com.example.anhnn_layr.utils.detectFaceLandmarks
import com.example.anhnn_layr.utils.blurBackground
import com.example.anhnn_layr.utils.buildWorkingBitmap
import com.example.anhnn_layr.utils.cropRotated
import com.example.anhnn_layr.utils.decodeUprightBitmap
import com.example.anhnn_layr.utils.mapPoint
import com.example.anhnn_layr.utils.queryCapturedPhotos
import com.example.anhnn_layr.utils.transformedByCropFrame
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

enum class EditorTool { BACKGROUND, ERASE, FACE, EFFECTS, CROP, TEXT }

// Preset "Làm đẹp tự động" — mức nhẹ tự nhiên (như chế độ auto của các app beauty),
// người dùng tinh chỉnh tiếp bằng slider từng mục nếu muốn.
private const val AUTO_EYE_ENLARGE = 0.15f
private const val AUTO_LIP_COLOR = 0.25f
private const val AUTO_TEETH_WHITEN = 0.3f
private const val AUTO_BLUSH = 0.2f
private const val AUTO_FACE_SLIM = 0.2f
private const val AUTO_SKIN_SMOOTH = 0.4f
private const val AUTO_SKIN_BRIGHTEN = 0.15f

data class EditorState(
    val selectedColor: Color = Color.Transparent,
    val activeTool: EditorTool = EditorTool.BACKGROUND,
    val sourceMimeType: String? = null,
    // true khi ảnh đã thực sự tách nền (luồng "Xoá nền"). Với ảnh thường (luồng
    // "Chỉnh ảnh") toàn ảnh là khối đục, nên tab "Nền" (màu nền/ảnh nền/mờ nền/mịn
    // viền) bị ẩn vì không có tác dụng.
    val isBackgroundRemoved: Boolean = false,
    val brushMode: BrushMode = BrushMode.ERASE,
    val brushColor: Color = Color(0xFFE53935),
    val brushSize: Float = 40f,
    val paths: List<TouchPath> = emptyList(),
    val redoStack: List<TouchPath> = emptyList(),
    val featherRadius: Float = 0f,
    val backgroundBitmap: Bitmap? = null,
    val backgroundBlur: Float = 0f,
    val blurredBackgroundBitmap: Bitmap? = null,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    // Bộ lọc màu preset (tab FX) — áp bằng ColorFilter như brightness/contrast/saturation.
    val colorPreset: ColorPreset = ColorPreset.NONE,
    // Chỉnh mặt: cường độ phóng to mắt (0..1). null = chưa dò mặt; true/false = có/không
    // tìm thấy khuôn mặt (để UI báo trạng thái).
    val eyeEnlarge: Float = 0f,
    // Cường độ tô son môi (0..1).
    val lipColor: Float = 0f,
    // Màu son đang chọn (từ bảng LIP_PALETTE).
    val lipShade: Color = Color(LIP_PINK),
    // Cường độ làm trắng răng (0..1).
    val teethWhiten: Float = 0f,
    // Cường độ đánh má hồng (0..1).
    val blush: Float = 0f,
    // Cường độ bóp thon gọn mặt / V-line (0..1).
    val faceSlim: Float = 0f,
    // Cường độ làm mịn da (0..1).
    val skinSmooth: Float = 0f,
    // Cường độ sáng da / trắng da (0..1).
    val skinBrighten: Float = 0f,
    val faceDetected: Boolean? = null,
    val textStickers: List<TextSticker> = emptyList(),
    val selectedTextStickerId: String? = null,
    // true khi đang gõ sửa nội dung chữ ngay trên ảnh (bàn phím đang mở).
    val isEditingText: Boolean = false,
    // Cắt ảnh: tỉ lệ khung đang chọn (null = tự do) và khung cắt tương tác hiện tại.
    val cropAspect: Float? = null,
    val cropFrame: CropFrame? = null,
) {
    /** true khi các giá trị chỉnh mặt đang đúng bằng preset auto (nút "Tự động" sáng). */
    val isAutoBeauty: Boolean
        get() = eyeEnlarge == AUTO_EYE_ENLARGE &&
            lipColor == AUTO_LIP_COLOR &&
            teethWhiten == AUTO_TEETH_WHITEN &&
            blush == AUTO_BLUSH &&
            faceSlim == AUTO_FACE_SLIM &&
            skinSmooth == AUTO_SKIN_SMOOTH &&
            skinBrighten == AUTO_SKIN_BRIGHTEN

    /** true khi có ít nhất một hiệu ứng chỉnh mặt đang bật (cần landmarks để render). */
    val hasFaceAdjustments: Boolean
        get() = eyeEnlarge > 0f || lipColor > 0f || teethWhiten > 0f || blush > 0f ||
            faceSlim > 0f || skinSmooth > 0f || skinBrighten > 0f
}

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

    // 5 ảnh chụp gần nhất (hiển thị ở mục "ẢNH GẦN ĐÂY" màn Home)
    private val _recentPhotos = MutableStateFlow<List<GalleryPhoto>>(emptyList())
    val recentPhotos: StateFlow<List<GalleryPhoto>> = _recentPhotos.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var processedBitmap: Bitmap? = null
    private var originalBitmap: Bitmap? = null
    private var currentSourceUri: Uri? = null
    // Điểm mỏ neo khuôn mặt (dò lười khi mở tab "Mặt"). Toạ độ theo workingBitmap.
    private var faceLandmarks: FaceLandmarks? = null
    private var faceDetectJob: kotlinx.coroutines.Job? = null

    private val subjectTrigger = Channel<Boolean>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            subjectTrigger.consumeAsFlow().collect { rebuildWorking ->
                runSubjectRecompose(rebuildWorking)
            }
        }
    }

    fun remove(
        uri: Uri,
        model: String = "isnet-general-use",
        sourceMimeType: String? = null,
    ) {
        _state.value = RembgUiState.Loading(sourceUri = uri)
        _editor.value = EditorState(sourceMimeType = sourceMimeType, isBackgroundRemoved = true)
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

    /**
     * Mở thẳng trình chỉnh sửa với ảnh gốc, KHÔNG qua xóa nền/làm nét. Toàn bộ ảnh
     * được coi là "subject" (đục hoàn toàn); người dùng có thể tự erase để cắt nền,
     * crop, thêm chữ, chỉnh sáng/tương phản… như luồng sau khi xóa nền.
     */
    fun edit(uri: Uri, sourceMimeType: String? = null) {
        _state.value = RembgUiState.Loading(sourceUri = uri)
        // Ảnh thường: chưa tách nền → ẩn tab "Nền" và "Cọ", mở thẳng tab "FX".
        _editor.value = EditorState(
            sourceMimeType = sourceMimeType,
            isBackgroundRemoved = false,
            activeTool = EditorTool.EFFECTS,
        )
        currentSourceUri = uri
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                decodeUprightBitmap(appContext, uri)
            }?.copy(Bitmap.Config.ARGB_8888, true)
            if (bitmap == null) {
                _state.value = RembgUiState.Error("Không đọc được ảnh")
                return@launch
            }
            processedBitmap = bitmap
            originalBitmap = bitmap
            _state.value = RembgUiState.Success(
                originalBitmap = bitmap,
                workingBitmap = bitmap,
                displayBitmap = bitmap,
                effectedBitmap = bitmap,
            )
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
            faceLandmarks = null
            faceDetectJob?.cancel()
            val working = snapshot.processedBitmap.copy(Bitmap.Config.ARGB_8888, true)
            _state.value = RembgUiState.Success(
                originalBitmap = original,
                workingBitmap = working,
                displayBitmap = working,
                effectedBitmap = working,
            )
            _editor.value = snapshot.editorState.toEditorState(paths = snapshot.touchPaths)
            subjectTrigger.trySend(true)
            // Bản nháp có chỉnh mặt: dò lại landmarks ngay (không đợi mở tab "Mặt") —
            // dò xong ensureFaceDetection sẽ tự recompose để render lại hiệu ứng.
            if (_editor.value.hasFaceAdjustments) ensureFaceDetection()
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
            _editor.update { it.copy(activeTool = tool, isEditingText = false) }
            if (tool == EditorTool.CROP) ensureCropFrame()
            if (tool == EditorTool.FACE) ensureFaceDetection()
        }
    }

    /** Dò khuôn mặt 1 lần khi mở tab "Mặt" (lười). Toạ độ theo workingBitmap hiện tại. */
    private fun ensureFaceDetection() {
        if (faceLandmarks != null || faceDetectJob?.isActive == true) return
        val current = _state.value as? RembgUiState.Success ?: return
        val bitmap = current.workingBitmap
        faceDetectJob = viewModelScope.launch {
            val found = withContext(Dispatchers.Default) {
                runCatching { detectFaceLandmarks(bitmap) }.getOrNull()
            }
            faceLandmarks = found
            _editor.update { it.copy(faceDetected = found != null) }
            // Đang có hiệu ứng mặt chờ landmarks (vd mở bản nháp) → render lại ngay.
            if (found != null && _editor.value.hasFaceAdjustments) {
                recomposeSubject(rebuildWorking = false)
            }
        }
    }

    fun setEyeEnlarge(value: Float) {
        _editor.update { it.copy(eyeEnlarge = value) }
        recomposeSubject(rebuildWorking = false)
    }

    fun setLipColor(value: Float) {
        _editor.update { it.copy(lipColor = value) }
        recomposeSubject(rebuildWorking = false)
    }

    fun setTeethWhiten(value: Float) {
        _editor.update { it.copy(teethWhiten = value) }
        recomposeSubject(rebuildWorking = false)
    }

    fun setBlush(value: Float) {
        _editor.update { it.copy(blush = value) }
        recomposeSubject(rebuildWorking = false)
    }

    /** Chọn màu son. Nếu cường độ đang 0 thì nâng lên mức vừa để thấy ngay hiệu ứng. */
    fun setLipShade(color: Color) {
        _editor.update {
            it.copy(
                lipShade = color,
                lipColor = if (it.lipColor > 0f) it.lipColor else 0.5f,
            )
        }
        recomposeSubject(rebuildWorking = false)
    }

    fun setFaceSlim(value: Float) {
        _editor.update { it.copy(faceSlim = value) }
        recomposeSubject(rebuildWorking = false)
    }

    fun setSkinSmooth(value: Float) {
        _editor.update { it.copy(skinSmooth = value) }
        recomposeSubject(rebuildWorking = false)
    }

    fun setSkinBrighten(value: Float) {
        _editor.update { it.copy(skinBrighten = value) }
        recomposeSubject(rebuildWorking = false)
    }

    /**
     * Bật/tắt "Làm đẹp tự động": đặt cả 6 giá trị chỉnh mặt về preset trong MỘT update
     * (1 lần recompose); bấm lần nữa khi đang đúng preset → trả tất cả về 0.
     */
    fun toggleAutoBeauty() {
        _editor.update {
            if (it.isAutoBeauty) it.copy(
                eyeEnlarge = 0f,
                lipColor = 0f,
                teethWhiten = 0f,
                blush = 0f,
                faceSlim = 0f,
                skinSmooth = 0f,
                skinBrighten = 0f,
            ) else it.copy(
                eyeEnlarge = AUTO_EYE_ENLARGE,
                lipColor = AUTO_LIP_COLOR,
                teethWhiten = AUTO_TEETH_WHITEN,
                blush = AUTO_BLUSH,
                faceSlim = AUTO_FACE_SLIM,
                skinSmooth = AUTO_SKIN_SMOOTH,
                skinBrighten = AUTO_SKIN_BRIGHTEN,
            )
        }
        recomposeSubject(rebuildWorking = false)
    }
    fun setBrushMode(mode: BrushMode) = _editor.update { it.copy(brushMode = mode) }
    fun setBrushColor(color: Color) = _editor.update { it.copy(brushColor = color) }
    fun setBrushSize(size: Float) = _editor.update { it.copy(brushSize = size) }

    fun setFeatherRadius(radius: Float) {
        _editor.update { it.copy(featherRadius = radius) }
        recomposeSubject(rebuildWorking = false)
    }

    // Màu (brightness/contrast/saturation) áp bằng ColorFilter ở preview/khi xuất,
    // KHÔNG nướng lại bitmap — chỉ cập nhật state để preview đổi màu tức thời.
    fun setBrightness(value: Float) = _editor.update { it.copy(brightness = value) }

    fun setContrast(value: Float) = _editor.update { it.copy(contrast = value) }

    fun setSaturation(value: Float) = _editor.update { it.copy(saturation = value) }

    fun setColorPreset(preset: ColorPreset) = _editor.update { it.copy(colorPreset = preset) }

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
        // Chữ mới bắt đầu rỗng + mở luôn chế độ gõ trên ảnh để người dùng nhập ngay.
        val sticker = createDefaultTextSticker(success.effectedBitmap, text = "")
        _editor.update {
            it.copy(
                activeTool = EditorTool.TEXT,
                textStickers = it.textStickers + sticker,
                selectedTextStickerId = sticker.id,
                isEditingText = true,
            )
        }
    }

    /** Bắt đầu sửa nội dung một chữ ngay trên ảnh (chạm vào chữ). */
    fun beginTextEdit(id: String) {
        _editor.update {
            if (it.textStickers.none { s -> s.id == id }) it
            else it.copy(selectedTextStickerId = id, isEditingText = true)
        }
    }

    /** Kết thúc gõ: nếu chữ để trống thì xoá luôn sticker (chữ mới chưa nhập gì). */
    fun endTextEdit() {
        _editor.update { editor ->
            val sel = editor.textStickers.firstOrNull { it.id == editor.selectedTextStickerId }
            if (sel != null && sel.text.isBlank()) {
                val remaining = editor.textStickers.filterNot { it.id == sel.id }
                editor.copy(
                    textStickers = remaining,
                    selectedTextStickerId = remaining.lastOrNull()?.id,
                    isEditingText = false,
                )
            } else {
                editor.copy(isEditingText = false)
            }
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

    private fun createDefaultTextSticker(bitmap: Bitmap, text: String = "Chữ mới"): TextSticker = TextSticker(
        id = UUID.randomUUID().toString(),
        text = text,
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
                isEditingText = false,
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

    /** [rotationDeg] tính bằng độ (tay nắm xoay truyền delta theo độ). */
    fun transformTextSticker(id: String, pan: Offset, zoom: Float, rotationDeg: Float) {
        _editor.update { editor ->
            editor.copy(
                textStickers = editor.textStickers.map { sticker ->
                    if (sticker.id != id) return@map sticker
                    sticker.copy(
                        center = sticker.center + pan,
                        scale = (sticker.scale * zoom).coerceIn(0.25f, 5f),
                        rotation = sticker.rotation + rotationDeg,
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

    /** Khung cắt mặc định: căn giữa, lớn nhất vừa với ảnh theo [aspect] (null = tự do). */
    private fun defaultCropFrame(width: Int, height: Int, aspect: Float?): CropFrame {
        val w = width.toFloat()
        val h = height.toFloat()
        val (fw, fh) = when {
            aspect == null -> w to h
            w / h > aspect -> (h * aspect) to h
            else -> w to (w / aspect)
        }
        return CropFrame(cx = w / 2f, cy = h / 2f, width = fw, height = fh, rotationDeg = 0f)
    }

    /** Khi mở công cụ Cắt mà chưa có khung, tạo khung mặc định theo tỉ lệ đang chọn. */
    private fun ensureCropFrame() {
        if (_editor.value.cropFrame != null) return
        val current = _state.value as? RembgUiState.Success ?: return
        val bmp = current.effectedBitmap
        _editor.update { it.copy(cropFrame = defaultCropFrame(bmp.width, bmp.height, it.cropAspect)) }
    }

    /** Chọn tỉ lệ khung (null = tự do): đặt lại khung cắt căn giữa theo tỉ lệ đó. */
    fun setCropAspect(aspect: Float?) {
        val current = _state.value as? RembgUiState.Success ?: return
        val bmp = current.effectedBitmap
        _editor.update {
            it.copy(cropAspect = aspect, cropFrame = defaultCropFrame(bmp.width, bmp.height, aspect))
        }
    }

    /** Cập nhật khung cắt khi người dùng di chuyển / xoay / phóng to trên ảnh. */
    fun setCropFrame(frame: CropFrame) = _editor.update { it.copy(cropFrame = frame) }

    /** Đặt lại khung cắt về mặc định theo tỉ lệ đang chọn. */
    fun resetCrop() {
        val current = _state.value as? RembgUiState.Success ?: return
        val bmp = current.effectedBitmap
        _editor.update { it.copy(cropFrame = defaultCropFrame(bmp.width, bmp.height, it.cropAspect)) }
    }

    /** Áp dụng cắt theo [EditorState.cropFrame] hiện tại (hỗ trợ khung xoay). */
    fun applyCrop() {
        val proc = processedBitmap ?: return
        val orig = originalBitmap ?: return
        val current = _state.value as? RembgUiState.Success ?: return
        val ed = _editor.value
        val frame = ed.cropFrame ?: return

        viewModelScope.launch {
            val cropped = withContext(Dispatchers.Default) {
                val croppedProcessed = proc.cropRotated(frame)
                val croppedOriginal = orig.cropRotated(frame)
                val croppedPaths = ed.paths.transformedByCropFrame(frame)
                val croppedRedoStack = ed.redoStack.transformedByCropFrame(frame)
                val croppedTextStickers = ed.textStickers.map { sticker ->
                    sticker.copy(
                        center = frame.mapPoint(sticker.center),
                        rotation = sticker.rotation - frame.rotationDeg,
                    )
                }
                val working = buildWorkingBitmap(croppedProcessed, croppedOriginal, croppedPaths)
                val display = applyFeather(working, ed.featherRadius)
                val effected = display
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
            // Toạ độ đã đổi sau khi cắt → bỏ điểm mỏ neo cũ, mở lại tab "Mặt" sẽ dò lại.
            faceLandmarks = null
            faceDetectJob?.cancel()
            _editor.update {
                it.copy(
                    paths = cropped.paths,
                    redoStack = cropped.redoStack,
                    textStickers = cropped.textStickers,
                    cropAspect = null,
                    // Tạo lại khung mặc định cho ảnh vừa cắt để có thể cắt tiếp.
                    cropFrame = defaultCropFrame(cropped.effected.width, cropped.effected.height, null),
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

    private suspend fun runSubjectRecompose(rebuildWorking: Boolean) {
        val proc = processedBitmap ?: return
        val orig = originalBitmap ?: return
        val ed = _editor.value
        val current = _state.value as? RembgUiState.Success
        val result = withContext(Dispatchers.Default) {
            val working = if (rebuildWorking || current == null) {
                buildWorkingBitmap(proc, orig, ed.paths)
            } else current.workingBitmap
            // Warp chỉnh mặt nướng vào bitmap, LUÔN tính từ working sạch (không méo
            // tích lũy), TRƯỚC feather. Trả về working nếu cường độ 0 / chưa có mặt.
            // Mịn da TRƯỚC khi warp: mặt nạ dùng toạ độ landmark khớp với working chưa
            // biến dạng (eye/slim warp sau đó chỉ biến hình lại lớp da đã mịn).
            val smoothed = applySkinSmoothing(working, faceLandmarks?.allPoints.orEmpty(), ed.skinSmooth)
            // Sáng da sau mịn (cùng mặt nạ, toạ độ landmark chưa biến dạng), trước warp.
            val brightened = applySkinBrighten(smoothed, faceLandmarks?.allPoints.orEmpty(), ed.skinBrighten)
            // Má hồng TRƯỚC warp: vùng má bị thon mặt kéo dịch, đánh trước để lớp phấn
            // biến dạng đồng bộ với da (không lệch vị trí như nếu vẽ sau).
            val blushed = applyBlush(brightened, faceLandmarks?.allPoints.orEmpty(), BLUSH_PINK, ed.blush)
            val faced = applyEyeEnlarge(blushed, faceLandmarks, ed.eyeEnlarge)
            // Bóp thon mặt: hút viền má vào trục giữa (centerGuard giữ vùng miệng nguyên).
            val slimmed = applyFaceSlim(faced, faceLandmarks, ed.faceSlim)
            // Tô son sau warp (mắt + má warp cục bộ không làm môi dịch nên toạ độ vẫn đúng).
            // applyLipstick đục lỗ môi trong (không tô răng) + blend giữ texture môi gốc.
            val painted = applyLipstick(
                slimmed,
                faceLandmarks?.allPoints.orEmpty(),
                ed.lipShade.toArgb(),
                ed.lipColor,
            )
            // Răng trắng dùng đúng đa giác môi trong mà son đục lỗ → hai vùng không
            // chồng nhau, thứ tự son/răng không ảnh hưởng kết quả.
            val whitened = applyTeethWhiten(painted, faceLandmarks?.allPoints.orEmpty(), ed.teethWhiten)
            val display = applyFeather(whitened, ed.featherRadius)
            working to display
        }
        val now = _state.value
        if (now is RembgUiState.Success) {
            _state.value = now.copy(
                workingBitmap = result.first,
                displayBitmap = result.second,
                effectedBitmap = result.second,
            )
        }
    }

    /** Tải lại 5 ảnh chụp gần nhất cho mục "ẢNH GẦN ĐÂY". */
    fun refreshRecentPhotos() {
        viewModelScope.launch {
            val photos = withContext(Dispatchers.IO) { queryCapturedPhotos(appContext) }
            _recentPhotos.value = photos.take(5)
        }
    }

    fun reset() {
        processedBitmap = null
        originalBitmap = null
        currentSourceUri = null
        faceLandmarks = null
        faceDetectJob?.cancel()
        _state.value = RembgUiState.Idle
        _editor.value = EditorState()
    }
}

private fun EditorState.toSnapshot(): EditorStateSnapshot = EditorStateSnapshot(
    selectedColorArgb = selectedColor.toArgb().toLong() and 0xFFFFFFFFL,
    sourceMimeType = sourceMimeType,
    brushMode = brushMode.name,
    brushColorArgb = brushColor.toArgb().toLong() and 0xFFFFFFFFL,
    brushSize = brushSize,
    featherRadius = featherRadius,
    backgroundBlur = backgroundBlur,
    brightness = brightness,
    contrast = contrast,
    saturation = saturation,
    textStickers = textStickers.map { it.toSnapshot() },
    selectedTextStickerId = selectedTextStickerId,
    isBackgroundRemoved = isBackgroundRemoved,
    colorPreset = colorPreset.name,
    eyeEnlarge = eyeEnlarge,
    lipColor = lipColor,
    lipShadeArgb = lipShade.toArgb().toLong() and 0xFFFFFFFFL,
    teethWhiten = teethWhiten,
    blush = blush,
    faceSlim = faceSlim,
    skinSmooth = skinSmooth,
    skinBrighten = skinBrighten,
)

private fun EditorStateSnapshot.toEditorState(paths: List<TouchPath>): EditorState {
    // Bản nháp cũ thiếu cờ → mặc định coi như đã xoá nền (giữ nguyên hành vi cũ).
    val bgRemoved = isBackgroundRemoved ?: true
    // Bản nháp cũ chỉ có isEraseMode (true/false) → suy ra ERASE/RESTORE.
    val mode = brushMode?.let { runCatching { BrushMode.valueOf(it) }.getOrNull() }
        ?: if (isEraseMode != false) BrushMode.ERASE else BrushMode.RESTORE
    return EditorState(
    selectedColor = Color(selectedColorArgb.toInt()),
    activeTool = if (bgRemoved) EditorTool.BACKGROUND else EditorTool.EFFECTS,
    sourceMimeType = sourceMimeType,
    brushMode = mode,
    brushColor = brushColorArgb?.let { Color(it.toInt()) } ?: Color(0xFFE53935),
    brushSize = brushSize,
    paths = paths,
    redoStack = emptyList(),
    featherRadius = featherRadius,
    backgroundBitmap = null,
    backgroundBlur = backgroundBlur,
    blurredBackgroundBitmap = null,
    brightness = brightness,
    contrast = contrast,
    saturation = saturation,
    textStickers = textStickers.orEmpty().map { it.toTextSticker() },
    selectedTextStickerId = selectedTextStickerId,
    isBackgroundRemoved = bgRemoved,
    colorPreset = colorPreset
        ?.let { runCatching { ColorPreset.valueOf(it) }.getOrNull() }
        ?: ColorPreset.NONE,
    eyeEnlarge = eyeEnlarge ?: 0f,
    lipColor = lipColor ?: 0f,
    lipShade = lipShadeArgb?.let { Color(it.toInt()) } ?: Color(LIP_PINK),
    teethWhiten = teethWhiten ?: 0f,
    blush = blush ?: 0f,
    faceSlim = faceSlim ?: 0f,
    skinSmooth = skinSmooth ?: 0f,
    skinBrighten = skinBrighten ?: 0f,
    )
}

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
    font = runCatching { TextStickerFont.valueOf(font) }.getOrDefault(TextStickerFont.INTER),
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
