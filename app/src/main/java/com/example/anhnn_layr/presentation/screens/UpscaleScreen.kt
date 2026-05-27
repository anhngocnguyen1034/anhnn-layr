package com.example.anhnn_layr.presentation.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.anhnn_layr.presentation.components.AnhnnGradientButton
import com.example.anhnn_layr.presentation.components.UpscaleModelDropdown
import com.example.anhnn_layr.presentation.viewmodels.UpscaleUiState
import com.example.anhnn_layr.presentation.viewmodels.UpscaleViewModel
import com.example.anhnn_layr.utils.SaveFormat
import com.example.anhnn_layr.utils.saveBitmapToGallery
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpscaleScreen(
    onBack: () -> Unit,
    vm: UpscaleViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val settings by vm.settings.collectAsState()
    val ctx = LocalContext.current

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val mime = ctx.contentResolver.getType(uri)
            vm.run(uri, mime)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Làm nét ảnh") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state is UpscaleUiState.Idle) onBack() else vm.reset()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                        )
                    }
                },
            )
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
        ) {
            when (val s = state) {
                UpscaleUiState.Idle -> UpscaleIdle(
                    model = settings.model,
                    outscale = settings.outscale,
                    tile = settings.tile,
                    onModelChange = vm::setModel,
                    onOutscaleChange = vm::setOutscale,
                    onTileChange = vm::setTile,
                    onPick = {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                )
                is UpscaleUiState.Loading -> UpscaleLoading(
                    model = settings.model,
                    outscale = settings.outscale,
                )
                is UpscaleUiState.Error -> UpscaleError(
                    message = s.message,
                    onRetry = vm::reset,
                )
                is UpscaleUiState.Success -> UpscaleSuccess(
                    state = s,
                    onSave = {
                        runCatching {
                            saveBitmapToGallery(ctx, s.resultBitmap, SaveFormat.PNG)
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
                    },
                    onAgain = vm::reset,
                )
            }
        }
    }
}

@Composable
private fun UpscaleIdle(
    model: String,
    outscale: Float,
    tile: Int,
    onModelChange: (String) -> Unit,
    onOutscaleChange: (Float) -> Unit,
    onTileChange: (Int) -> Unit,
    onPick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Chọn model & hệ số phóng to, sau đó chọn ảnh để làm nét.",
            style = MaterialTheme.typography.bodyMedium,
        )

        UpscaleModelDropdown(
            selected = model,
            onSelected = onModelChange,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Hệ số phóng to: ${"%.1f".format(outscale)}x",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Slider(
                value = outscale,
                onValueChange = onOutscaleChange,
                valueRange = 1f..8f,
                steps = 13,
            )
        }

        var tileText by remember(tile) { mutableStateOf(if (tile == 0) "" else tile.toString()) }
        OutlinedTextField(
            value = tileText,
            onValueChange = {
                tileText = it.filter { c -> c.isDigit() }
                onTileChange(tileText.toIntOrNull() ?: 0)
            },
            label = { Text("Tile (để trống = tắt)") },
            supportingText = {
                Text("Bật nếu ảnh lớn hoặc gặp lỗi out-of-memory trên server")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        AnhnnGradientButton(
            text = "Chọn ảnh từ thư viện",
            onClick = onPick,
        )
    }
}

@Composable
private fun UpscaleLoading(model: String, outscale: Float) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            "Đang làm nét ảnh ${outscale.roundToInt()}x với $model",
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Quá trình có thể mất từ vài chục giây tới vài phút.",
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
        )
    }
}

@Composable
private fun UpscaleError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Lỗi: $message", color = MaterialTheme.colorScheme.error)
        AnhnnGradientButton(text = "Thử lại", onClick = onRetry)
    }
}

@Composable
private fun UpscaleSuccess(
    state: UpscaleUiState.Success,
    onSave: () -> Unit,
    onAgain: () -> Unit,
) {
    val bmp = state.resultBitmap
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Ảnh gốc", style = MaterialTheme.typography.titleSmall)
        AsyncImage(
            model = state.sourceUri,
            contentDescription = "Ảnh gốc",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat()),
            contentScale = ContentScale.Fit,
        )
        Text(
            "Ảnh sau khi làm nét (${bmp.width} × ${bmp.height})",
            style = MaterialTheme.typography.titleSmall,
        )
        androidx.compose.foundation.Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Ảnh làm nét",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat()),
            contentScale = ContentScale.Fit,
        )
        AnhnnGradientButton(text = "Lưu vào thư viện", onClick = onSave)
        AnhnnGradientButton(text = "Làm nét ảnh khác", onClick = onAgain)
    }
}
