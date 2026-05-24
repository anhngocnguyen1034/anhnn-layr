package com.example.anhnn_layr.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.anhnn_layr.presentation.components.AnhnnGradientButton
import com.example.anhnn_layr.presentation.viewmodels.RembgUiState
import com.example.anhnn_layr.presentation.viewmodels.RembgViewModel

@Composable
fun RembgScreen(vm: RembgViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val editor by vm.editor.collectAsState()

    when (val s = state) {
        RembgUiState.Idle -> HomeScreen(
            onImagePicked = { uri, model -> vm.remove(uri, model) },
        )
        RembgUiState.Loading -> LoadingScreen()
        is RembgUiState.Error -> ErrorScreen(message = s.message, onRetry = vm::reset)
        is RembgUiState.Success -> EditorScreen(
            workingBitmap = s.workingBitmap,
            displayBitmap = s.displayBitmap,
            effectedBitmap = s.effectedBitmap,
            originalBitmap = s.originalBitmap,
            editor = editor,
            onColorChange = vm::setColor,
            onToolChange = vm::setTool,
            onFormatChange = vm::setFormat,
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
            onBack = vm::reset,
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text("Đang xoá nền… (lần đầu có thể mất ~30s để tải model)")
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
