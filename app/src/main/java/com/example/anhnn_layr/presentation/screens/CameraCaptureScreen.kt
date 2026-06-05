package com.example.anhnn_layr.presentation.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.anhnn_layr.presentation.components.AnhnnGradientButton
import com.example.anhnn_layr.presentation.components.CustomZoomSlider
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleDark
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleGradient
import com.example.anhnn_layr.utils.saveCaptureToGallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Màn chụp ảnh trong app (CameraX). Sau khi chụp sẽ hiện preview để người dùng
 * chọn "Chụp lại" hoặc "Dùng ảnh". Bấm "Dùng ảnh" sẽ lưu ảnh vào thư viện ảnh
 * của máy rồi gọi [onPhotoSaved] (để mở màn Thư viện).
 */
@Composable
fun CameraCaptureScreen(
    onPhotoSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    // Người dùng đã từ chối kèm "Không hỏi lại": gọi launch() lúc này hệ thống sẽ
    // bỏ qua, nên phải hướng họ vào Cài đặt để tự bật quyền.
    var permanentlyDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (!granted && activity != null) {
            permanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA,
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Khi quay lại từ màn Cài đặt, kiểm tra lại quyền để cập nhật giao diện ngay.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED
                hasPermission = granted
                if (granted) permanentlyDenied = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val openAppSettings = {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        }
        Unit
    }

    // Hiện hộp thoại hỏi mở Cài đặt khi người dùng đã từ chối quyền trước đó.
    var showSettingsDialog by remember { mutableStateOf(false) }

    var capturedFile by remember { mutableStateOf<File?>(null) }
    var saving by remember { mutableStateOf(false) }

    if (!hasPermission) {
        CameraPermissionDenied(
            permanentlyDenied = permanentlyDenied,
            onGrant = {
                if (permanentlyDenied) {
                    showSettingsDialog = true
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onBack = onBack,
        )
        if (showSettingsDialog) {
            OpenSettingsDialog(
                onConfirm = {
                    showSettingsDialog = false
                    openAppSettings()
                },
                onDismiss = { showSettingsDialog = false },
            )
        }
        return
    }

    val file = capturedFile
    if (file != null) {
        val uri = remember(file) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
        CapturedPreview(
            uri = uri,
            saving = saving,
            onRetake = {
                runCatching { file.delete() }
                capturedFile = null
            },
            onConfirm = {
                if (saving) return@CapturedPreview
                saving = true
                scope.launch {
                    val result = runCatching {
                        withContext(Dispatchers.IO) { saveCaptureToGallery(context, file) }
                    }
                    saving = false
                    if (result.isSuccess) {
                        runCatching { file.delete() }
                        onPhotoSaved()
                    } else {
                        Toast.makeText(context, "Lưu ảnh thất bại", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onBack = onBack,
        )
        return
    }

    LiveCamera(
        onCaptured = { capturedFile = it },
        onBack = onBack,
    )
}

@Composable
private fun LiveCamera(
    onCaptured: (File) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by rememberSaveable { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    // Camera đang bind — cần để điều khiển zoom. Cập nhật lại mỗi lần đổi ống kính.
    var camera by remember { mutableStateOf<Camera?>(null) }
    // Vị trí thanh kéo (0f..1f): setLinearZoom map tuyến tính theo góc nhìn, mượt
    // hơn so với set thẳng zoomRatio. Giữ lại khi xoay máy / đổi ống kính.
    var linearZoom by rememberSaveable { mutableStateOf(0f) }

    DisposableEffect(lensFacing) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose { runCatching { future.get().unbindAll() } }
    }

    // Áp mức zoom lên camera mỗi khi kéo thanh hoặc vừa bind camera mới.
    LaunchedEffect(camera, linearZoom) {
        camera?.cameraControl?.setLinearZoom(linearZoom)
    }

    val takePhoto = takePhoto@{
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onCaptured(file)
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        context,
                        "Không chụp được ảnh: ${exc.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        )
        Unit
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                // Pinch (chụm 2 ngón) để phóng to/thu nhỏ — cập nhật linearZoom,
                // LaunchedEffect ở trên sẽ áp setLinearZoom lên camera.
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        linearZoom = (linearZoom + (zoom - 1f) * 0.5f).coerceIn(0f, 1f)
                    }
                },
        )

        CircleIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Quay lại",
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Thanh kéo phóng to (zoom) kiểu máy ảnh — nằm ngang phía trên nút chụp.
            ZoomBar(
                linearZoom = linearZoom,
                onZoomChange = { linearZoom = it },
                modifier = Modifier.padding(bottom = 20.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Spacer giữ nút chụp ở chính giữa.
                Box(modifier = Modifier.size(52.dp))

                ShutterButton(onClick = takePhoto)

                CircleIconButton(
                    icon = Icons.Outlined.Cameraswitch,
                    contentDescription = "Đổi camera",
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                )
            }
        }
    }
}

/**
 * Thanh kéo zoom kiểu máy ảnh: nút trừ/cộng hai bên + [CustomZoomSlider] ở giữa,
 * bọc trong một pill nền đen mờ. [linearZoom] trong khoảng 0f..1f.
 */
@Composable
private fun ZoomBar(
    linearZoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ZoomStepButton(
            icon = Icons.Rounded.Remove,
            contentDescription = "Thu nhỏ",
            onClick = { onZoomChange((linearZoom - 0.1f).coerceIn(0f, 1f)) },
        )

        CustomZoomSlider(
            value = linearZoom,
            onValueChange = onZoomChange,
            modifier = Modifier.weight(1f),
        )

        ZoomStepButton(
            icon = Icons.Rounded.Add,
            contentDescription = "Phóng to",
            onClick = { onZoomChange((linearZoom + 0.1f).coerceIn(0f, 1f)) },
        )
    }
}

@Composable
private fun ZoomStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .background(AnhnnPurpleGradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .border(4.dp, Color.White, CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = "Chụp ảnh",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun CapturedPreview(
    uri: Uri,
    saving: Boolean,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = uri,
            contentDescription = "Ảnh vừa chụp",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )

        CircleIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Quay lại",
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable(enabled = !saving, onClick = onRetake)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            ) {
                Text(
                    text = "Chụp lại",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (saving) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 3.dp,
                    )
                }
            } else {
                AnhnnGradientButton(
                    text = "Dùng ảnh",
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionDenied(
    permanentlyDenied: Boolean,
    onGrant: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircleIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Quay lại",
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = null,
                tint = AnhnnPurpleDark,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = "Cần quyền truy cập camera",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (permanentlyDenied) {
                    "Bạn đã từ chối quyền camera. Hãy mở Cài đặt để bật quyền rồi quay lại."
                } else {
                    "Hãy cấp quyền camera để chụp ảnh và chỉnh sửa ngay."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            AnhnnGradientButton(
                text = if (permanentlyDenied) "Mở cài đặt" else "Cấp quyền",
                onClick = onGrant,
            )
        }
    }
}

/**
 * Hộp thoại hỏi người dùng có muốn mở Cài đặt để tự bật quyền hay không, hiện khi
 * họ đã từ chối quyền camera từ trước (không gọi lại hộp thoại hệ thống được nữa).
 */
@Composable
private fun OpenSettingsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Cần quyền truy cập camera") },
        text = {
            Text(
                text = "Bạn đã từ chối quyền camera. Hãy mở Cài đặt để bật quyền rồi quay lại app.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Mở cài đặt", color = AnhnnPurpleDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Đóng")
            }
        },
    )
}
