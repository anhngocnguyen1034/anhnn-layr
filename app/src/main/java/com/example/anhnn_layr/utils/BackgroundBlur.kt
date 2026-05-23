package com.example.anhnn_layr.utils

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build

fun blurBackground(src: Bitmap, intensity: Float): Bitmap {
    if (intensity <= 0.5f) return src
    val clamped = intensity.coerceIn(0f, 25f)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching { blurWithRenderEffect(src, clamped) }
            .getOrElse { downscaleBlur(src, clamped) }
    } else {
        downscaleBlur(src, clamped)
    }
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
private fun blurWithRenderEffect(src: Bitmap, radius: Float): Bitmap {
    val w = src.width
    val h = src.height
    val node = RenderNode("blur").apply {
        setPosition(0, 0, w, h)
        setRenderEffect(
            RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
        )
    }
    val canvas = node.beginRecording()
    canvas.drawBitmap(src, 0f, 0f, null)
    node.endRecording()

    val reader = ImageReader.newInstance(
        w, h, PixelFormat.RGBA_8888, 1,
        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
    )
    val renderer = android.graphics.HardwareRenderer().apply {
        setSurface(reader.surface)
        setContentRoot(node)
    }
    renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
    val image = reader.acquireNextImage() ?: error("blur: no image acquired")
    val hardwareBitmap = Bitmap.wrapHardwareBuffer(image.hardwareBuffer!!, null)
        ?: error("blur: wrapHardwareBuffer failed")
    val out = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
    image.close()
    reader.close()
    renderer.destroy()
    node.discardDisplayList()
    return out
}

private fun downscaleBlur(src: Bitmap, intensity: Float): Bitmap {
    val scale = (1f / (1f + intensity * 0.5f)).coerceIn(0.04f, 1f)
    val smallW = (src.width * scale).toInt().coerceAtLeast(2)
    val smallH = (src.height * scale).toInt().coerceAtLeast(2)
    val small = Bitmap.createScaledBitmap(src, smallW, smallH, true)
    val blurred = Bitmap.createScaledBitmap(small, src.width, src.height, true)
    if (small !== blurred) small.recycle()
    return blurred
}
