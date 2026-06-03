package com.example.anhnn_layr.presentation.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.size.SizeResolver
import com.example.anhnn_layr.presentation.components.AnhnnGradientButton
import com.example.anhnn_layr.utils.GalleryPhoto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MaxScale = 5f

/** Cho phép vượt ngưỡng trong lúc kéo, thả tay mới thu lại. */
private const val GestureMinScale = 0.5f
private const val GestureMaxScale = 6f

/** Zoom nhỏ hơn ngưỡng này sẽ thu về 1f khi thả tay. */
private const val SnapBackScaleThreshold = 1.1f

/** Debounce: sau khi ngừng gesture (ms) mới chạy hiệu ứng thu lại. */
private const val GestureEndDelayMs = 180L

private val snapBackSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow,
)

/**
 * Màn xem ảnh toàn màn hình: vuốt ngang để chuyển ảnh, chụm để zoom, kéo khi đã
 * zoom to. Bấm "Chỉnh sửa" để đưa ảnh hiện tại vào luồng tách nền/chỉnh sửa.
 */
@Composable
fun PhotoPreviewScreen(
    photos: List<GalleryPhoto>,
    initialIndex: Int,
    onEdit: (Uri) -> Unit,
    onBack: () -> Unit,
) {
    if (photos.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, photos.lastIndex),
        pageCount = { photos.size },
    )
    val scales = remember { mutableStateMapOf<Long, Animatable<Float, AnimationVector1D>>() }

    val currentPhoto = photos.getOrNull(pagerState.currentPage) ?: photos.first()
    val currentScale = scales[currentPhoto.id]?.value ?: 1f

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().clipToBounds(),
            pageSpacing = 16.dp,
            // Khi đang zoom thì khoá vuốt ngang để kéo ảnh.
            userScrollEnabled = currentScale <= 1.05f,
        ) { page ->
            val photo = photos[page]
            val scale = remember(photo.id) { scales.getOrPut(photo.id) { Animatable(1f) } }
            ZoomableImage(uri = photo.uri, scale = scale)
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

        AnhnnGradientButton(
            text = "Chỉnh sửa",
            onClick = { onEdit(currentPhoto.uri) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        )
    }
}

@Composable
private fun ZoomableImage(
    uri: Uri,
    scale: Animatable<Float, AnimationVector1D>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val snapBackJob = remember { mutableStateOf<Job?>(null) }

    fun scheduleSnapBack() {
        snapBackJob.value?.cancel()
        snapBackJob.value = scope.launch {
            delay(GestureEndDelayMs)
            val currScale = scale.value
            val maxX = (containerSize.width * (currScale - 1) / 2f).coerceAtLeast(0f)
            val maxY = (containerSize.height * (currScale - 1) / 2f).coerceAtLeast(0f)
            when {
                currScale < SnapBackScaleThreshold -> {
                    scale.animateTo(1f, snapBackSpring)
                    offsetX.animateTo(0f, snapBackSpring)
                    offsetY.animateTo(0f, snapBackSpring)
                }
                currScale > MaxScale -> {
                    scale.animateTo(MaxScale, snapBackSpring)
                    val maxXAtMax = (containerSize.width * (MaxScale - 1) / 2f).coerceAtLeast(0f)
                    val maxYAtMax = (containerSize.height * (MaxScale - 1) / 2f).coerceAtLeast(0f)
                    offsetX.animateTo(offsetX.value.coerceIn(-maxXAtMax, maxXAtMax), snapBackSpring)
                    offsetY.animateTo(offsetY.value.coerceIn(-maxYAtMax, maxYAtMax), snapBackSpring)
                }
                else -> {
                    val clampedX = offsetX.value.coerceIn(-maxX, maxX)
                    val clampedY = offsetY.value.coerceIn(-maxY, maxY)
                    if (offsetX.value != clampedX || offsetY.value != clampedY) {
                        offsetX.animateTo(clampedX, snapBackSpring)
                        offsetY.animateTo(clampedY, snapBackSpring)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(containerSize) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        when {
                            pressed.size >= 2 -> {
                                event.changes.forEach { it.consume() }
                                val c0 = pressed[0]
                                val c1 = pressed[1]
                                val currDist = (c1.position - c0.position).getDistance()
                                val prevDist = (c1.previousPosition - c0.previousPosition).getDistance()
                                val zoom = if (prevDist > 0f) currDist / prevDist else 1f
                                val currCentroid = (c0.position + c1.position) / 2f
                                val prevCentroid = (c0.previousPosition + c1.previousPosition) / 2f
                                val pan = currCentroid - prevCentroid
                                scope.launch {
                                    val newScale = (scale.value * zoom).coerceIn(GestureMinScale, GestureMaxScale)
                                    scale.snapTo(newScale)
                                    val maxX = (containerSize.width * (newScale - 1) / 2f).coerceAtLeast(0f)
                                    val maxY = (containerSize.height * (newScale - 1) / 2f).coerceAtLeast(0f)
                                    offsetX.snapTo((offsetX.value + pan.x).coerceIn(-maxX, maxX))
                                    offsetY.snapTo((offsetY.value + pan.y).coerceIn(-maxY, maxY))
                                    scheduleSnapBack()
                                }
                            }
                            pressed.size == 1 && scale.value > 1.05f -> {
                                pressed[0].consume()
                                val pan = pressed[0].position - pressed[0].previousPosition
                                scope.launch {
                                    val maxX = (containerSize.width * (scale.value - 1) / 2f).coerceAtLeast(0f)
                                    val maxY = (containerSize.height * (scale.value - 1) / 2f).coerceAtLeast(0f)
                                    offsetX.snapTo((offsetX.value + pan.x).coerceIn(-maxX, maxX))
                                    offsetY.snapTo((offsetY.value + pan.y).coerceIn(-maxY, maxY))
                                    scheduleSnapBack()
                                }
                            }
                            // 1 ngón khi scale ≈ 1f: không consume → Pager xử lý vuốt ngang.
                        }
                    }
                }
            },
    ) {
        AsyncImage(
            model = run {
                val builder = ImageRequest.Builder(context).data(uri)
                if (containerSize.width > 0 && containerSize.height > 0) {
                    val w = (containerSize.width * MaxScale).toInt().coerceIn(2048, 4096)
                    val h = (containerSize.height * MaxScale).toInt().coerceIn(2048, 4096)
                    builder.size(SizeResolver { Size(w, h) })
                }
                builder.build()
            },
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                    translationX = offsetX.value,
                    translationY = offsetY.value,
                ),
            contentScale = ContentScale.Fit,
        )
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
