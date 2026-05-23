package com.example.anhnn_layr.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.components.AnhnnGradientButton
import com.example.anhnn_layr.presentation.components.ModelDropdown
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleDark
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleLight

@Composable
fun HomeScreen(
    onImagePicked: (Uri, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedModel by rememberSaveable { mutableStateOf("u2net") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onImagePicked(uri, selectedModel) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Tay Máy Sticker",
            style = MaterialTheme.typography.headlineMedium.copy(
                brush = Brush.verticalGradient(listOf(AnhnnPurpleLight, AnhnnPurpleDark)),
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
        )
        Text(
            "Chọn model tách nền phù hợp, sau đó chọn ảnh từ thư viện.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        ModelDropdown(selected = selectedModel, onSelected = { selectedModel = it })

        AnhnnGradientButton(
            text = "Chọn ảnh từ thư viện",
            onClick = {
                picker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
        )
    }
}
