package com.example.anhnn_layr.presentation.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.anhnn_layr.presentation.components.AnhnnGradientButton
import com.example.anhnn_layr.presentation.screens.layr.LayrEditorScreen
import com.example.anhnn_layr.presentation.screens.layr.LayrMainScreen
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleDark
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleLight
import com.example.anhnn_layr.presentation.viewmodels.RembgUiState
import com.example.anhnn_layr.presentation.viewmodels.RembgViewModel
import com.example.anhnn_layr.utils.GalleryPhoto

@Composable
fun RembgScreen(vm: RembgViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val editor by vm.editor.collectAsState()
    val drafts by vm.drafts.collectAsState()
    val recentPhotos by vm.recentPhotos.collectAsState()
    val ctx = LocalContext.current

    // Tải lại 5 ảnh gần nhất mỗi khi quay về màn Home (Idle).
    LaunchedEffect(state) {
        if (state is RembgUiState.Idle) vm.refreshRecentPhotos()
    }

    LaunchedEffect(vm) {
        vm.messages.collect { msg ->
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Ảnh đang chờ chỉnh sửa (đã chọn/chụp xong) -> hiển thị màn Editor chọn tính năng.
    var pendingEdit by remember { mutableStateOf<Pair<Uri, String?>?>(null) }
    // Ảnh đang đưa vào luồng "Làm nét" (upscale).
    var upscaleTarget by remember { mutableStateOf<Pair<Uri, String?>?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }
    // Khi != null: đang xem ảnh toàn màn hình (danh sách ảnh + vị trí ảnh đang xem).
    var preview by remember { mutableStateOf<Pair<List<GalleryPhoto>, Int>?>(null) }

    // Bộ chọn ảnh hệ thống — chọn xong thì đưa sang màn Editor.
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            pendingEdit = uri to ctx.contentResolver.getType(uri)
        }
    }
    val openImagePicker = {
        imagePicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    // --- Lớp phủ Làm nét (upscale) ---
    upscaleTarget?.let { (uri, mime) ->
        UpscaleScreen(
            initialUri = uri,
            initialMime = mime,
            onBack = { upscaleTarget = null },
        )
        return
    }

    // --- Lớp phủ Camera ---
    if (showCamera) {
        CameraCaptureScreen(
            onPhotoSaved = {
                showCamera = false
                showGallery = true // mở thư viện để chọn ảnh vừa chụp
            },
            onOpenGallery = {
                showCamera = false
                showGallery = true // chạm thumbnail ảnh gần nhất -> mở thư viện
            },
            onBack = { showCamera = false },
        )
        return
    }

    // --- Lớp phủ Thư viện / Xem trước ---
    if (showGallery) {
        val current = preview
        if (current != null) {
            PhotoPreviewScreen(
                photos = current.first,
                initialIndex = current.second,
                onEdit = { uri ->
                    preview = null
                    showGallery = false
                    pendingEdit = uri to ctx.contentResolver.getType(uri)
                },
                onBack = { preview = null },
            )
        } else {
            GalleryScreen(
                onOpenPhoto = { photos, index -> preview = photos to index },
                onBack = { showGallery = false },
            )
        }
        return
    }

    // --- Màn Editor chọn tính năng (sau khi đã có ảnh) ---
    pendingEdit?.let { (uri, mime) ->
        LayrEditorScreen(
            imageUri = uri,
            onBack = { pendingEdit = null },
            onSharpen = {
                pendingEdit = null
                upscaleTarget = uri to mime
            },
            onRemoveBackground = {
                pendingEdit = null
                vm.remove(uri, sourceMimeType = mime) // model mặc định isnet-general-use
            },
            onEdit = {
                pendingEdit = null
                vm.edit(uri, sourceMimeType = mime) // chỉnh ảnh thẳng, không xóa nền/làm nét
            },
        )
        return
    }

    when (val s = state) {
        RembgUiState.Idle -> LayrMainScreen(
            drafts = drafts,
            recentPhotos = recentPhotos,
            onCapture = { showCamera = true },
            onPickFromGallery = openImagePicker,
            onOpenRecentPhoto = { photo ->
                pendingEdit = photo.uri to ctx.contentResolver.getType(photo.uri)
            },
            onSeeAllRecent = { showGallery = true },
            onOpenDraft = vm::openDraft,
            onDeleteDraft = vm::deleteDraft,
        )
        is RembgUiState.Loading -> LoadingScreen(sourceUri = s.sourceUri)
        is RembgUiState.Error -> ErrorScreen(message = s.message, onRetry = vm::reset)
        is RembgUiState.Success -> EditorScreen(
            workingBitmap = s.workingBitmap,
            displayBitmap = s.displayBitmap,
            effectedBitmap = s.effectedBitmap,
            originalBitmap = s.originalBitmap,
            editor = editor,
            onColorChange = vm::setColor,
            onToolChange = vm::setTool,
            onBrushModeChange = vm::setBrushMode,
            onBrushColorChange = vm::setBrushColor,
            onBrushSizeChange = vm::setBrushSize,
            onFeatherChange = vm::setFeatherRadius,
            onBackgroundImageSelected = vm::setBackgroundImage,
            onBackgroundBlurChange = vm::setBackgroundBlur,
            onUseOriginalBackground = vm::useOriginalAsBackground,
            onBrightnessChange = vm::setBrightness,
            onContrastChange = vm::setContrast,
            onSaturationChange = vm::setSaturation,
            onColorPresetChange = vm::setColorPreset,
            onAutoBeautyToggle = vm::toggleAutoBeauty,
            onEyeEnlargeChange = vm::setEyeEnlarge,
            onLipColorChange = vm::setLipColor,
            onLipShadeChange = vm::setLipShade,
            onTeethWhitenChange = vm::setTeethWhiten,
            onBlushChange = vm::setBlush,
            onFaceSlimChange = vm::setFaceSlim,
            onSkinSmoothChange = vm::setSkinSmooth,
            onSkinBrightenChange = vm::setSkinBrighten,
            onSelectCropAspect = vm::setCropAspect,
            onApplyCrop = vm::applyCrop,
            onResetCrop = vm::resetCrop,
            onCropFrameChange = vm::setCropFrame,
            onAddText = vm::addTextSticker,
            onSelectText = vm::selectTextSticker,
            onTextChange = vm::setSelectedText,
            onTextFontChange = vm::setSelectedTextFont,
            onTextColorChange = vm::setSelectedTextColor,
            onTextOutlineColorChange = vm::setSelectedTextOutlineColor,
            onTextOutlineWidthChange = vm::setSelectedTextOutlineWidth,
            onTextShadowRadiusChange = vm::setSelectedTextShadowRadius,
            onTextFontSizeChange = vm::setSelectedTextFontSize,
            onTextTransform = vm::transformTextSticker,
            onStartTextEdit = vm::beginTextEdit,
            onEndTextEdit = vm::endTextEdit,
            onDeleteText = vm::deleteSelectedTextSticker,
            onCommitPath = vm::commitPath,
            onUndo = vm::undo,
            onRedo = vm::redo,
            onSaveDraft = vm::saveCurrentDraft,
            onBack = vm::reset,
        )
    }
}

@Composable
private fun LoadingScreen(sourceUri: Uri?) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (sourceUri == null) {
                CircularProgressIndicator()
            } else {
                ProcessingImagePreview(sourceUri = sourceUri)
            }

            Text(
                text = "Đang xoá nền ảnh",
                modifier = Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Vui lòng chờ trong giây lát",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ProcessingImagePreview(sourceUri: Uri) {
    val transition = rememberInfiniteTransition(label = "background-removal-processing")
    val pulse by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "processing-pulse",
    )
    val shape = RoundedCornerShape(8.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = AnhnnPurpleDark.copy(alpha = pulse),
        ),
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = sourceUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                AnhnnPurpleLight.copy(alpha = 0.05f),
                                AnhnnPurpleDark.copy(alpha = 0.10f),
                            ),
                        ),
                    ),
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 2.dp,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp),
                    strokeWidth = 3.dp,
                    color = AnhnnPurpleDark,
                )
            }
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Lỗi: $message", color = MaterialTheme.colorScheme.error)
            AnhnnGradientButton(text = "Thử lại", onClick = onRetry)
        }
    }
}
