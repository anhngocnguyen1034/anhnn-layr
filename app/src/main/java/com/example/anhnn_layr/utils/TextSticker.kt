package com.example.anhnn_layr.utils

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class TextStickerFont(val label: String) {
    SANS("Sans"),
    SANS_BLACK("Black"),
    CONDENSED("Gọn"),
    SERIF("Serif"),
    SERIF_ITALIC("Serif nghiêng"),
    MONO("Mono"),
    CASUAL("Casual"),
    CURSIVE("Script"),
}

data class TextSticker(
    val id: String,
    val text: String,
    val center: Offset,
    val textColor: Color = Color.White,
    val outlineColor: Color = Color.Black,
    val outlineWidth: Float = 6f,
    val shadowRadius: Float = 0f,
    val fontSize: Float = 72f,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val font: TextStickerFont = TextStickerFont.SANS,
)

fun drawTextStickers(
    canvas: Canvas,
    stickers: List<TextSticker>,
) {
    stickers.forEach { sticker ->
        if (sticker.text.isBlank()) return@forEach
        drawTextSticker(canvas, sticker)
    }
}

private fun drawTextSticker(canvas: Canvas, sticker: TextSticker) {
    val lines = sticker.text.lines().ifEmpty { listOf(sticker.text) }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = sticker.fontSize
        typeface = sticker.font.toTypeface()
    }
    val lineHeight = paint.fontSpacing
    val totalHeight = lineHeight * lines.size
    val firstBaseline = -totalHeight / 2f - paint.fontMetrics.ascent

    canvas.save()
    canvas.translate(sticker.center.x, sticker.center.y)
    canvas.rotate(sticker.rotation)
    canvas.scale(sticker.scale, sticker.scale)

    if (sticker.outlineWidth > 0f) {
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeWidth = sticker.outlineWidth
        paint.color = sticker.outlineColor.toArgb()
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, 0f, firstBaseline + index * lineHeight, paint)
        }
    }

    paint.style = Paint.Style.FILL
    paint.strokeWidth = 0f
    paint.color = sticker.textColor.toArgb()
    if (sticker.shadowRadius > 0f) {
        paint.setShadowLayer(sticker.shadowRadius, 0f, sticker.shadowRadius / 2f, Color.Black.toArgb())
    } else {
        paint.clearShadowLayer()
    }
    lines.forEachIndexed { index, line ->
        canvas.drawText(line, 0f, firstBaseline + index * lineHeight, paint)
    }
    canvas.restore()
}

private fun TextStickerFont.toTypeface(): Typeface = when (this) {
    TextStickerFont.SANS -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    TextStickerFont.SANS_BLACK -> Typeface.create("sans-serif-black", Typeface.NORMAL)
    TextStickerFont.CONDENSED -> Typeface.create("sans-serif-condensed", Typeface.BOLD)
    TextStickerFont.SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
    TextStickerFont.SERIF_ITALIC -> Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
    TextStickerFont.MONO -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    TextStickerFont.CASUAL -> Typeface.create("casual", Typeface.NORMAL)
    TextStickerFont.CURSIVE -> Typeface.create("cursive", Typeface.BOLD)
}
