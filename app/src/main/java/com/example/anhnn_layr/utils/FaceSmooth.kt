package com.example.anhnn_layr.utils

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import kotlin.math.abs

// ID theo chuẩn Canonical Face Mesh (MediaPipe/ML Kit, index 0..467).
// Viền ngoài khuôn mặt (FACE_OVAL) — vùng da cần làm mịn.
private val FACE_OVAL_IDS = intArrayOf(
    10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288, 397, 365, 379, 378,
    400, 377, 152, 148, 176, 149, 150, 136, 172, 58, 132, 93, 234, 127, 162, 21, 54,
    103, 67, 109,
)
// Vành mắt trái/phải + viền ngoài môi — ĐỤC LỖ khỏi mặt nạ để giữ nét sắc.
private val LEFT_EYE_IDS = intArrayOf(33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246)
private val RIGHT_EYE_IDS = intArrayOf(263, 249, 390, 373, 374, 380, 381, 382, 362, 398, 384, 385, 386, 387, 388, 466)
private val LIPS_OUTER_IDS = intArrayOf(61, 185, 40, 39, 37, 0, 267, 269, 270, 409, 291, 375, 321, 405, 314, 17, 84, 181, 91, 146)
private const val MAX_SKIN_ID = 466  // index lớn nhất dùng tới

// Bề rộng tối đa khi xử lý blur (hạ scale về đây cho nhanh; ảnh nhỏ hơn giữ nguyên).
private const val PROC_MAX_WIDTH = 480
// Bán kính blur ≈ bề ngang mặt * hệ số (đo trên ảnh đã hạ scale).
private const val SMOOTH_RADIUS_FRAC = 0.06f
// Độ mềm mép mặt nạ (feather) = bề ngang mặt * hệ số.
private const val MASK_FEATHER_FRAC = 0.05f
// Range-guard: chênh lệch độ sáng (0..255) giữa gốc và bản mờ. <=LO: mịn hết; >=HI: giữ
// nguyên (cạnh sắc: cánh mũi, lông mày, nếp...); ở giữa: nội suy tuyến tính.
private const val EDGE_LO = 10
private const val EDGE_HI = 48

/**
 * Làm mịn da giữ cạnh (skin smoothing) từ 468 điểm Face Mesh.
 *
 * Thuật toán (xấp xỉ bilateral, tối ưu cho realtime):
 *  1. Lớp mịn = hạ scale → [boxBlur] tách trục 3 lượt (≈ Gaussian, O(W·H) bất kể bán
 *     kính) → upscale. Tránh O(r²) của bilateral thật.
 *  2. Mặt nạ da = đa giác FACE_OVAL, ĐỤC LỖ mắt + môi, mép feather bằng [BlurMaskFilter].
 *  3. Blend gốc↔mịn theo mặt nạ * [intensity], có RANGE-GUARD: nơi gốc và bản mờ lệch
 *     sáng nhiều (cạnh: cánh mũi, lông mày) thì giữ gốc → không nhoè nét. Đây là thành
 *     phần "range" của bilateral, áp ở bước blend nên chỉ O(W·H).
 *
 * Chỉ xử lý trong khung bao khuôn mặt nên thường <100ms khi kéo slider.
 * @param intensity 0..1. Trả về thẳng [srcBitmap] khi không cần / thiếu điểm / lỗi.
 * Thuần CPU — gọi trên Dispatchers.Default.
 */
fun applySkinSmoothing(
    srcBitmap: Bitmap,
    allPoints: List<FaceMeshPoint>,
    intensity: Float,
): Bitmap {
    val amount = intensity.coerceIn(0f, 1f)
    if (amount <= 0f || allPoints.size <= MAX_SKIN_ID) return srcBitmap

    return runCatching {
        val w = srcBitmap.width
        val h = srcBitmap.height

        // --- Khung bao khuôn mặt (chỉ xử lý vùng này) ---
        val oval = polyPath(allPoints, FACE_OVAL_IDS)
        val rb = RectF()
        oval.computeBounds(rb, true)
        val faceW = rb.width()
        if (faceW <= 0f) return@runCatching srcBitmap
        val feather = (faceW * MASK_FEATHER_FRAC).coerceAtLeast(2f)
        val bx = (rb.left - feather).toInt().coerceIn(0, w - 1)
        val by = (rb.top - feather).toInt().coerceIn(0, h - 1)
        val bw = ((rb.right + feather).toInt().coerceIn(bx + 1, w)) - bx
        val bh = ((rb.bottom + feather).toInt().coerceIn(by + 1, h)) - by

        // --- (1) Lớp mịn: hạ scale → box-blur → upscale ---
        val smoothed = buildSmoothedLayer(srcBitmap, faceW)

        // --- (2) Mặt nạ da (alpha = trọng số mịn 0..255) ---
        val mask = buildSkinMask(allPoints, w, h, oval, feather)

        // --- (3) Blend trong khung bao ---
        val area = bw * bh
        val srcBuf = IntArray(area).also { srcBitmap.getPixels(it, 0, bw, bx, by, bw, bh) }
        val smoBuf = IntArray(area).also { smoothed.getPixels(it, 0, bw, bx, by, bw, bh) }
        val mskBuf = IntArray(area).also { mask.getPixels(it, 0, bw, bx, by, bw, bh) }

        val amount256 = (amount * 256f).toInt()
        for (i in 0 until area) {
            val ma = (mskBuf[i] ushr 24) and 0xFF
            if (ma == 0) continue
            val s = srcBuf[i]
            val sm = smoBuf[i]
            val sr = (s ushr 16) and 0xFF
            val sg = (s ushr 8) and 0xFF
            val sb = s and 0xFF
            val mr = (sm ushr 16) and 0xFF
            val mg = (sm ushr 8) and 0xFF
            val mb = sm and 0xFF
            // Range-guard theo chênh lệch độ sáng (xấp xỉ luma: 0.30R 0.59G 0.11B).
            val diff = abs(((sr * 77 + sg * 150 + sb * 29) ushr 8) - ((mr * 77 + mg * 150 + mb * 29) ushr 8))
            if (diff >= EDGE_HI) continue                       // cạnh sắc → giữ nguyên gốc
            var wq = ma * amount256 / 255                        // 0..256
            if (diff > EDGE_LO) wq = wq * (EDGE_HI - diff) / (EDGE_HI - EDGE_LO)
            if (wq <= 0) continue
            // Nội suy gốc → mịn theo wq/256 trên từng kênh.
            val rr = sr + (((mr - sr) * wq) shr 8)
            val gg = sg + (((mg - sg) * wq) shr 8)
            val bb = sb + (((mb - sb) * wq) shr 8)
            srcBuf[i] = (s and 0xFF000000.toInt()) or (rr shl 16) or (gg shl 8) or bb
        }

        val out = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
        out.setPixels(srcBuf, 0, bw, bx, by, bw, bh)

        smoothed.recycle()
        mask.recycle()
        out
    }.getOrElse { srcBitmap }
}

/** Lớp mịn toàn ảnh: hạ scale về [PROC_MAX_WIDTH] → box-blur 3 lượt → upscale lại. */
private fun buildSmoothedLayer(src: Bitmap, faceWidth: Float): Bitmap {
    val scale = (PROC_MAX_WIDTH.toFloat() / src.width).coerceAtMost(1f)   // chỉ thu nhỏ
    val sw = (src.width * scale).toInt().coerceAtLeast(1)
    val sh = (src.height * scale).toInt().coerceAtLeast(1)
    val small = Bitmap.createScaledBitmap(src, sw, sh, true)
    val px = IntArray(sw * sh).also { small.getPixels(it, 0, sw, 0, 0, sw, sh) }

    val radius = (faceWidth * scale * SMOOTH_RADIUS_FRAC).toInt().coerceIn(1, 64)
    boxBlur(px, sw, sh, radius)

    small.setPixels(px, 0, sw, 0, 0, sw, sh)
    val up = Bitmap.createScaledBitmap(small, src.width, src.height, true)   // bilinear
    if (small != up) small.recycle()
    return up
}

/** Mặt nạ ARGB: oval mặt = alpha 255, đục mắt/môi = 0, mép feather (BlurMaskFilter). */
private fun buildSkinMask(pts: List<FaceMeshPoint>, w: Int, h: Int, oval: Path, feather: Float): Bitmap {
    val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(mask)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        maskFilter = BlurMaskFilter(feather, BlurMaskFilter.Blur.NORMAL)
    }
    canvas.drawPath(oval, fill)
    // Đục lỗ: vẽ CLEAR (mép cũng feather nhẹ để chuyển vùng mượt) lên mắt + môi.
    val clear = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        maskFilter = BlurMaskFilter(feather * 0.7f, BlurMaskFilter.Blur.NORMAL)
    }
    canvas.drawPath(polyPath(pts, LEFT_EYE_IDS), clear)
    canvas.drawPath(polyPath(pts, RIGHT_EYE_IDS), clear)
    canvas.drawPath(polyPath(pts, LIPS_OUTER_IDS), clear)
    return mask
}

/** Dựng [Path] đa giác kín từ danh sách ID điểm Face Mesh (đọc x,y, bỏ z). */
private fun polyPath(pts: List<FaceMeshPoint>, ids: IntArray): Path {
    val path = Path()
    val first = pts[ids[0]].position
    path.moveTo(first.x, first.y)
    for (k in 1 until ids.size) {
        val p = pts[ids[k]].position
        path.lineTo(p.x, p.y)
    }
    path.close()
    return path
}

/**
 * Box blur tách trục (ngang rồi dọc), [passes] lượt ≈ Gaussian. Dùng tổng trượt nên O(W·H)
 * KHÔNG phụ thuộc [radius]. Biên kẹp (clamp) — sai số mép không đáng kể với làm mịn da.
 */
private fun boxBlur(px: IntArray, w: Int, h: Int, radius: Int, passes: Int = 3) {
    if (radius < 1) return
    val tmp = IntArray(px.size)
    repeat(passes) {
        boxBlurAxis(px, tmp, w, h, radius, horizontal = true)
        boxBlurAxis(tmp, px, w, h, radius, horizontal = false)
    }
}

/** Một lượt blur theo trục. [horizontal]=true: trượt theo x; false: theo y (dùng stride). */
private fun boxBlurAxis(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int, horizontal: Boolean) {
    val window = radius * 2 + 1
    val lines = if (horizontal) h else w           // số dòng/cột
    val len = if (horizontal) w else h             // độ dài mỗi dòng/cột
    val step = if (horizontal) 1 else w            // bước nhảy giữa 2 phần tử liền kề
    for (line in 0 until lines) {
        val base = if (horizontal) line * w else line
        var sa = 0; var sr = 0; var sg = 0; var sb = 0
        // Khởi tạo cửa sổ [-radius, radius] với clamp ở biên.
        for (k in -radius..radius) {
            val p = src[base + k.coerceIn(0, len - 1) * step]
            sa += (p ushr 24) and 0xFF; sr += (p ushr 16) and 0xFF
            sg += (p ushr 8) and 0xFF; sb += p and 0xFF
        }
        for (j in 0 until len) {
            dst[base + j * step] =
                ((sa / window) shl 24) or ((sr / window) shl 16) or ((sg / window) shl 8) or (sb / window)
            val pOut = src[base + (j - radius).coerceIn(0, len - 1) * step]
            val pIn = src[base + (j + radius + 1).coerceIn(0, len - 1) * step]
            sa += ((pIn ushr 24) and 0xFF) - ((pOut ushr 24) and 0xFF)
            sr += ((pIn ushr 16) and 0xFF) - ((pOut ushr 16) and 0xFF)
            sg += ((pIn ushr 8) and 0xFF) - ((pOut ushr 8) and 0xFF)
            sb += (pIn and 0xFF) - (pOut and 0xFF)
        }
    }
}
