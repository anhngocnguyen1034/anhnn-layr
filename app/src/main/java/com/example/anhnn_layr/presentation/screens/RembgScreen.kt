package com.example.anhnn_layr.presentation.screens

import android.net.Uri
import android.widget.Toast
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
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleDark
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleLight
import com.example.anhnn_layr.presentation.viewmodels.RembgUiState
import com.example.anhnn_layr.presentation.viewmodels.RembgViewModel

@Composable
fun RembgScreen(vm: RembgViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val editor by vm.editor.collectAsState()
    val drafts by vm.drafts.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(vm) {
        vm.messages.collect { msg ->
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        }
    }

    var showUpscale by remember { mutableStateOf(false) }

    if (showUpscale) {
        UpscaleScreen(onBack = { showUpscale = false })
        return
    }

    when (val s = state) {
        RembgUiState.Idle -> HomeScreen(
            onImagePicked = { uri, model ->
                val mime = ctx.contentResolver.getType(uri)
                vm.remove(uri, model, mime)
            },
            drafts = drafts,
            onOpenDraft = vm::openDraft,
            onDeleteDraft = vm::deleteDraft,
            onOpenUpscale = { showUpscale = true },
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
            onEraseModeChange = vm::setEraseMode,
            onBrushSizeChange = vm::setBrushSize,
            onFeatherChange = vm::setFeatherRadius,
            onBackgroundImageSelected = vm::setBackgroundImage,
            onBackgroundBlurChange = vm::setBackgroundBlur,
            onUseOriginalBackground = vm::useOriginalAsBackground,
            onOutlineWidthChange = vm::setOutlineWidth,
            onOutlineColorChange = vm::setOutlineColor,
            onShadowRadiusChange = vm::setShadowRadius,
            onBrightnessChange = vm::setBrightness,
            onContrastChange = vm::setContrast,
            onSaturationChange = vm::setSaturation,
            onCrop = { preset -> vm.cropToAspect(preset.width, preset.height) },
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
