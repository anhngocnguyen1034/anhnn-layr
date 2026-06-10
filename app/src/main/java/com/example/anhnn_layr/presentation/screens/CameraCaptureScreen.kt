package com.example.anhnn_layr.presentation.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.FlashAuto
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
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
import com.example.anhnn_layr.utils.GalleryPhoto
import com.example.anhnn_layr.utils.queryCapturedPhotos
import com.example.anhnn_layr.utils.saveCaptureToGallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * Màn chụp ảnh trong app (CameraX). Sau khi chụp sẽ hiện preview để người dùng
 * chọn "Chụp lại" hoặc "Dùng ảnh". Bấm "Dùng ảnh" sẽ lưu ảnh vào thư viện ảnh
 * của máy rồi gọi [onPhotoSaved] (để mở màn Thư viện). [onOpenGallery] được gọi
 * khi chạm vào thumbnail ảnh gần nhất cạnh nút chụp.
 */
@Composable
fun CameraCaptureScreen(
    onPhotoSaved: () -> Unit,
    onOpenGallery: () -> Unit,
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
        onOpenGallery = onOpenGallery,
        onBack = onBack,
    )
}

@Composable
private fun LiveCamera(
    onCaptured: (File) -> Unit,
    onOpenGallery: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

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
    // Chế độ flash khi chụp (OFF/AUTO/ON) — giữ lại khi xoay máy / đổi ống kính.
    var flashMode by rememberSaveable { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    // Ống kính hiện tại có đèn flash không (camera trước thường không) → ẩn nút.
    val hasFlashUnit = camera?.cameraInfo?.hasFlashUnit() == true
    val isFrontLens = lensFacing == CameraSelector.LENS_FACING_FRONT
    // "Flash màn hình" cho selfie: camera trước không có đèn nên khi bật, lúc chụp
    // toàn màn hình lóe trắng + độ sáng đẩy tối đa để rọi sáng khuôn mặt.
    var screenFlashOn by rememberSaveable { mutableStateOf(false) }
    // true trong lúc đang lóe trắng chờ chụp.
    var screenFlashActive by remember { mutableStateOf(false) }

    // Đẩy/trả độ sáng màn hình theo trạng thái lóe. onDispose trả về mặc định phòng
    // trường hợp rời màn giữa chừng (chụp xong là rời composition ngay).
    val activity = context as? Activity
    LaunchedEffect(screenFlashActive) {
        val window = activity?.window ?: return@LaunchedEffect
        window.attributes = window.attributes.apply {
            screenBrightness = if (screenFlashActive) 1f
            else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            val window = activity?.window ?: return@onDispose
            window.attributes = window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }
    // Điểm vừa chạm lấy nét (toạ độ pixel trên preview) — null khi không hiện vòng nét.
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    // Ảnh chụp gần nhất trong Pictures/AnhnnLayr — thumbnail cạnh nút chụp, chạm mở
    // thư viện. LiveCamera rời composition khi sang màn xem ảnh nên quay lại sẽ tự
    // load lại (ảnh vừa chụp xong hiện ngay).
    var latestPhoto by remember { mutableStateOf<GalleryPhoto?>(null) }
    LaunchedEffect(Unit) {
        latestPhoto = withContext(Dispatchers.IO) {
            runCatching { queryCapturedPhotos(context).firstOrNull() }.getOrNull()
        }
    }

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

    // Áp chế độ flash lên use case chụp — có hiệu lực ngay lần takePicture kế tiếp.
    LaunchedEffect(flashMode) {
        imageCapture.flashMode = flashMode
    }

    val capture = {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    screenFlashActive = false
                    onCaptured(file)
                }

                override fun onError(exc: ImageCaptureException) {
                    screenFlashActive = false
                    Toast.makeText(
                        context,
                        "Không chụp được ảnh: ${exc.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        )
    }
    val takePhoto = takePhoto@{
        if (isFrontLens && screenFlashOn) {
            // Lóe trắng + sáng tối đa, chờ một nhịp cho màn hình rọi lên mặt và AE
            // thích ứng rồi mới chụp; tắt lóe trong callback chụp xong/ lỗi.
            scope.launch {
                screenFlashActive = true
                delay(400)
                capture()
            }
        } else {
            capture()
        }
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
                }
                // Chạm để lấy nét + đo sáng tại điểm chạm. meteringPointFactory tự quy
                // đổi toạ độ view → toạ độ sensor (đúng cả khi đang zoom/lật ống kính).
                // FocusMeteringAction mặc định tự huỷ sau ~5s, trả về lấy nét liên tục.
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val cam = camera ?: return@detectTapGestures
                        val point = previewView.meteringPointFactory
                            .createPoint(offset.x, offset.y)
                        runCatching {
                            cam.cameraControl.startFocusAndMetering(
                                FocusMeteringAction.Builder(point).build(),
                            )
                        }
                        focusPoint = offset
                    }
                },
        )

        // Vòng lấy nét tại điểm chạm: co nhẹ vào rồi tự biến mất.
        focusPoint?.let { point ->
            FocusRing(
                position = point,
                onFinished = { focusPoint = null },
            )
        }

        CircleIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Quay lại",
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
        )

        // Nút flash: ống kính có đèn → xoay vòng Tắt/Tự động/Bật; camera trước không
        // đèn → công tắc "flash màn hình" Tắt/Bật.
        if (hasFlashUnit) {
            CircleIconButton(
                icon = when (flashMode) {
                    ImageCapture.FLASH_MODE_ON -> Icons.Outlined.FlashOn
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Outlined.FlashAuto
                    else -> Icons.Outlined.FlashOff
                },
                contentDescription = when (flashMode) {
                    ImageCapture.FLASH_MODE_ON -> "Flash: bật"
                    ImageCapture.FLASH_MODE_AUTO -> "Flash: tự động"
                    else -> "Flash: tắt"
                },
                onClick = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp),
            )
        } else if (isFrontLens) {
            CircleIconButton(
                icon = if (screenFlashOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                contentDescription = if (screenFlashOn) "Flash màn hình: bật" else "Flash màn hình: tắt",
                onClick = { screenFlashOn = !screenFlashOn },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp),
            )
        }

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
                // Thumbnail ảnh gần nhất — chạm mở thư viện. Chưa có ảnh nào thì là
                // ô trống cùng cỡ để nút chụp vẫn ở chính giữa.
                val latest = latestPhoto
                if (latest != null) {
                    AsyncImage(
                        model = latest.uri,
                        contentDescription = "Ảnh gần nhất — mở thư viện",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenGallery),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(modifier = Modifier.size(52.dp))
                }

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

        // Lớp lóe trắng "flash màn hình" — vẽ TRÊN CÙNG, phủ cả nút bấm trong lúc
        // chờ chụp để ánh sáng màn hình rọi tối đa lên khuôn mặt.
        if (screenFlashActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
            )
        }
    }
}

// Đường kính vòng lấy nét.
private val FOCUS_RING_SIZE = 64.dp

/**
 * Vòng lấy nét tại [position] (tâm vòng = điểm chạm): xuất hiện hơi to rồi co về
 * kích thước thật (180ms), giữ ~600ms rồi gọi [onFinished] để ẩn. Chạm điểm mới khi
 * vòng cũ còn hiện sẽ restart animation nhờ key theo [position].
 */
@Composable
private fun FocusRing(
    position: Offset,
    onFinished: () -> Unit,
) {
    val scale = remember(position) { Animatable(1.35f) }
    LaunchedEffect(position) {
        scale.animateTo(1f, animationSpec = tween(durationMillis = 180))
        delay(600)
        onFinished()
    }
    Box(
        modifier = Modifier
            .offset {
                val half = (FOCUS_RING_SIZE.toPx() / 2f).roundToInt()
                IntOffset(position.x.roundToInt() - half, position.y.roundToInt() - half)
            }
            .size(FOCUS_RING_SIZE)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // Chấm tâm nhỏ cho dễ thấy điểm nét giữa khung hình sáng.
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
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
