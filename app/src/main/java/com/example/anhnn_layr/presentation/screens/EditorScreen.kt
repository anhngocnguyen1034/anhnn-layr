package com.example.anhnn_layr.presentation.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.components.EraseCanvas
import com.example.anhnn_layr.presentation.components.ExportBottomSheet
import com.example.anhnn_layr.presentation.components.checkerboardBackground
import com.example.anhnn_layr.presentation.components.tools.BackgroundToolPanel
import com.example.anhnn_layr.presentation.components.tools.ComingSoonPanel
import com.example.anhnn_layr.presentation.components.tools.EraseToolPanel
import com.example.anhnn_layr.presentation.components.tools.ToolTabs
import com.example.anhnn_layr.presentation.viewmodels.EditorState
import com.example.anhnn_layr.presentation.viewmodels.EditorTool
import com.example.anhnn_layr.utils.SaveFormat
import com.example.anhnn_layr.utils.TouchPath
import com.example.anhnn_layr.utils.generateFinalBitmap
import com.example.anhnn_layr.utils.saveBitmapToGallery

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    workingBitmap: Bitmap,
    editor: EditorState,
    onColorChange: (Color) -> Unit,
    onToolChange: (EditorTool) -> Unit,
    onFormatChange: (SaveFormat) -> Unit,
    onEraseModeChange: (Boolean) -> Unit,
    onBrushSizeChange: (Float) -> Unit,
    onCommitPath: (TouchPath) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    var showExport by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { showExport = true }) {
                        Icon(Icons.Outlined.SaveAlt, contentDescription = "Lưu")
                    }
                },
            )
        },
        bottomBar = {
            Column {
                AnimatedContent(targetState = editor.activeTool, label = "tool-panel") { tool ->
                    when (tool) {
                        EditorTool.BACKGROUND -> BackgroundToolPanel(
                            selected = editor.selectedColor,
                            onSelected = onColorChange,
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
                        EditorTool.CROP -> ComingSoonPanel(name = "Cắt")
                        EditorTool.TEXT -> ComingSoonPanel(name = "Chữ")
                    }
                }
                ToolTabs(active = editor.activeTool, onSelect = onToolChange)
            }
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val bgMod = if (editor.selectedColor == Color.Transparent)
                Modifier.checkerboardBackground()
            else Modifier.background(editor.selectedColor)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(workingBitmap.width.toFloat() / workingBitmap.height.toFloat())
                    .then(bgMod),
                contentAlignment = Alignment.Center,
            ) {
                if (editor.activeTool == EditorTool.ERASE) {
                    EraseCanvas(
                        workingBitmap = workingBitmap,
                        isEraseMode = editor.isEraseMode,
                        brushSize = editor.brushSize,
                        onCommitPath = onCommitPath,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Image(
                        bitmap = workingBitmap.asImageBitmap(),
                        contentDescription = "Ảnh đã xoá nền",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }

    if (showExport) {
        ExportBottomSheet(
            selectedColor = editor.selectedColor,
            format = editor.format,
            onFormatChange = onFormatChange,
            onDismiss = { showExport = false },
            onConfirm = {
                runCatching {
                    val finalBmp = generateFinalBitmap(workingBitmap, editor.selectedColor)
                    saveBitmapToGallery(ctx, finalBmp, editor.format)
                }.onSuccess {
                    Toast.makeText(ctx, "Đã lưu vào Pictures/TayMaySticker", Toast.LENGTH_SHORT).show()
                    showExport = false
                }.onFailure {
                    Toast.makeText(ctx, "Lưu thất bại: ${it.message}", Toast.LENGTH_LONG).show()
                }
            },
        )
    }
}
