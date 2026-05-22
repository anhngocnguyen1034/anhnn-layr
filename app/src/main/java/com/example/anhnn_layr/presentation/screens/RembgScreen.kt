package com.example.anhnn_layr.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.anhnn_layr.presentation.components.AnhnnGradientButton
import com.example.anhnn_layr.presentation.components.ModelDropdown
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleDark
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleLight
import com.example.anhnn_layr.presentation.viewmodels.RembgUiState
import com.example.anhnn_layr.presentation.viewmodels.RembgViewModel

@Composable
fun RembgScreen(vm: RembgViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    var selectedModel by rememberSaveable { mutableStateOf("u2net") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            vm.remove(uri, model = selectedModel)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            is RembgUiState.Success -> AsyncImage(
                model = ImageRequest.Builder(ctx).data(s.pngBytes).build(),
                contentDescription = "Ảnh đã xoá nền",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
