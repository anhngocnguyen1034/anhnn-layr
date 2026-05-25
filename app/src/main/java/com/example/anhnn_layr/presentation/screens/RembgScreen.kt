package com.example.anhnn_layr.presentation.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.anhnn_layr.presentation.components.AnhnnGradientButton
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleDark
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleLight
import com.example.anhnn_layr.presentation.viewmodels.RembgUiState
import com.example.anhnn_layr.presentation.viewmodels.RembgViewModel
import kotlin.math.roundToInt

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

    when (val s = state) {
        RembgUiState.Idle -> HomeScreen(
            onImagePicked = { uri, model ->
                val mime = ctx.contentResolver.getType(uri)
                vm.remove(uri, model, mime)
            },
            onImagePickedForLasso = { uri, _ ->
                val mime = ctx.contentResolver.getType(uri)
                vm.pickForLasso(uri, sourceMimeType = mime)
            },
            drafts = drafts,
            onOpenDraft = vm::openDraft,
            onDeleteDraft = vm::deleteDraft,
        )
        is RembgUiState.AwaitingLasso -> LassoScreen(
            sourceUri = s.sourceUri,
            onConfirm = vm::confirmLasso,
            onSkip = vm::skipLasso,
            onCancel = vm::cancelLasso,
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
                ScanningImagePreview(sourceUri = sourceUri)
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
private fun ScanningImagePreview(sourceUri: Uri) {
    val imageSizeState = remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val scanHeightPx = with(density) { 96.dp.toPx() }
    val transition = rememberInfiniteTransition(label = "background-removal-scan")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scan-progress",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { imageSizeState.value = it },
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
                .fillMaxWidth()
                .height(96.dp)
                .offset {
                    val imageSize = imageSizeState.value
                    val travel = imageSize.height + scanHeightPx
                    IntOffset(x = 0, y = (travel * progress - scanHeightPx).roundToInt())
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            AnhnnPurpleLight.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.72f),
                            AnhnnPurpleDark.copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .size(28.dp),
            strokeWidth = 3.dp,
            color = AnhnnPurpleDark,
        )
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
