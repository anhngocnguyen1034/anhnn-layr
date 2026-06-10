package com.example.anhnn_layr.utils

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Region
import android.graphics.Shader
import android.os.Build
import com.google.mlkit.vision.facemesh.FaceMeshPoint

/** Màu môi hồng mặc định. */
const val LIP_PINK: Int = 0xFFF0567A.toInt()

/**
 * Bảng màu son cho tab "Mặt" (ARGB). [LIP_PINK] đứng đầu làm mặc định; các màu còn lại
 * theo tông son thật phổ biến (đỏ cổ điển, cam đào, hồng đất, mận, đỏ rượu, nude...).
 */
val LIP_PALETTE: List<Int> = listOf(
    LIP_PINK,                 // Hồng
    0xFFD32F2F.toInt(),       // Đỏ cổ điển
    0xFFB7472A.toInt(),       // Đỏ gạch
    0xFFFF7043.toInt(),       // Cam đào
    0xFFC97B72.toInt(),       // Hồng đất (MLBB)
    0xFF8E3B59.toInt(),       // Mận
    0xFF7B1F2B.toInt(),       // Đỏ rượu
    0xFFC8917F.toInt(),       // Nude
)

// Độ phủ tối đa khi cường độ = 1 — chừa lại chút môi gốc cho tự nhiên, không phủ đặc.
private const val MAX_LIPSTICK_ALPHA = 0.9f

/**
 * Bản tiện dụng của [applyLipstick] nhận cường độ 0..1 (giá trị slider) thay vì alpha
 * 0..255 — quy đổi qua [MAX_LIPSTICK_ALPHA] để cường độ tối đa vẫn giữ nét môi gốc.
 */
fun applyLipstick(
    srcBitmap: Bitmap,
    allPoints: List<FaceMeshPoint>,
    lipstickColor: Int,
    intensity: Float,
): Bitmap = applyLipstick(
    srcBitmap = srcBitmap,
    allPoints = allPoints,
    lipstickColor = lipstickColor,
    opacity = (intensity.coerceIn(0f, 1f) * MAX_LIPSTICK_ALPHA * 255f).toInt(),
)

// ID điểm mốc môi theo chuẩn Canonical Face Mesh (MediaPipe/ML Kit, index 0..467).
// Mỗi mảng là một vòng KÍN đi theo thứ tự: nửa môi trên (trái→phải) rồi nửa môi dưới
// (phải→trái), nên nối thẳng các điểm là ra đa giác viền đóng.
private val OUTER_LIP_IDS = intArrayOf(
    61, 185, 40, 39, 37, 0, 267, 269, 270, 409, 291,   // viền ngoài môi TRÊN
    375, 321, 405, 314, 17, 84, 181, 91, 146,          // viền ngoài môi DƯỚI
)
private val INNER_LIP_IDS = intArrayOf(
    78, 191, 80, 81, 82, 13, 312, 311, 310, 415, 308,  // viền trong môi TRÊN
    324, 318, 402, 317, 14, 87, 178, 88, 95,           // viền trong môi DƯỚI
)
// Index lớn nhất được dùng ở trên — cần allPoints chứa tới điểm này.
private const val MAX_LIP_ID = 415

// Bán kính blur viền ngoài = chiều cao môi * hệ số (mép son mềm, không gắt).
private const val LIP_BLUR_FACTOR = 0.06f

/**
 * Đổi màu son môi tự nhiên từ 468 điểm Face Mesh.
 *
 * - Dựng 2 đa giác: viền NGOÀI ([OUTER_LIP_IDS]) và viền TRONG ([INNER_LIP_IDS]).
 * - Đục lỗ vùng môi trong: [Canvas.clipOutPath] (API26+) / [Region.Op.DIFFERENCE] cấm
 *   vẽ vào trong miệng → khi cười KHÔNG bị tô lên răng. Đây là biên CỨNG (chính xác).
 * - [BlurMaskFilter] làm MỀM riêng mép ngoài (soft edge), hoà vào da.
 * - Trộn màu bằng [BlendMode.COLOR] (API29+) — giữ độ sáng/nếp nhăn/độ bóng của môi gốc,
 *   chỉ thay sắc; máy cũ fallback [PorterDuff.Mode.MULTIPLY]. KHÔNG tô màu phẳng.
 *
 * @param opacity 0..255 — độ đậm son (alpha của paint, đồng thời là cường độ blend).
 * Trả về thẳng [srcBitmap] khi không cần làm / thiếu điểm / có lỗi. Thuần CPU → Dispatchers.Default.
 */
fun applyLipstick(
    srcBitmap: Bitmap,
    allPoints: List<FaceMeshPoint>,
    lipstickColor: Int,
    opacity: Int,
): Bitmap {
    val alpha = opacity.coerceIn(0, 255)
    if (alpha == 0 || allPoints.size <= MAX_LIP_ID) return srcBitmap

    return runCatching {
        val outer = lipPath(allPoints, OUTER_LIP_IDS)
        val inner = lipPath(allPoints, INNER_LIP_IDS)

        // Bán kính blur theo kích thước môi thực tế (ảnh to/nhỏ đều mềm cân đối).
        val bounds = RectF()
        outer.computeBounds(bounds, true)
        val blur = (bounds.height() * LIP_BLUR_FACTOR).coerceAtLeast(1f)

        val out = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)

        // Cấm vẽ vào vùng môi trong (biên cứng) → bảo vệ răng tuyệt đối.
        canvas.save()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.clipOutPath(inner)
        } else {
            @Suppress("DEPRECATION")
            canvas.clipPath(inner, Region.Op.DIFFERENCE)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lipstickColor
            this.alpha = alpha                       // điều khiển độ đậm son
            maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
            // Vẽ TRỰC TIẾP lên pixel môi gốc để blend đọc được texture bên dưới.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                blendMode = BlendMode.COLOR          // giữ luma gốc, đổi sắc → tự nhiên
            } else {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            }
        }
        // Tô cả vùng môi ngoài; clip ở trên đã trừ phần môi trong → còn lại "vành son".
        canvas.drawPath(outer, paint)
        canvas.restore()
        out
    }.getOrElse { srcBitmap }
}

// --- Má hồng ---

/** Màu má hồng tự nhiên. */
const val BLUSH_PINK: Int = 0xFFE8707E.toInt()

// Độ phủ tối đa ở tâm má khi cường độ = 1 (gradient tản dần ra rìa nên nhìn mỏng hơn).
private const val MAX_BLUSH_ALPHA = 0.35f
// Tâm má hai bên theo chuẩn Canonical Face Mesh (vùng "táo má" dưới gò má).
private const val LEFT_CHEEK_APPLE_ID = 425
private const val RIGHT_CHEEK_APPLE_ID = 205
// Bán kính vùng má = khoảng cách giữa 2 tâm má * hệ số (tự cân theo cỡ mặt/góc nghiêng).
private const val BLUSH_RADIUS_FRAC = 0.30f

/**
 * Đánh má hồng từ 468 điểm Face Mesh: tại tâm má mỗi bên vẽ một [RadialGradient]
 * [color] → trong suốt (đậm ở tâm, tản mượt ra rìa như cọ phấn), [PorterDuff.Mode.SRC_ATOP]
 * để chỉ ăn lên pixel có sẵn của chủ thể (không lem ra nền trong suốt).
 * @param intensity 0..1. Trả về thẳng [srcBitmap] khi không cần / thiếu điểm / lỗi.
 * Thuần CPU — gọi trên Dispatchers.Default.
 */
fun applyBlush(
    srcBitmap: Bitmap,
    allPoints: List<FaceMeshPoint>,
    color: Int,
    intensity: Float,
): Bitmap {
    val amount = intensity.coerceIn(0f, 1f)
    val maxId = maxOf(LEFT_CHEEK_APPLE_ID, RIGHT_CHEEK_APPLE_ID)
    if (amount <= 0f || allPoints.size <= maxId) return srcBitmap

    return runCatching {
        val left = allPoints[LEFT_CHEEK_APPLE_ID].position
        val right = allPoints[RIGHT_CHEEK_APPLE_ID].position
        val span = kotlin.math.hypot(left.x - right.x, left.y - right.y)
        val radius = span * BLUSH_RADIUS_FRAC
        if (radius < 2f) return@runCatching srcBitmap

        val centerAlpha = (amount * MAX_BLUSH_ALPHA * 255f).toInt().coerceIn(0, 255)
        val centerColor = (centerAlpha shl 24) or (color and 0x00FFFFFF)
        val edgeColor = color and 0x00FFFFFF

        val out = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        for (p in listOf(left, right)) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    p.x, p.y, radius,
                    centerColor, edgeColor,
                    Shader.TileMode.CLAMP,
                )
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            }
            canvas.drawCircle(p.x, p.y, radius, paint)
        }
        out
    }.getOrElse { srcBitmap }
}

// --- Răng trắng ---

// Guard theo độ sáng (luma 0..255): dưới LO (khoang miệng tối, lưỡi, kẽ răng) không xử
// lý; trên HI (răng sáng) xử lý hết; ở giữa nội suy tuyến tính → miệng há không bị xám.
private const val TEETH_LUMA_LO = 70
private const val TEETH_LUMA_HI = 135
// Mức khử màu (bỏ ám vàng, kéo về xám trung tính) và nâng sáng tối đa khi cường độ = 1.
private const val TEETH_DESAT_MAX = 0.7f
private const val TEETH_LIFT_MAX = 0.22f
// Mép mặt nạ mềm = chiều cao vùng miệng * hệ số.
private const val TEETH_FEATHER_FRAC = 0.15f

/**
 * Làm trắng răng từ 468 điểm Face Mesh. Vùng trong miệng (đa giác [INNER_LIP_IDS] —
 * cùng đa giác mà son môi đục lỗ bảo vệ) được khử bão hoà + nâng sáng:
 * - Mặt nạ mép mềm ([BlurMaskFilter]) như son, chỉ tính trong khung bao vùng miệng.
 * - Guard độ sáng: chỉ ăn vào pixel sáng (răng), bỏ qua khoang miệng tối/lưỡi.
 * - Miệng ngậm (đa giác gần suy biến) → trả về ảnh gốc, không lem lên vành môi.
 * @param intensity 0..1. Trả về thẳng [srcBitmap] khi không cần / thiếu điểm / lỗi.
 * Thuần CPU — gọi trên Dispatchers.Default.
 */
fun applyTeethWhiten(
    srcBitmap: Bitmap,
    allPoints: List<FaceMeshPoint>,
    intensity: Float,
): Bitmap {
    val amount = intensity.coerceIn(0f, 1f)
    if (amount <= 0f || allPoints.size <= MAX_LIP_ID) return srcBitmap

    return runCatching {
        val inner = lipPath(allPoints, INNER_LIP_IDS)
        val rb = RectF()
        inner.computeBounds(rb, true)
        // Miệng ngậm: vùng trong miệng chỉ là lằn mỏng → không có răng để trắng.
        if (rb.height() < 4f || rb.width() < 4f) return@runCatching srcBitmap

        val w = srcBitmap.width
        val h = srcBitmap.height
        val feather = (rb.height() * TEETH_FEATHER_FRAC).coerceAtLeast(1f)
        val bx = (rb.left - feather).toInt().coerceIn(0, w - 1)
        val by = (rb.top - feather).toInt().coerceIn(0, h - 1)
        val bw = ((rb.right + feather).toInt().coerceIn(bx + 1, w)) - bx
        val bh = ((rb.bottom + feather).toInt().coerceIn(by + 1, h)) - by

        // Mặt nạ chỉ bằng khung bao vùng miệng (dịch canvas về gốc khung) cho nhẹ RAM.
        val mask = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        Canvas(mask).apply {
            translate(-bx.toFloat(), -by.toFloat())
            drawPath(
                inner,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    maskFilter = BlurMaskFilter(feather, BlurMaskFilter.Blur.NORMAL)
                },
            )
        }

        val area = bw * bh
        val srcBuf = IntArray(area).also { srcBitmap.getPixels(it, 0, bw, bx, by, bw, bh) }
        val mskBuf = IntArray(area).also { mask.getPixels(it, 0, bw, 0, 0, bw, bh) }

        for (i in 0 until area) {
            val ma = (mskBuf[i] ushr 24) and 0xFF
            if (ma == 0) continue
            val p = srcBuf[i]
            val r = (p ushr 16) and 0xFF
            val g = (p ushr 8) and 0xFF
            val b = p and 0xFF
            val luma = (r * 77 + g * 150 + b * 29) ushr 8
            if (luma <= TEETH_LUMA_LO) continue
            val bright = if (luma >= TEETH_LUMA_HI) 1f
            else (luma - TEETH_LUMA_LO).toFloat() / (TEETH_LUMA_HI - TEETH_LUMA_LO)
            val wq = (ma / 255f) * amount * bright
            val desat = TEETH_DESAT_MAX * wq
            val lift = TEETH_LIFT_MAX * wq
            // Khử vàng: kéo từng kênh về luma; nâng sáng: cộng phần còn thiếu tới trắng.
            val rr = whitenChannel(r, luma, desat, lift)
            val gg = whitenChannel(g, luma, desat, lift)
            val bb = whitenChannel(b, luma, desat, lift)
            srcBuf[i] = (p and 0xFF000000.toInt()) or (rr shl 16) or (gg shl 8) or bb
        }

        val out = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
        out.setPixels(srcBuf, 0, bw, bx, by, bw, bh)
        mask.recycle()
        out
    }.getOrElse { srcBitmap }
}

/** Một kênh màu: desaturate về [luma] rồi nâng sáng về phía 255, kẹp 0..255. */
private fun whitenChannel(c: Int, luma: Int, desat: Float, lift: Float): Int {
    val d = c + ((luma - c) * desat)
    return (d + (255f - d) * lift).toInt().coerceIn(0, 255)
}

/** Dựng [Path] kín từ danh sách ID điểm Face Mesh (đọc x,y, bỏ z). */
private fun lipPath(allPoints: List<FaceMeshPoint>, ids: IntArray): Path {
    val path = Path()
    val first = allPoints[ids[0]].position
    path.moveTo(first.x, first.y)
    for (k in 1 until ids.size) {
        val p = allPoints[ids[k]].position
        path.lineTo(p.x, p.y)
    }
    path.close()
    return path
}
