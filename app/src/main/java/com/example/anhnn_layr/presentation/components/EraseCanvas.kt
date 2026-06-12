package com.example.anhnn_layr.presentation.components

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.utils.BrushMode
import com.example.anhnn_layr.utils.TouchPath

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f

// Kính lúp: phóng thêm LOUPE_FACTOR lần so với mức zoom đang xem.
private const val LOUPE_FACTOR = 2f
private val LOUPE_SIZE = 110.dp
private val LOUPE_MARGIN = 12.dp

@Composable
fun EraseCanvas(
    workingBitmap: Bitmap,
    originalBitmap: Bitmap,
    brushMode: BrushMode,
    brushColor: Color,
    brushSize: Float,
    scale: Float,
    offset: Offset,
    onTransform: (newScale: Float, newOffset: Offset) -> Unit,
    onCommitPath: (TouchPath) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentPath by remember { mutableStateOf<Path?>(null, neverEqualPolicy()) }
    // Nét vừa thả tay: giữ làm lớp phủ tạm cho tới khi workingBitmap mới (đã có nét)
    // được dựng xong và truyền vào — tránh nháy do bitmap rebuild bất đồng bộ.
    var pendingPath by remember { mutableStateOf<Path?>(null, neverEqualPolicy()) }
    var lastPoint by remember { mutableStateOf(Offset.Zero) }
    val currentPoints = remember { mutableListOf<Offset>() }
    // Cỡ cọ hiệu dụng (toạ độ bitmap) chốt tại lúc đặt ngón: chia cho scale để khi
    // zoom vào cọ thực tế nhỏ lại tương ứng — độ to trên màn hình giữ nguyên, zoom
    // càng sâu vẽ càng tinh. Giữ riêng cho nét đang vẽ + nét chờ bake để đổi cỡ cọ
    // giữa chừng không làm lệch lớp phủ.
    var strokeBrushSize by remember { mutableStateOf(brushSize) }
    // Vị trí ngón (toạ độ bitmap) khi đang vẽ — null = không hiện kính lúp.
    var fingerPos by remember { mutableStateOf<Offset?>(null) }
    val view = LocalView.current

    val bmpW = workingBitmap.width.toFloat()
    val bmpH = workingBitmap.height.toFloat()
    val workingImage = remember(workingBitmap) { workingBitmap.asImageBitmap() }
    val originalImage = remember(originalBitmap) { originalBitmap.asImageBitmap() }
    val bmpIntSize = remember(workingBitmap) { IntSize(workingBitmap.width, workingBitmap.height) }

    // workingBitmap đổi = nét đã nằm trong bitmap, bỏ lớp phủ tạm.
    LaunchedEffect(workingBitmap) { pendingPath = null }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(bmpW / bmpH)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .pointerInput(brushMode, brushColor, brushSize, bmpW, bmpH, scale, offset) {
                awaitEachGesture {
                    val canvasW = size.width.toFloat()
                    val canvasH = size.height.toFloat()
                    var transforming = false
                    var brushStarted = false

                    fun screenToBitmap(p: Offset): Offset {
                        val contentX = (p.x - offset.x) / scale
                        val contentY = (p.y - offset.y) / scale
                        val s = bmpW / canvasW
                        return Offset(contentX * s, contentY * s)
                    }

                    val firstChange = awaitFirstDown(requireUnconsumed = false)
                    // Nhận diện tap nhanh nhiều ngón (2 ngón = undo, 3 ngón = redo):
                    // đếm số ngón tối đa + tổng dịch chuyển để phân biệt với pinch zoom.
                    val downTime = firstChange.uptimeMillis
                    var lastTime = downTime
                    var maxPointers = 1
                    var multiMoved = 0f
                    val firstBmp = screenToBitmap(firstChange.position)
                    strokeBrushSize = (brushSize / scale).coerceAtLeast(1f)
                    lastPoint = firstBmp
                    fingerPos = firstBmp
                    currentPath = Path().apply { moveTo(firstBmp.x, firstBmp.y) }
                    currentPoints.clear()
                    currentPoints.add(firstBmp)
                    brushStarted = true
                    firstChange.consume()

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        event.changes.firstOrNull()?.let { lastTime = it.uptimeMillis }
                        val active = event.changes.filter { it.pressed }
                        if (active.size > maxPointers) maxPointers = active.size
                        if (active.isEmpty()) break

                        if (active.size >= 2) {
                            if (brushStarted) {
                                currentPath = null
                                brushStarted = false
                                fingerPos = null
                            }
                            transforming = true
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = false)
                            multiMoved += pan.getDistance() + kotlin.math.abs(zoom - 1f) * 300f
                            val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            val actualZoom = if (scale == 0f) 1f else newScale / scale
                            val newOffset = Offset(
                                x = offset.x + pan.x + (offset.x - centroid.x) * (actualZoom - 1f),
                                y = offset.y + pan.y + (offset.y - centroid.y) * (actualZoom - 1f),
                            )
                            val clamped = clampPan(newOffset, newScale, canvasW, canvasH)
                            onTransform(newScale, clamped)
                            active.forEach { it.consume() }
                        } else if (!transforming && brushStarted) {
                            val ch = active.first()
                            // Rung nhẹ đúng 1 lần khi nét bắt đầu thực sự kéo (không rung
                            // lúc đặt ngón để pinch zoom không bị rung oan).
                            if (currentPoints.size == 1) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            val cur = screenToBitmap(ch.position)
                            val mid = Offset(
                                (lastPoint.x + cur.x) / 2f,
                                (lastPoint.y + cur.y) / 2f,
                            )
                            currentPath = currentPath?.apply {
                                quadraticBezierTo(lastPoint.x, lastPoint.y, mid.x, mid.y)
                            }
                            currentPoints.add(cur)
                            lastPoint = cur
                            fingerPos = cur
                            ch.consume()
                        } else {
                            active.forEach { it.consume() }
                        }
                    }

                    // Tap nhanh nhiều ngón, gần như không di chuyển → undo/redo.
                    if (maxPointers >= 2 && lastTime - downTime < 350 && multiMoved < 24f) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        if (maxPointers == 2) onUndo() else onRedo()
                    }

                    if (brushStarted && currentPoints.isNotEmpty()) {
                        // Chuyển nét sang lớp phủ tạm trước khi xoá nét đang vẽ, để nét
                        // không biến mất trong lúc chờ workingBitmap được dựng lại.
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        pendingPath = currentPath
                        onCommitPath(
                            TouchPath(
                                points = currentPoints.toList(),
                                mode = brushMode,
                                brushSize = strokeBrushSize,
                                color = brushColor.toArgb(),
                            )
                        )
                    }
                    currentPath = null
                    currentPoints.clear()
                    fingerPos = null
                }
            },
    ) {
        val bmpToCanvas = size.width / bmpW
        withTransform({
            translate(left = offset.x, top = offset.y)
            scale(scale * bmpToCanvas, scale * bmpToCanvas, pivot = Offset.Zero)
        }) {
            drawImage(
                image = workingImage,
                srcOffset = IntOffset.Zero,
                srcSize = bmpIntSize,
                dstOffset = IntOffset.Zero,
                dstSize = bmpIntSize,
            )
            // Nét đã thả tay (chờ bitmap) vẽ trước, rồi tới nét đang vẽ.
            pendingPath?.let { drawStrokeOverlay(it, brushMode, brushColor, strokeBrushSize, originalImage, bmpIntSize) }
            currentPath?.let { drawStrokeOverlay(it, brushMode, brushColor, strokeBrushSize, originalImage, bmpIntSize) }
        }
        // Kính lúp: ngón tay che mất điểm đang vẽ nên hiện vùng dưới ngón ở góc trên,
        // phóng LOUPE_FACTOR lần so với mức zoom đang xem, kèm vòng tròn cỡ cọ.
        fingerPos?.let { fp ->
            drawLoupe(
                finger = fp,
                viewScale = scale * bmpToCanvas,
                viewOffset = offset,
                brushSizeBmp = strokeBrushSize,
                workingImage = workingImage,
                pendingPath = pendingPath,
                currentPath = currentPath,
                brushMode = brushMode,
                brushColor = brushColor,
                originalImage = originalImage,
                bmpIntSize = bmpIntSize,
            )
        }
    }
}

/** Vẽ ô kính lúp ở góc trên, tự né sang góc đối diện khi ngón tay tới gần. */
private fun DrawScope.drawLoupe(
    finger: Offset,
    viewScale: Float,
    viewOffset: Offset,
    brushSizeBmp: Float,
    workingImage: ImageBitmap,
    pendingPath: Path?,
    currentPath: Path?,
    brushMode: BrushMode,
    brushColor: Color,
    originalImage: ImageBitmap,
    bmpIntSize: IntSize,
) {
    val loupeSize = LOUPE_SIZE.toPx()
    val margin = LOUPE_MARGIN.toPx()
    // Vị trí ngón trên màn hình để biết có đè lên ô lúp góc trái không.
    val fingerScreen = finger * viewScale + viewOffset
    val nearLeft = fingerScreen.x < margin * 2 + loupeSize && fingerScreen.y < margin * 2 + loupeSize
    val left = if (nearLeft) size.width - margin - loupeSize else margin
    val rect = Rect(Offset(left, margin), Size(loupeSize, loupeSize))
    // bitmap → lúp: phóng thêm LOUPE_FACTOR lần so với mức đang xem.
    val k = viewScale * LOUPE_FACTOR

    clipRect(rect.left, rect.top, rect.right, rect.bottom) {
        drawRect(color = Color.Black, topLeft = rect.topLeft, size = rect.size)
        withTransform({
            translate(
                left = rect.center.x - finger.x * k,
                top = rect.center.y - finger.y * k,
            )
            scale(k, k, pivot = Offset.Zero)
        }) {
            drawImage(
                image = workingImage,
                srcOffset = IntOffset.Zero,
                srcSize = bmpIntSize,
                dstOffset = IntOffset.Zero,
                dstSize = bmpIntSize,
            )
            pendingPath?.let { drawStrokeOverlay(it, brushMode, brushColor, brushSizeBmp, originalImage, bmpIntSize) }
            currentPath?.let { drawStrokeOverlay(it, brushMode, brushColor, brushSizeBmp, originalImage, bmpIntSize) }
        }
    }
    // Vòng tròn cỡ cọ tại tâm lúp + viền ô.
    drawCircle(
        color = Color.White,
        radius = brushSizeBmp * k / 2f,
        center = rect.center,
        style = Stroke(width = 1.5.dp.toPx()),
    )
    drawRect(
        color = Color.White,
        topLeft = rect.topLeft,
        size = rect.size,
        style = Stroke(width = 2.dp.toPx()),
    )
}

/**
 * Vẽ lớp phủ một nét cọ: xoá (BlendMode.Clear), tô màu (vẽ màu đè lên), hoặc
 * phục hồi (clip ảnh gốc).
 */
private fun DrawScope.drawStrokeOverlay(
    path: Path,
    brushMode: BrushMode,
    brushColor: Color,
    brushSize: Float,
    originalImage: ImageBitmap,
    bmpIntSize: IntSize,
) {
    when (brushMode) {
        BrushMode.ERASE -> drawPath(
            path = path,
            color = Color.Black,
            blendMode = BlendMode.Clear,
            style = Stroke(
                width = brushSize,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        BrushMode.PAINT -> drawPath(
            path = path,
            color = brushColor,
            style = Stroke(
                width = brushSize,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        BrushMode.RESTORE -> {
            val fillPath = Path()
            strokeOutline(brushSize).getFillPath(path.asAndroidPath(), fillPath.asAndroidPath())
            clipPath(fillPath) {
                drawImage(
                    image = originalImage,
                    srcOffset = IntOffset.Zero,
                    srcSize = bmpIntSize,
                    dstOffset = IntOffset.Zero,
                    dstSize = bmpIntSize,
                )
            }
        }
    }
}

private fun strokeOutline(width: Float): android.graphics.Paint = android.graphics.Paint().apply {
    style = android.graphics.Paint.Style.STROKE
    strokeWidth = width
    strokeCap = android.graphics.Paint.Cap.ROUND
    strokeJoin = android.graphics.Paint.Join.ROUND
}

private fun clampPan(offset: Offset, scale: Float, canvasW: Float, canvasH: Float): Offset {
    if (scale <= 1f) return Offset.Zero
    val maxX = 0f
    val minX = canvasW - canvasW * scale
    val maxY = 0f
    val minY = canvasH - canvasH * scale
    return Offset(
        x = offset.x.coerceIn(minX, maxX),
        y = offset.y.coerceIn(minY, maxY),
    )
}
