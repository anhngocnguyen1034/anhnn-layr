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
// Vành mắt trái/phải + lông mày + viền ngoài môi — ĐỤC LỖ khỏi mặt nạ để giữ nét sắc.
private val LEFT_EYE_IDS = intArrayOf(33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246)
private val RIGHT_EYE_IDS = intArrayOf(263, 249, 390, 373, 374, 380, 381, 382, 362, 398, 384, 385, 386, 387, 388, 466)
// Lông mày: vòng kín = mép TRÊN (ngoài→trong) nối mép DƯỚI (trong→ngoài).
private val LEFT_BROW_IDS = intArrayOf(70, 63, 105, 66, 107, 55, 65, 52, 53, 46)
private val RIGHT_BROW_IDS = intArrayOf(300, 293, 334, 296, 336, 285, 295, 282, 283, 276)
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
 *  2. Mặt nạ da = đa giác FACE_OVAL, ĐỤC LỖ mắt + lông mày + môi, mép feather bằng
 *     [BlurMaskFilter].
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
        // --- Khung bao khuôn mặt + mặt nạ da (helper dùng chung với sáng da) ---
        val region = buildFaceRegion(srcBitmap, allPoints) ?: return@runCatching srcBitmap
        val bx = region.bx
        val by = region.by
        val bw = region.bw
        val bh = region.bh

        // --- (1) Lớp mịn: hạ scale → box-blur → upscale ---
        val smoothed = buildSmoothedLayer(srcBitmap, region.faceWidth)

        // --- (2) + (3) Blend trong khung bao theo mặt nạ ---
        val mask = region.mask
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

// Mức nâng sáng tối đa khi cường độ = 1 (screen-blend về phía trắng: pixel càng sáng
// càng được cộng ít nên highlight không bị cháy).
private const val BRIGHTEN_LIFT_MAX = 0.30f

// Cổng màu da theo sắc độ YCbCr (dải kinh điển cho da người, mọi tông sáng/tối):
// chỉ pixel có Cb/Cr trong dải mới được nâng sáng → tóc xoã trên trán, gọng kính,
// nền lọt vào oval mặt KHÔNG bị sáng theo. SOFT = bề rộng mép chuyển tiếp mềm.
private const val SKIN_CB_LO = 77
private const val SKIN_CB_HI = 127
private const val SKIN_CR_LO = 133
private const val SKIN_CR_HI = 173
private const val SKIN_SOFT = 10

/**
 * Sáng da / trắng da từ 468 điểm Face Mesh. Dùng CHUNG mặt nạ da với [applySkinSmoothing]
 * (oval mặt, đục mắt + lông mày + môi, mép feather) nhưng thay vì blend lớp mờ thì nâng
 * sáng kiểu screen: c' = c + (255 − c) · lift — da sáng đều, highlight không cháy, mắt/
 * mày/môi giữ nguyên độ tương phản.
 *
 * Mặt nạ oval chỉ là vùng GIỚI HẠN; trọng số thật của từng pixel còn nhân thêm cổng
 * màu da (Cb/Cr) — oval hình học không trùng khít viền mặt thật (tóc, kính, nền sát
 * viền) nên nếu nâng cả oval thì vùng sáng nhìn như lệch khỏi khuôn mặt.
 * @param intensity 0..1. Trả về thẳng [srcBitmap] khi không cần / thiếu điểm / lỗi.
 * Thuần CPU — gọi trên Dispatchers.Default.
 */
fun applySkinBrighten(
    srcBitmap: Bitmap,
    allPoints: List<FaceMeshPoint>,
    intensity: Float,
): Bitmap {
    val amount = intensity.coerceIn(0f, 1f)
    if (amount <= 0f || allPoints.size <= MAX_SKIN_ID) return srcBitmap

    return runCatching {
        val region = buildFaceRegion(srcBitmap, allPoints) ?: return@runCatching srcBitmap
        val bx = region.bx
        val by = region.by
        val bw = region.bw
        val bh = region.bh

        val area = bw * bh
        val srcBuf = IntArray(area).also { srcBitmap.getPixels(it, 0, bw, bx, by, bw, bh) }
        val mskBuf = IntArray(area).also { region.mask.getPixels(it, 0, bw, bx, by, bw, bh) }

        // lift256 = trọng số nâng sáng 0..256, nhân sẵn cường độ để vòng lặp chỉ còn
        // số nguyên (như applySkinSmoothing).
        val amount256 = (amount * BRIGHTEN_LIFT_MAX * 256f).toInt()
        for (i in 0 until area) {
            val ma = (mskBuf[i] ushr 24) and 0xFF
            if (ma == 0) continue
            val p = srcBuf[i]
            // Pixel trong suốt (nền đã tách) — nâng sáng cũng vô hình, bỏ qua cho nhanh.
            if ((p ushr 24) and 0xFF == 0) continue
            val r = (p ushr 16) and 0xFF
            val g = (p ushr 8) and 0xFF
            val b = p and 0xFF
            // Cổng màu da: Cb/Cr ngoài dải da → trọng số 0 (tóc, kính, nền trong oval).
            val cb = 128 + ((-38 * r - 74 * g + 112 * b) shr 8)
            val cr = 128 + ((112 * r - 94 * g - 18 * b) shr 8)
            val skin256 = softRange256(cb, SKIN_CB_LO, SKIN_CB_HI) *
                softRange256(cr, SKIN_CR_LO, SKIN_CR_HI) shr 8
            if (skin256 <= 0) continue
            val lift256 = ma * amount256 / 255 * skin256 shr 8
            if (lift256 <= 0) continue
            val rr = r + (((255 - r) * lift256) shr 8)
            val gg = g + (((255 - g) * lift256) shr 8)
            val bb = b + (((255 - b) * lift256) shr 8)
            srcBuf[i] = (p and 0xFF000000.toInt()) or (rr shl 16) or (gg shl 8) or bb
        }

        val out = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
        out.setPixels(srcBuf, 0, bw, bx, by, bw, bh)
        region.mask.recycle()
        out
    }.getOrElse { srcBitmap }
}

/**
 * Trọng số mềm 0..256 cho giá trị [v] so với khoảng [lo, hi]: trong khoảng = 256,
 * ngoài khoảng quá [SKIN_SOFT] = 0, mép chuyển tiếp nội suy tuyến tính — tránh viền
 * cứng giữa vùng được nâng sáng và không.
 */
private fun softRange256(v: Int, lo: Int, hi: Int): Int = when {
    v < lo - SKIN_SOFT || v > hi + SKIN_SOFT -> 0
    v < lo -> (v - (lo - SKIN_SOFT)) * 256 / SKIN_SOFT
    v > hi -> ((hi + SKIN_SOFT) - v) * 256 / SKIN_SOFT
    else -> 256
}

/** Khung bao khuôn mặt + mặt nạ da — phần dùng chung của mịn da và sáng da. */
private class FaceRegion(
    val bx: Int,
    val by: Int,
    val bw: Int,
    val bh: Int,
    val faceWidth: Float,
    val mask: Bitmap,
)

/**
 * Dựng [FaceRegion] từ 468 điểm: oval mặt → khung bao (nở thêm feather, kẹp trong ảnh)
 * + mặt nạ da ([buildSkinMask]). Trả về null khi không dựng được (mặt suy biến).
 */
private fun buildFaceRegion(src: Bitmap, pts: List<FaceMeshPoint>): FaceRegion? {
    val w = src.width
    val h = src.height
    val oval = polyPath(pts, FACE_OVAL_IDS)
    val rb = RectF()
    oval.computeBounds(rb, true)
    val faceW = rb.width()
    if (faceW <= 0f) return null
    val feather = (faceW * MASK_FEATHER_FRAC).coerceAtLeast(2f)
    val bx = (rb.left - feather).toInt().coerceIn(0, w - 1)
    val by = (rb.top - feather).toInt().coerceIn(0, h - 1)
    val bw = ((rb.right + feather).toInt().coerceIn(bx + 1, w)) - bx
    val bh = ((rb.bottom + feather).toInt().coerceIn(by + 1, h)) - by
    return FaceRegion(
        bx = bx,
        by = by,
        bw = bw,
        bh = bh,
        faceWidth = faceW,
        mask = buildSkinMask(pts, w, h, oval, feather),
    )
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

/** Mặt nạ ARGB: oval mặt = alpha 255, đục mắt/lông mày/môi = 0, mép feather (BlurMaskFilter). */
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
    canvas.drawPath(polyPath(pts, LEFT_BROW_IDS), clear)
    canvas.drawPath(polyPath(pts, RIGHT_BROW_IDS), clear)
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
