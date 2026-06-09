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
