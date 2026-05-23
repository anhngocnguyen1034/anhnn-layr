package com.example.anhnn_layr.presentation.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.anhnn_layr.presentation.components.AnhnnGradientButton
import com.example.anhnn_layr.presentation.components.BackgroundColorPicker
import com.example.anhnn_layr.presentation.components.ModelDropdown
import com.example.anhnn_layr.presentation.components.checkerboardBackground
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleDark
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleLight
import com.example.anhnn_layr.presentation.viewmodels.RembgUiState
import com.example.anhnn_layr.presentation.viewmodels.RembgViewModel
import com.example.anhnn_layr.utils.SaveFormat
import com.example.anhnn_layr.utils.generateFinalBitmap
import com.example.anhnn_layr.utils.saveBitmapToGallery

@Composable
fun RembgScreen(vm: RembgViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    var selectedModel by rememberSaveable { mutableStateOf("u2net") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedColor by remember { mutableStateOf(Color.Transparent) }
    var format by remember { mutableStateOf(SaveFormat.PNG) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            selectedColor = Color.Transparent
            vm.remove(uri, model = selectedModel)
        }
    }

    val subjectBitmap = remember(state) {
        (state as? RembgUiState.Success)?.let {
            BitmapFactory.decodeByteArray(it.pngBytes, 0, it.pngBytes.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Rembg Demo",
            style = MaterialTheme.typography.headlineSmall.copy(
                brush = Brush.verticalGradient(listOf(AnhnnPurpleLight, AnhnnPurpleDark)),
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
        )

        ModelDropdown(selected = selectedModel, onSelected = { selectedModel = it })

        AnhnnGradientButton(
            text = "Chọn ảnh",
            enabled = state !is RembgUiState.Loading,
            onClick = {
                picker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
        )

        when (val s = state) {
            RembgUiState.Idle -> Text("Hãy chọn một ảnh để xoá nền.")
            RembgUiState.Loading -> {
                CircularProgressIndicator()
                Text("Đang xử lý… (lần đầu có thể mất ~30s để tải model)")
            }
            is RembgUiState.Error -> Text(
                "Lỗi: ${s.message}",
                color = MaterialTheme.colorScheme.error,
            )
            is RembgUiState.Success -> {
                val bmp = subjectBitmap
                if (bmp != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                            .then(
                                if (selectedColor == Color.Transparent)
                                    Modifier.checkerboardBackground()
                                else Modifier
                            )
                            .then(
                                if (selectedColor != Color.Transparent)
                                    Modifier.background(selectedColor)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Ảnh đã xoá nền",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }

                    Text("Chọn màu nền", style = MaterialTheme.typography.titleSmall)
                    BackgroundColorPicker(
                        selected = selectedColor,
                        onSelected = { c ->
                            selectedColor = c
                            if (c == Color.Transparent) format = SaveFormat.PNG
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (selectedColor != Color.Transparent) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = format == SaveFormat.PNG,
                                onClick = { format = SaveFormat.PNG },
                                label = { Text("PNG") },
                            )
                            FilterChip(
                                selected = format == SaveFormat.JPEG,
                                onClick = { format = SaveFormat.JPEG },
                                label = { Text("JPEG") },
                            )
                        }
                    } else {
                        Text(
                            "Nền trong suốt → xuất .png",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    AnhnnGradientButton(
                        text = "Lưu ảnh",
                        onClick = {
                            runCatching {
                                val finalBmp = generateFinalBitmap(bmp, selectedColor)
                                val fmt = if (selectedColor == Color.Transparent) SaveFormat.PNG else format
                                saveBitmapToGallery(ctx, finalBmp, fmt)
                            }.onSuccess {
                                Toast.makeText(ctx, "Đã lưu vào Pictures/TayMaySticker", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(ctx, "Lưu thất bại: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                    )
                }
            }
        }
    }
}

