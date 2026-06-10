package com.example.anhnn_layr.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

// Số ô lưới mỗi chiều cho drawBitmapMesh. Lưới càng dày warp càng mượt nhưng tốn hơn.
private const val MESH = 40

// Bán kính ảnh hưởng = bán kính mắt * hệ số (vùng warp lan ra quanh mắt).
private const val INFLUENCE_FACTOR = 2.2f

// Hệ số zoom tối đa ở tâm khi strength = 1 (1 + 0.6 = phóng to 1.6 lần).
private const val MAX_ZOOM = 0.6f

// Tỉ lệ kéo viền má vào trục tối đa khi strength = 1 (0.18 = thu ~18% khoảng cách tới trục).
private const val SLIM_MAX = 0.18f

/**
 * Phóng to mắt bằng phép warp lưới ([Canvas.drawBitmapMesh]). Mỗi mắt tạo một "zoom
 * cục bộ": các đỉnh lưới quanh tâm mắt bị đẩy ra xa tâm theo độ mạnh giảm dần
 * (smoothstep) tới rìa vùng ảnh hưởng, nên phần còn lại của mặt không méo.
 *
 * Trả về thẳng [src] khi không cần làm gì (strength<=0, không có mặt) để tránh copy.
 * Hàm thuần CPU — gọi trên Dispatchers.Default.
 */
fun applyEyeEnlarge(src: Bitmap, landmarks: FaceLandmarks?, strength: Float): Bitmap {
    val verts = computeEyeEnlargeVerts(src.width, src.height, landmarks, strength) ?: return src
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    Canvas(out).drawBitmapMesh(src, MESH, MESH, verts, 0, null, 0, paint)
    return out
}

/**
 * Tính mảng đỉnh lưới đã dịch chuyển cho [Canvas.drawBitmapMesh] (kích thước
 * (MESH+1)² × 2, xen kẽ x,y). Hàm THUẦN (không Android) để test được trên JVM.
 *
 * Trả về null khi không cần warp (strength<=0, không có mặt) → caller dùng ảnh gốc.
 */
fun computeEyeEnlargeVerts(
    width: Int,
    height: Int,
    landmarks: FaceLandmarks?,
    strength: Float,
): FloatArray? {
    if (strength <= 0f || landmarks == null || landmarks.eyes.isEmpty()) return null

    val cols = MESH + 1
    val rows = MESH + 1
    val verts = FloatArray(cols * rows * 2)

    var i = 0
    for (row in 0 until rows) {
        val baseY = row.toFloat() / MESH * height
        for (col in 0 until cols) {
            val baseX = col.toFloat() / MESH * width
            var dx = 0f
            var dy = 0f
            // Cộng dồn dịch chuyển của từng mắt (các mắt cách xa nhau nên ít chồng lấn).
            for (eye in landmarks.eyes) {
                val influence = eye.radius * INFLUENCE_FACTOR
                if (influence <= 0f) continue
                val ox = baseX - eye.cx
                val oy = baseY - eye.cy
                val dist = kotlin.math.hypot(ox, oy)
                if (dist >= influence) continue
                val t = 1f - dist / influence            // 1 ở tâm → 0 ở rìa
                val falloff = t * t * (3f - 2f * t)       // smoothstep
                val factor = strength * MAX_ZOOM * falloff
                dx += ox * factor
                dy += oy * factor
            }
            verts[i] = baseX + dx
            verts[i + 1] = baseY + dy
            i += 2
        }
    }
    return verts
}

/**
 * Bóp thon gọn mặt (slim / V-line) bằng warp lưới ([Canvas.drawBitmapMesh]). Đối xứng
 * với [applyEyeEnlarge] nhưng lực là HÚT VÀO trục giữa thay vì đẩy ra.
 * Trả về thẳng [src] khi không cần làm gì. Thuần CPU — gọi trên Dispatchers.Default.
 */
fun applyFaceSlim(src: Bitmap, landmarks: FaceLandmarks?, strength: Float): Bitmap {
    val verts = computeFaceSlimVerts(src.width, src.height, landmarks, strength) ?: return src
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    Canvas(out).drawBitmapMesh(src, MESH, MESH, verts, 0, null, 0, paint)
    return out
}

/**
 * Tính đỉnh lưới đã dịch cho hiệu ứng bóp thon mặt. Mỗi đỉnh trong vùng ảnh hưởng của
 * anchor má/hàm bị KÉO NGANG về trục giữa ([FaceLandmarks.faceAxis]); dịch chuyển càng
 * lớn khi càng gần anchor (smoothstep theo bán kính) và giảm về 0 khi:
 *  - tới rìa vùng ảnh hưởng  → hạn chế cong vẹo phông nền phía sau,
 *  - tới gần trục giữa (miệng/mũi/cằm) → không méo khuôn miệng (centerGuard).
 *
 * Chỉ dịch theo trục X (giữ nguyên chiều cao mặt). Hàm THUẦN (không Android) để test JVM.
 * Trả về null khi không cần warp (strength<=0, không có má/trục) → caller dùng ảnh gốc.
 */
fun computeFaceSlimVerts(
    width: Int,
    height: Int,
    landmarks: FaceLandmarks?,
    strength: Float,
): FloatArray? {
    val axis = landmarks?.faceAxis
    if (strength <= 0f || axis == null || landmarks.cheeks.isEmpty()) return null

    val cols = MESH + 1
    val rows = MESH + 1
    val verts = FloatArray(cols * rows * 2)
    val cheeks = landmarks.cheeks

    // Nội suy axisX theo y (trục gần dọc) — tính trước nghịch đảo để khỏi chia trong vòng.
    val axisDX = axis.botX - axis.topX
    val axisDY = axis.botY - axis.topY
    val invAxisDY = if (axisDY != 0f) 1f / axisDY else 0f

    // Hộp bao toàn bộ vùng ảnh hưởng → bỏ qua đỉnh nền ở xa cho nhanh.
    var bbL = Float.MAX_VALUE
    var bbT = Float.MAX_VALUE
    var bbR = -Float.MAX_VALUE
    var bbB = -Float.MAX_VALUE
    for (a in cheeks) {
        if (a.cx - a.radius < bbL) bbL = a.cx - a.radius
        if (a.cx + a.radius > bbR) bbR = a.cx + a.radius
        if (a.cy - a.radius < bbT) bbT = a.cy - a.radius
        if (a.cy + a.radius > bbB) bbB = a.cy + a.radius
    }

    var i = 0
    for (row in 0 until rows) {
        val baseY = row.toFloat() / MESH * height
        // axisX tại y này (kẹp trong đoạn mũi→cằm) — chỉ phụ thuộc y nên tính 1 lần/hàng.
        val ty = ((baseY - axis.topY) * invAxisDY).coerceIn(0f, 1f)
        val axisX = axis.topX + axisDX * ty
        val rowOutside = baseY < bbT || baseY > bbB
        for (col in 0 until cols) {
            val baseX = col.toFloat() / MESH * width
            var dx = 0f
            if (!rowOutside && baseX >= bbL && baseX <= bbR) {
                val toAxis = axisX - baseX              // hướng (có dấu) từ đỉnh vào trục
                for (a in cheeks) {
                    val infl = a.radius
                    if (infl <= 0f) continue
                    val ox = baseX - a.cx
                    val oy = baseY - a.cy
                    val dist2 = ox * ox + oy * oy
                    if (dist2 >= infl * infl) continue
                    val dist = kotlin.math.sqrt(dist2)
                    val r = 1f - dist / infl
                    val radial = r * r * (3f - 2f * r)  // smoothstep theo bán kính
                    // centerGuard: chuẩn hoá theo offset của anchor → 0 ở trục, 1 ở viền.
                    val anchorOffset = axisX - a.cx
                    if (anchorOffset == 0f) continue
                    val q = (toAxis / anchorOffset).coerceIn(0f, 1f)
                    val centerGuard = q * q * (3f - 2f * q)
                    dx += toAxis * (strength * SLIM_MAX * radial * centerGuard)
                }
            }
            verts[i] = baseX + dx
            verts[i + 1] = baseY                        // chỉ kéo ngang, giữ chiều cao mặt
            i += 2
        }
    }
    return verts
}
