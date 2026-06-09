package com.example.anhnn_layr.utils

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Region
import android.os.Build
import com.google.mlkit.vision.facemesh.FaceMeshPoint

/** Màu môi hồng mặc định. */
const val LIP_PINK: Int = 0xFFF0567A.toInt()

// Độ phủ tối đa của lớp màu khi cường độ = 1 (tô film, không phủ đặc).
private const val MAX_LIP_ALPHA = 0.6f

/**
 * Tô màu môi (makeup). Vẽ đa giác viền môi [outline] bằng [color] với:
 * - [PorterDuff.Mode.SRC_ATOP]: chỉ ăn màu lên pixel ĐÃ CÓ của chủ thể (giữ nguyên
 *   alpha, không lem ra vùng trong suốt ngoài mặt).
 * - [BlurMaskFilter]: mép môi mềm, hoà tự nhiên.
 * [intensity] (0..1) điều chỉnh độ đậm. Trả về thẳng [src] khi không cần làm gì.
 * Hàm thuần CPU — gọi trên Dispatchers.Default.
 */
fun applyLipColor(src: Bitmap, outline: List<FacePoint>?, color: Int, intensity: Float): Bitmap {
    if (intensity <= 0f || outline == null || outline.size < 3) return src

    val path = Path().apply {
        moveTo(outline[0].x, outline[0].y)
        for (k in 1 until outline.size) lineTo(outline[k].x, outline[k].y)
        close()
    }

    var minY = Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    for (p in outline) {
        if (p.y < minY) minY = p.y
        if (p.y > maxY) maxY = p.y
    }
    val lipHeight = (maxY - minY).coerceAtLeast(1f)

    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val paint = Paint().apply {
        isAntiAlias = true
        this.color = color
        // Đặt alpha SAU color (giữ RGB, chỉ đổi byte alpha).
        alpha = (intensity * MAX_LIP_ALPHA * 255f).toInt().coerceIn(0, 255)
        maskFilter = BlurMaskFilter((lipHeight * 0.12f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }
    Canvas(out).drawPath(path, paint)
    return out
}

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
