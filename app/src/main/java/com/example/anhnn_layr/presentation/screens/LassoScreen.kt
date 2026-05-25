package com.example.anhnn_layr.presentation.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.example.anhnn_layr.domain.repository.NormalizedBox
import com.example.anhnn_layr.presentation.components.AnhnnGradientButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LassoScreen(
    sourceUri: Uri,
    onConfirm: (NormalizedBox) -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit,
) {
    val ctx = LocalContext.current
    var imageBitmap by remember(sourceUri) { mutableStateOf<ImageBitmap?>(null) }
    var imageWidth by remember(sourceUri) { mutableStateOf(0) }
    var imageHeight by remember(sourceUri) { mutableStateOf(0) }

    LaunchedEffect(sourceUri) {
        val bmp = withContext(Dispatchers.IO) {
            ctx.contentResolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it)
            }
        }
        if (bmp != null) {
            imageBitmap = bmp.asImageBitmap()
            imageWidth = bmp.width
            imageHeight = bmp.height
        }
    }

    val points = remember(sourceUri) { mutableStateListOf<Offset>() }
    var canvasSize by remember(sourceUri) { mutableStateOf(Size.Zero) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Khoanh vùng vật thể",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Vẽ một nét bao quanh vật thể bạn muốn tách. Hệ thống sẽ dùng vùng này làm gợi ý cho AI.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.04f)),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = imageBitmap
                if (bmp != null && imageWidth > 0 && imageHeight > 0) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(sourceUri) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        points.clear()
                                        points.add(offset)
                                    },
                                    onDrag = { change, _ ->
                                        points.add(change.position)
                                    },
                                )
                            },
                    ) {
                        canvasSize = size
                        val rect = fitRect(size, imageWidth, imageHeight)
                        drawImage(
                            image = bmp,
                            dstOffset = androidx.compose.ui.unit.IntOffset(rect.left.toInt(), rect.top.toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(rect.width.toInt(), rect.height.toInt()),
                        )

                        if (points.isNotEmpty()) {
                            val path = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFF4B4EEE),
                                style = Stroke(
                                    width = 4.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(18f, 12f), 0f,
                                    ),
                                ),
                            )

                            val bbox = computeBoundsPx(points)
                            drawRect(
                                color = Color(0xFFA1A2FF).copy(alpha = 0.85f),
                                topLeft = Offset(bbox.left, bbox.top),
                                size = Size(bbox.right - bbox.left, bbox.bottom - bbox.top),
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnhnnGradientButton(
                text = "Xác nhận vùng đã chọn",
                onClick = {
                    val box = normalizedBoxFromPoints(
                        points = points,
                        canvas = canvasSize,
                        imgW = imageWidth,
                        imgH = imageHeight,
                    ) ?: return@AnhnnGradientButton
                    onConfirm(box)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Bỏ qua, tách tự động")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row3Buttons(
                onClearPath = { points.clear() },
                onCancel = onCancel,
            )
        }
    }
}

@Composable
private fun Row3Buttons(onClearPath: () -> Unit, onCancel: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onClearPath) { Text("Vẽ lại") }
        TextButton(onClick = onCancel) { Text("Huỷ") }
    }
}

private data class RectF(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

private fun fitRect(canvas: Size, imgW: Int, imgH: Int): RectF {
    if (canvas.width <= 0f || canvas.height <= 0f) return RectF(0f, 0f, 0f, 0f)
    val scale = minOf(canvas.width / imgW, canvas.height / imgH)
    val w = imgW * scale
    val h = imgH * scale
    val left = (canvas.width - w) / 2f
    val top = (canvas.height - h) / 2f
    return RectF(left, top, left + w, top + h)
}

private fun computeBoundsPx(points: List<Offset>): RectF {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    for (p in points) {
        if (p.x < minX) minX = p.x
        if (p.y < minY) minY = p.y
        if (p.x > maxX) maxX = p.x
        if (p.y > maxY) maxY = p.y
    }
    return RectF(minX, minY, maxX, maxY)
}

private fun normalizedBoxFromPoints(
    points: List<Offset>,
    canvas: Size,
    imgW: Int,
    imgH: Int,
): NormalizedBox? {
    if (points.size < 2 || imgW <= 0 || imgH <= 0 || canvas.width <= 0f) return null
    val fit = fitRect(canvas, imgW, imgH)
    if (fit.width <= 0f || fit.height <= 0f) return null
    val bbox = computeBoundsPx(points)

    val clampedLeft = bbox.left.coerceIn(fit.left, fit.right)
    val clampedRight = bbox.right.coerceIn(fit.left, fit.right)
    val clampedTop = bbox.top.coerceIn(fit.top, fit.bottom)
    val clampedBottom = bbox.bottom.coerceIn(fit.top, fit.bottom)

    val nx1 = (clampedLeft - fit.left) / fit.width
    val nx2 = (clampedRight - fit.left) / fit.width
    val ny1 = (clampedTop - fit.top) / fit.height
    val ny2 = (clampedBottom - fit.top) / fit.height

    if ((nx2 - nx1) < 0.01f || (ny2 - ny1) < 0.01f) return null
    return NormalizedBox(left = nx1, top = ny1, right = nx2, bottom = ny2)
}
