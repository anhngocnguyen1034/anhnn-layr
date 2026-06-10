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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Compare
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
import com.example.anhnn_layr.presentation.components.tools.BackgroundToolPanel
import com.example.anhnn_layr.presentation.components.tools.CropToolPanel
import com.example.anhnn_layr.presentation.components.tools.EffectsToolPanel
import com.example.anhnn_layr.presentation.components.tools.EraseToolPanel
import com.example.anhnn_layr.presentation.components.tools.FaceToolPanel
import com.example.anhnn_layr.presentation.components.tools.TextToolPanel
import com.example.anhnn_layr.presentation.components.tools.ToolTabs
import com.example.anhnn_layr.presentation.viewmodels.EditorState
import com.example.anhnn_layr.presentation.viewmodels.EditorTool
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleDark
import com.example.anhnn_layr.utils.BrushMode
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
    onBrushModeChange: (BrushMode) -> Unit,
    onBrushColorChange: (Color) -> Unit,
    onBrushSizeChange: (Float) -> Unit,
    onFeatherChange: (Float) -> Unit,
    onBackgroundImageSelected: (android.graphics.Bitmap?) -> Unit,
    onBackgroundBlurChange: (Float) -> Unit,
    onUseOriginalBackground: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onAutoBeautyToggle: () -> Unit,
    onEyeEnlargeChange: (Float) -> Unit,
    onLipColorChange: (Float) -> Unit,
    onLipShadeChange: (Color) -> Unit,
    onTeethWhitenChange: (Float) -> Unit,
    onBlushChange: (Float) -> Unit,
    onFaceSlimChange: (Float) -> Unit,
    onSkinSmoothChange: (Float) -> Unit,
    onSkinBrightenChange: (Float) -> Unit,
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
                onSelect = onToolChange,
                showBackground = editor.isBackgroundRemoved,
            )
        },
    ) { inner ->
        // Ảnh ở vùng trên, bảng công cụ là vùng RIÊNG bên dưới (ngăn cách rõ, không
        // đè lên ảnh) với chiều cao CỐ ĐỊNH nên ảnh không nhảy khi đổi tab.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
            )
            FloatingToolPanel(
                editor = editor,
                onColorChange = onColorChange,
                onFeatherChange = onFeatherChange,
                onBackgroundImageSelected = onBackgroundImageSelected,
                onBackgroundBlurChange = onBackgroundBlurChange,
                onUseOriginalBackground = onUseOriginalBackground,
                onBrightnessChange = onBrightnessChange,
                onContrastChange = onContrastChange,
                onSaturationChange = onSaturationChange,
                onAutoBeautyToggle = onAutoBeautyToggle,
                onEyeEnlargeChange = onEyeEnlargeChange,
                onLipColorChange = onLipColorChange,
                onLipShadeChange = onLipShadeChange,
                onTeethWhitenChange = onTeethWhitenChange,
                onBlushChange = onBlushChange,
                onFaceSlimChange = onFaceSlimChange,
                onSkinSmoothChange = onSkinSmoothChange,
                onSkinBrightenChange = onSkinBrightenChange,
                onBrushModeChange = onBrushModeChange,
                onBrushColorChange = onBrushColorChange,
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
    modifier: Modifier = Modifier,
) {
    val imageRatio = displayBitmap.width.toFloat() / displayBitmap.height.toFloat()
    // Đè giữ nút so sánh → xem ảnh gốc; thả tay → quay lại ảnh đã chỉnh. State thuần
    // hiển thị (như scale/offset) nên giữ cục bộ, không cần hoist lên ViewModel.
    var showOriginal by remember { mutableStateOf(false) }

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
            showOriginal = showOriginal,
            scale = scale,
            offset = offset,
            onTransform = onTransform,
            onCommitPath = onCommitPath,
            onTextTransform = onTextTransform,
            onTextChange = onTextChange,
            onStartTextEdit = onStartTextEdit,
            onEndTextEdit = onEndTextEdit,
            onCropFrameChange = onCropFrameChange,
            modifier = previewSizeModifier,
        )

        // Nhãn báo đang xem ảnh gốc.
        if (showOriginal) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(50),
                color = Color(0x99000000),
                contentColor = Color.White,
            ) {
                Text(
                    text = "Ảnh gốc",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        CompareHoldButton(
            onPressedChange = { showOriginal = it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp),
        )
    }
}

/**
 * Nút tròn nổi "so sánh trước/sau": ĐÈ GIỮ để xem ảnh gốc, thả tay quay về ảnh đã
 * chỉnh (chuẩn thao tác của các app chỉnh sửa). Dùng [detectTapGestures.onPress] +
 * `tryAwaitRelease` thay vì clickable để bắt được trạng thái giữ.
 */
@Composable
private fun CompareHoldButton(
    onPressedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(44.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressedChange(true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            // finally: kể cả khi gesture bị huỷ cũng trả về ảnh đã chỉnh.
                            onPressedChange(false)
                        }
                    },
                )
            }
            .semantics { contentDescription = "Đè giữ để xem ảnh gốc" },
        shape = CircleShape,
        color = Color(0x99000000),
        contentColor = Color.White,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Compare,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun PreviewCanvas(
    displayBitmap: Bitmap,
    effectedBitmap: Bitmap,
    originalBitmap: Bitmap,
    editor: EditorState,
    showOriginal: Boolean,
    scale: Float,
    offset: Offset,
    onTransform: (Float, Offset) -> Unit,
    onCommitPath: (TouchPath) -> Unit,
    onTextTransform: (String, Offset, Float, Float) -> Unit,
    onTextChange: (String) -> Unit,
    onStartTextEdit: (String) -> Unit,
    onEndTextEdit: () -> Unit,
    onCropFrameChange: (CropFrame) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasBg = editor.blurredBackgroundBitmap != null
    val baseMod = when {
        hasBg -> Modifier
        // Vùng trong suốt để lộ nền đen của canvas (bỏ ô caro cho gọn mắt).
        editor.selectedColor == Color.Transparent -> Modifier
        else -> Modifier.background(editor.selectedColor)
    }

    Box(
        modifier = modifier.then(baseMod),
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
                brushMode = editor.brushMode,
                brushColor = editor.brushColor,
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
            // Bọc 1 lần / mỗi bitmap, tránh tạo wrapper mới mỗi frame khi kéo slider.
            val effectedImage = remember(effectedBitmap) { effectedBitmap.asImageBitmap() }
            Image(
                bitmap = effectedImage,
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
        // Lớp "trước khi chỉnh" phủ TRÊN CÙNG khi đè nút so sánh: ảnh gốc nguyên bản
        // (không filter màu, không sticker, không nền), cùng transform zoom/pan để hai
        // ảnh thẳng hàng tuyệt đối. Image không có pointerInput nên không nuốt cử chỉ.
        if (showOriginal) {
            val originalImage = remember(originalBitmap) { originalBitmap.asImageBitmap() }
            Image(
                bitmap = originalImage,
                contentDescription = "Ảnh gốc",
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
            )
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
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onAutoBeautyToggle: () -> Unit,
    onEyeEnlargeChange: (Float) -> Unit,
    onLipColorChange: (Float) -> Unit,
    onLipShadeChange: (Color) -> Unit,
    onTeethWhitenChange: (Float) -> Unit,
    onBlushChange: (Float) -> Unit,
    onFaceSlimChange: (Float) -> Unit,
    onSkinSmoothChange: (Float) -> Unit,
    onSkinBrightenChange: (Float) -> Unit,
    onBrushModeChange: (BrushMode) -> Unit,
    onBrushColorChange: (Color) -> Unit,
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
    Surface(
        // Vùng công cụ RIÊNG dưới ảnh: nền đặc, mép vuông (không bo góc) + đổ bóng lên
        // trên để ngăn cách rõ với ảnh. Đặt NGOÀI AnimatedContent để nền surface luôn
        // phủ kín khi panel đổi/đổi cỡ → không lộ nền trắng phía sau gây nháy lúc đổi tab.
        modifier = modifier
            .fillMaxWidth()
            .blockPointerThrough(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 12.dp,
    ) {
        AnimatedContent(
            targetState = editor.activeTool,
            transitionSpec = {
                (fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 4 }) togetherWith
                    (fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 4 })
            },
            label = "tool-panel",
            modifier = Modifier.fillMaxWidth(),
        ) { tool ->
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
                        brightness = editor.brightness,
                        contrast = editor.contrast,
                        saturation = editor.saturation,
                        onBrightnessChange = onBrightnessChange,
                        onContrastChange = onContrastChange,
                        onSaturationChange = onSaturationChange,
                    )
                    EditorTool.FACE -> FaceToolPanel(
                        eyeEnlarge = editor.eyeEnlarge,
                        lipColor = editor.lipColor,
                        lipShade = editor.lipShade,
                        teethWhiten = editor.teethWhiten,
                        blush = editor.blush,
                        faceSlim = editor.faceSlim,
                        skinSmooth = editor.skinSmooth,
                        skinBrighten = editor.skinBrighten,
                        autoApplied = editor.isAutoBeauty,
                        faceDetected = editor.faceDetected,
                        onAutoBeautyToggle = onAutoBeautyToggle,
                        onEyeEnlargeChange = onEyeEnlargeChange,
                        onLipColorChange = onLipColorChange,
                        onLipShadeChange = onLipShadeChange,
                        onTeethWhitenChange = onTeethWhitenChange,
                        onBlushChange = onBlushChange,
                        onFaceSlimChange = onFaceSlimChange,
                        onSkinSmoothChange = onSkinSmoothChange,
                        onSkinBrightenChange = onSkinBrightenChange,
                    )
                    EditorTool.ERASE -> EraseToolPanel(
                        brushMode = editor.brushMode,
                        brushColor = editor.brushColor,
                        brushSize = editor.brushSize,
                        onModeChange = onBrushModeChange,
                        onColorChange = onBrushColorChange,
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
        EditorTool.FACE -> "Chỉnh mặt"
        EditorTool.EFFECTS -> "Hiệu ứng"
        EditorTool.CROP -> "Cắt ảnh"
        EditorTool.TEXT -> "Chữ"
    }
