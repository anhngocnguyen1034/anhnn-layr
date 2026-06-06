package com.example.anhnn_layr.presentation.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.components.CropOverlay
import com.example.anhnn_layr.presentation.components.EraseCanvas
import com.example.anhnn_layr.presentation.components.TextStickerLayer
import com.example.anhnn_layr.presentation.components.checkerboardBackground
import com.example.anhnn_layr.presentation.components.tools.BackgroundToolPanel
import com.example.anhnn_layr.presentation.components.tools.CropToolPanel
import com.example.anhnn_layr.presentation.components.tools.EffectsToolPanel
import com.example.anhnn_layr.presentation.components.tools.EraseToolPanel
import com.example.anhnn_layr.presentation.components.tools.TextToolPanel
import com.example.anhnn_layr.presentation.components.tools.ToolTabs
import com.example.anhnn_layr.presentation.viewmodels.EditorState
import com.example.anhnn_layr.presentation.viewmodels.EditorTool
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleDark
import com.example.anhnn_layr.utils.CropFrame
import com.example.anhnn_layr.utils.TextStickerFont
import com.example.anhnn_layr.utils.TouchPath
import com.example.anhnn_layr.utils.colorAdjustMatrixOrNull
import com.example.anhnn_layr.utils.generateFinalBitmap
import com.example.anhnn_layr.utils.pickSaveFormat
import com.example.anhnn_layr.utils.saveBitmapToGallery

@Composable
fun EditorScreen(
    workingBitmap: Bitmap,
    displayBitmap: Bitmap,
    effectedBitmap: Bitmap,
    originalBitmap: Bitmap,
    editor: EditorState,
    onColorChange: (Color) -> Unit,
    onToolChange: (EditorTool) -> Unit,
    onEraseModeChange: (Boolean) -> Unit,
    onBrushSizeChange: (Float) -> Unit,
    onFeatherChange: (Float) -> Unit,
    onBackgroundImageSelected: (android.graphics.Bitmap?) -> Unit,
    onBackgroundBlurChange: (Float) -> Unit,
    onUseOriginalBackground: () -> Unit,
    onOutlineWidthChange: (Float) -> Unit,
    onOutlineColorChange: (Color) -> Unit,
    onShadowRadiusChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onSelectCropAspect: (Float?) -> Unit,
    onApplyCrop: () -> Unit,
    onResetCrop: () -> Unit,
    onCropFrameChange: (CropFrame) -> Unit,
    onAddText: () -> Unit,
    onSelectText: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onTextFontChange: (TextStickerFont) -> Unit,
    onTextColorChange: (Color) -> Unit,
    onTextOutlineColorChange: (Color) -> Unit,
    onTextOutlineWidthChange: (Float) -> Unit,
    onTextShadowRadiusChange: (Float) -> Unit,
    onTextFontSizeChange: (Float) -> Unit,
    onTextTransform: (String, Offset, Float, Float) -> Unit,
    onStartTextEdit: (String) -> Unit,
    onEndTextEdit: () -> Unit,
    onDeleteText: () -> Unit,
    onCommitPath: (TouchPath) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSaveDraft: () -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    var scale by remember(workingBitmap) { mutableStateOf(1f) }
    var offset by remember(workingBitmap) { mutableStateOf(Offset.Zero) }
    // Chạm vào ảnh để ẩn/hiện bảng công cụ nổi, xem trọn ảnh trực quan.
    var controlsVisible by remember { mutableStateOf(true) }
    val exportImage: () -> Unit = {
        runCatching {
            val finalBmp = generateFinalBitmap(
                context = ctx,
                subjectBitmap = effectedBitmap,
                bgColor = editor.selectedColor,
                bgBitmap = editor.blurredBackgroundBitmap,
                textStickers = editor.textStickers,
                subjectColorMatrix = colorAdjustMatrixOrNull(
                    editor.brightness,
                    editor.contrast,
                    editor.saturation,
                ),
            )
            val hasTransparency = editor.selectedColor == Color.Transparent &&
                editor.blurredBackgroundBitmap == null
            val format = pickSaveFormat(editor.sourceMimeType, hasTransparency)
            saveBitmapToGallery(ctx, finalBmp, format)
        }.onSuccess {
            Toast.makeText(
                ctx,
                "Đã lưu vào Pictures/TayMaySticker",
                Toast.LENGTH_SHORT,
            ).show()
        }.onFailure {
            Toast.makeText(
                ctx,
                "Lưu thất bại: ${it.message}",
                Toast.LENGTH_LONG,
            ).show()
        }
        Unit
    }

    Scaffold(
        containerColor = Color(0xFFF7F8FC),
        topBar = {
            EditorTopBar(
                activeTool = editor.activeTool,
                onBack = onBack,
                onSaveDraft = onSaveDraft,
                onExport = exportImage,
            )
        },
        bottomBar = {
            ToolTabs(
                active = editor.activeTool,
                onSelect = { tool ->
                    controlsVisible = true // đổi công cụ thì hiện lại bảng
                    onToolChange(tool)
                },
            )
        },
    ) { inner ->
        // Ảnh chiếm trọn vùng giữa thanh trên và thanh tab; bảng công cụ nổi đè
        // lên đáy ảnh (translucent) để không co nhỏ ảnh khi chỉnh sửa.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentAlignment = Alignment.Center,
        ) {
            EditorPreview(
                displayBitmap = displayBitmap,
                effectedBitmap = effectedBitmap,
                originalBitmap = originalBitmap,
                editor = editor,
                scale = scale,
                offset = offset,
                onTransform = { s, o -> scale = s; offset = o },
                onCommitPath = onCommitPath,
                onTextTransform = onTextTransform,
                onTextChange = onTextChange,
                onStartTextEdit = onStartTextEdit,
                onEndTextEdit = onEndTextEdit,
                onCropFrameChange = onCropFrameChange,
                onToggleControls = { controlsVisible = !controlsVisible },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = controlsVisible,
                enter = slideInVertically(tween(220)) { it } + fadeIn(tween(180)),
                exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
            FloatingToolPanel(
                editor = editor,
                onColorChange = onColorChange,
                onFeatherChange = onFeatherChange,
                onBackgroundImageSelected = onBackgroundImageSelected,
                onBackgroundBlurChange = onBackgroundBlurChange,
                onUseOriginalBackground = onUseOriginalBackground,
                onOutlineWidthChange = onOutlineWidthChange,
                onOutlineColorChange = onOutlineColorChange,
                onShadowRadiusChange = onShadowRadiusChange,
                onBrightnessChange = onBrightnessChange,
                onContrastChange = onContrastChange,
                onSaturationChange = onSaturationChange,
                onEraseModeChange = onEraseModeChange,
                onBrushSizeChange = onBrushSizeChange,
                onUndo = onUndo,
                onRedo = onRedo,
                onSelectCropAspect = onSelectCropAspect,
                onApplyCrop = onApplyCrop,
                onResetCrop = onResetCrop,
                onAddText = onAddText,
                onSelectText = onSelectText,
                onTextFontChange = onTextFontChange,
                onTextColorChange = onTextColorChange,
                onTextOutlineColorChange = onTextOutlineColorChange,
                onTextOutlineWidthChange = onTextOutlineWidthChange,
                onTextShadowRadiusChange = onTextShadowRadiusChange,
                onTextFontSizeChange = onTextFontSizeChange,
                onDeleteText = onDeleteText,
            )
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    activeTool: EditorTool,
    onBack: () -> Unit,
    onSaveDraft: () -> Unit,
    onExport: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarIconButton(
                onClick = onBack,
                contentDescription = "Quay lại",
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Chỉnh sửa",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = activeTool.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ToolbarIconButton(
                onClick = onSaveDraft,
                contentDescription = "Lưu nháp",
            ) {
                Icon(Icons.Outlined.Bookmark, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(6.dp))
            ToolbarIconButton(
                onClick = onExport,
                contentDescription = "Lưu ảnh",
                emphasized = true,
            ) {
                Icon(Icons.Outlined.SaveAlt, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    emphasized: Boolean = false,
    content: @Composable () -> Unit,
) {
    val background = if (emphasized) AnhnnPurpleDark else Color.Transparent
    val contentColor = if (emphasized) Color.White else MaterialTheme.colorScheme.onSurface
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .semantics { this.contentDescription = contentDescription },
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

@Composable
private fun EditorPreview(
    displayBitmap: Bitmap,
    effectedBitmap: Bitmap,
    originalBitmap: Bitmap,
    editor: EditorState,
    scale: Float,
    offset: Offset,
    onTransform: (Float, Offset) -> Unit,
    onCommitPath: (TouchPath) -> Unit,
    onTextTransform: (String, Offset, Float, Float) -> Unit,
    onTextChange: (String) -> Unit,
    onStartTextEdit: (String) -> Unit,
    onEndTextEdit: () -> Unit,
    onCropFrameChange: (CropFrame) -> Unit,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageRatio = displayBitmap.width.toFloat() / displayBitmap.height.toFloat()

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val containerRatio = maxWidth.value / maxHeight.value.coerceAtLeast(1f)
        val previewSizeModifier = if (imageRatio > containerRatio) {
            Modifier.fillMaxWidth().aspectRatio(imageRatio)
        } else {
            Modifier.fillMaxHeight().aspectRatio(imageRatio)
        }

        PreviewCanvas(
            displayBitmap = displayBitmap,
            effectedBitmap = effectedBitmap,
            originalBitmap = originalBitmap,
            editor = editor,
            scale = scale,
            offset = offset,
            onTransform = onTransform,
            onCommitPath = onCommitPath,
            onTextTransform = onTextTransform,
            onTextChange = onTextChange,
            onStartTextEdit = onStartTextEdit,
            onEndTextEdit = onEndTextEdit,
            onCropFrameChange = onCropFrameChange,
            onToggleControls = onToggleControls,
            modifier = previewSizeModifier,
        )
    }
}

@Composable
private fun PreviewCanvas(
    displayBitmap: Bitmap,
    effectedBitmap: Bitmap,
    originalBitmap: Bitmap,
    editor: EditorState,
    scale: Float,
    offset: Offset,
    onTransform: (Float, Offset) -> Unit,
    onCommitPath: (TouchPath) -> Unit,
    onTextTransform: (String, Offset, Float, Float) -> Unit,
    onTextChange: (String) -> Unit,
    onStartTextEdit: (String) -> Unit,
    onEndTextEdit: () -> Unit,
    onCropFrameChange: (CropFrame) -> Unit,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasBg = editor.blurredBackgroundBitmap != null
    val shape = RoundedCornerShape(8.dp)
    val baseMod = when {
        hasBg -> Modifier
        editor.selectedColor == Color.Transparent -> Modifier.checkerboardBackground(
            cellSize = 14.dp,
            light = Color(0xFFFFFFFF),
            dark = Color(0xFFE1E3EC),
        )
        else -> Modifier.background(editor.selectedColor)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(baseMod)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            // Chạm vào ảnh (chỗ công cụ không xử lý) để ẩn/hiện bảng công cụ.
            // Là cha của các lớp công cụ nên chỉ nhận tap khi chúng không tiêu thụ.
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggleControls() })
            },
        contentAlignment = Alignment.Center,
    ) {
        editor.blurredBackgroundBitmap?.let { bg ->
            Image(
                bitmap = bg.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        if (hasBg && editor.selectedColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(editor.selectedColor),
            )
        }
        if (editor.activeTool == EditorTool.ERASE) {
            EraseCanvas(
                workingBitmap = displayBitmap,
                originalBitmap = originalBitmap,
                isEraseMode = editor.isEraseMode,
                brushSize = editor.brushSize,
                scale = scale,
                offset = offset,
                onTransform = onTransform,
                onCommitPath = onCommitPath,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Màu áp bằng ColorFilter (GPU) để đổi tức thời khi kéo slider, không
            // phải nướng lại bitmap.
            val colorFilter = remember(editor.brightness, editor.contrast, editor.saturation) {
                colorAdjustMatrixOrNull(editor.brightness, editor.contrast, editor.saturation)
                    ?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
            }
            Image(
                bitmap = effectedBitmap.asImageBitmap(),
                contentDescription = "Ảnh đã xoá nền",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                        transformOrigin = TransformOrigin(0f, 0f),
                    ),
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
            )
        }
        TextStickerLayer(
            stickers = editor.textStickers,
            selectedId = editor.selectedTextStickerId,
            bitmapWidth = effectedBitmap.width,
            bitmapHeight = effectedBitmap.height,
            editable = editor.activeTool == EditorTool.TEXT,
            onTransform = onTextTransform,
            onStartEdit = onStartTextEdit,
            onTapEmpty = onEndTextEdit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                    transformOrigin = TransformOrigin(0f, 0f),
                ),
        )
        if (editor.activeTool == EditorTool.TEXT && editor.isEditingText) {
            val editing = editor.textStickers.firstOrNull { it.id == editor.selectedTextStickerId }
            if (editing != null) {
                InlineTextEditor(
                    stickerId = editing.id,
                    initialText = editing.text,
                    onTextChange = onTextChange,
                    onDone = onEndTextEdit,
                )
            }
        }
        if (editor.activeTool == EditorTool.CROP) {
            editor.cropFrame?.let { frame ->
                CropOverlay(
                    frame = frame,
                    bitmapWidth = effectedBitmap.width,
                    bitmapHeight = effectedBitmap.height,
                    onFrameChange = onCropFrameChange,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * Trình sửa chữ inline: một [BasicTextField] ẩn (kích thước 1dp, trong suốt) chỉ để
 * giữ con trỏ và mở bàn phím. Nội dung gõ được phản chiếu sang sticker qua
 * [onTextChange] nên chữ hiển thị trực tiếp trên ảnh khi gõ. Mất focus (đóng bàn
 * phím / chạm ra ngoài) sẽ gọi [onDone].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun InlineTextEditor(
    stickerId: String,
    initialText: String,
    onTextChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var value by remember(stickerId) {
        mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length)))
    }
    var hadFocus by remember(stickerId) { mutableStateOf(false) }

    BasicTextField(
        value = value,
        onValueChange = {
            value = it
            onTextChange(it.text)
        },
        cursorBrush = SolidColor(Color.Transparent),
        modifier = Modifier
            .size(1.dp)
            .alpha(0f)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    hadFocus = true
                } else if (hadFocus) {
                    onDone()
                }
            },
    )

    LaunchedEffect(stickerId) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
}

@Composable
private fun FloatingToolPanel(
    editor: EditorState,
    onColorChange: (Color) -> Unit,
    onFeatherChange: (Float) -> Unit,
    onBackgroundImageSelected: (android.graphics.Bitmap?) -> Unit,
    onBackgroundBlurChange: (Float) -> Unit,
    onUseOriginalBackground: () -> Unit,
    onOutlineWidthChange: (Float) -> Unit,
    onOutlineColorChange: (Color) -> Unit,
    onShadowRadiusChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onEraseModeChange: (Boolean) -> Unit,
    onBrushSizeChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSelectCropAspect: (Float?) -> Unit,
    onApplyCrop: () -> Unit,
    onResetCrop: () -> Unit,
    onAddText: () -> Unit,
    onSelectText: (String) -> Unit,
    onTextFontChange: (TextStickerFont) -> Unit,
    onTextColorChange: (Color) -> Unit,
    onTextOutlineColorChange: (Color) -> Unit,
    onTextOutlineWidthChange: (Float) -> Unit,
    onTextShadowRadiusChange: (Float) -> Unit,
    onTextFontSizeChange: (Float) -> Unit,
    onDeleteText: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = editor.activeTool,
        transitionSpec = {
            (fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 4 }) togetherWith
                (fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 4 })
        },
        label = "tool-panel",
        modifier = modifier.fillMaxWidth(),
    ) { tool ->
        Surface(
            // Thẻ nổi mờ đè lên đáy ảnh: chặn chạm xuyên qua để không vẽ nhầm lên
            // canvas phía dưới; bo góc + đổ bóng cho cảm giác "nổi".
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .padding(bottom = 10.dp)
                .blockPointerThrough(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 3.dp,
            shadowElevation = 10.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (tool) {
                    EditorTool.BACKGROUND -> BackgroundToolPanel(
                        selected = editor.selectedColor,
                        onSelected = onColorChange,
                        featherRadius = editor.featherRadius,
                        onFeatherChange = onFeatherChange,
                        hasBackgroundImage = editor.backgroundBitmap != null,
                        backgroundBlur = editor.backgroundBlur,
                        onBackgroundImageSelected = onBackgroundImageSelected,
                        onBackgroundBlurChange = onBackgroundBlurChange,
                        onUseOriginalBackground = onUseOriginalBackground,
                    )
                    EditorTool.EFFECTS -> EffectsToolPanel(
                        outlineWidth = editor.outlineWidth,
                        outlineColor = editor.outlineColor,
                        onOutlineWidthChange = onOutlineWidthChange,
                        onOutlineColorChange = onOutlineColorChange,
                        shadowRadius = editor.shadowRadius,
                        onShadowRadiusChange = onShadowRadiusChange,
                        brightness = editor.brightness,
                        contrast = editor.contrast,
                        saturation = editor.saturation,
                        onBrightnessChange = onBrightnessChange,
                        onContrastChange = onContrastChange,
                        onSaturationChange = onSaturationChange,
                    )
                    EditorTool.ERASE -> EraseToolPanel(
                        isEraseMode = editor.isEraseMode,
                        brushSize = editor.brushSize,
                        onModeChange = onEraseModeChange,
                        onBrushSizeChange = onBrushSizeChange,
                        onUndo = onUndo,
                        onRedo = onRedo,
                        canUndo = editor.paths.isNotEmpty(),
                        canRedo = editor.redoStack.isNotEmpty(),
                    )
                    EditorTool.CROP -> CropToolPanel(
                        selectedAspect = editor.cropAspect,
                        onSelectAspect = onSelectCropAspect,
                        onApply = onApplyCrop,
                        onReset = onResetCrop,
                    )
                    EditorTool.TEXT -> TextToolPanel(
                        stickers = editor.textStickers,
                        selectedId = editor.selectedTextStickerId,
                        onAdd = onAddText,
                        onSelect = onSelectText,
                        onFontChange = onTextFontChange,
                        onTextColorChange = onTextColorChange,
                        onOutlineColorChange = onTextOutlineColorChange,
                        onOutlineWidthChange = onTextOutlineWidthChange,
                        onShadowRadiusChange = onTextShadowRadiusChange,
                        onFontSizeChange = onTextFontSizeChange,
                        onDelete = onDeleteText,
                    )
                }
            }
        }
    }
}

/**
 * Chặn chạm rơi xuống canvas ảnh phía dưới: chỉ cần thẻ là một node pointer-input
 * phủ hết vùng thì nó đã che (occlude) sibling bên dưới — KHÔNG được consume sự
 * kiện, nếu không slider/nút con trong thẻ sẽ bị huỷ cử chỉ và "chết".
 */
private fun Modifier.blockPointerThrough(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent()
        }
    }
}

private val EditorTool.label: String
    get() = when (this) {
        EditorTool.BACKGROUND -> "Nền"
        EditorTool.ERASE -> "Cọ"
        EditorTool.EFFECTS -> "Hiệu ứng"
        EditorTool.CROP -> "Cắt ảnh"
        EditorTool.TEXT -> "Chữ"
    }
