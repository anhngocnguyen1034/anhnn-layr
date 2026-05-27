package com.example.anhnn_layr.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.annotation.FontRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.example.anhnn_layr.R

enum class TextStickerFont(val label: String, @FontRes val fontRes: Int) {
    INTER("Inter", R.font.inter_18pt_regular),
    ROBOTO("Roboto", R.font.roboto_regular),
    MONTSERRAT("Montserrat", R.font.montserrat_regular),
    OSWALD("Oswald", R.font.oswald_regular),
    ANTON("Anton", R.font.anton_regular),
    BEVIETNAM("Be Vietnam Pro", R.font.bevietnampro_regular),
    QUICKSAND("Quicksand", R.font.quicksand_regular),
    LORA("Lora", R.font.lora_regular),
    PLAYFAIR("Playfair Display", R.font.playfairdisplay_regular),
    MERRIWEATHER("Merriweather", R.font.merriweather_24pt_semicondensed_regular),
    EB_GARAMOND("EB Garamond", R.font.ebgaramond_regular),
    BANGERS("Bangers", R.font.bangers_regular),
    LOBSTER("Lobster", R.font.lobster_regular),
    PACIFICO("Pacifico", R.font.pacifico_regular),
    CAVEAT("Caveat", R.font.caveat_regular),
    DANCING_SCRIPT("Dancing Script", R.font.dancingscript_regular),
    SATISFY("Satisfy", R.font.satisfy_regular),
    GREAT_VIBES("Great Vibes", R.font.greatvibes_regular),
    ALEX_BRUSH("Alex Brush", R.font.alexbrush_regular),
    KELLY_SLAB("Kelly Slab", R.font.kellyslab_regular),
    SPECIAL_ELITE("Special Elite", R.font.specialelite_regular),
    FREDERICKA("Fredericka", R.font.frederickathegreat_regular),
    LONDRINA("Londrina Sketch", R.font.londrinasketch_regular),
    LOVEYA("Love Ya", R.font.loveyalikeasister_regular),
    PIEDRA("Piedra", R.font.piedra_regular),
    RYE("Rye", R.font.rye_regular),
    SANCREEK("Sancreek", R.font.sancreek_regular),
    UNCIAL("Uncial Antiqua", R.font.uncialantiqua_regular),
    VAST_SHADOW("Vast Shadow", R.font.vastshadow_regular),
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
    val font: TextStickerFont = TextStickerFont.INTER,
)

private val typefaceCache = mutableMapOf<Int, Typeface>()

fun TextStickerFont.loadTypeface(context: Context): Typeface {
    typefaceCache[fontRes]?.let { return it }
    val tf = runCatching { ResourcesCompat.getFont(context, fontRes) }.getOrNull()
        ?: Typeface.DEFAULT
    typefaceCache[fontRes] = tf
    return tf
}

fun drawTextStickers(
    context: Context,
    canvas: Canvas,
    stickers: List<TextSticker>,
) {
    stickers.forEach { sticker ->
        if (sticker.text.isBlank()) return@forEach
        drawTextSticker(context, canvas, sticker)
    }
}

private fun drawTextSticker(context: Context, canvas: Canvas, sticker: TextSticker) {
    val lines = sticker.text.lines().ifEmpty { listOf(sticker.text) }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = sticker.fontSize
        typeface = sticker.font.loadTypeface(context)
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
