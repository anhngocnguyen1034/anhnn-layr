package com.example.anhnn_layr.utils

import android.graphics.Bitmap
import android.graphics.PointF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import kotlinx.coroutines.tasks.await

/**
 * Một mắt: tâm ([cx], [cy]) và bán kính, tính theo pixel của bitmap đầu vào. Dùng
 * float thuần (không [PointF]) để phần toán warp test được trên JVM.
 */
data class EyeAnchor(val cx: Float, val cy: Float, val radius: Float)

/**
 * Trục dọc giữa khuôn mặt: từ ([topX],[topY]) (sống mũi) xuống ([botX],[botY]) (cằm).
 * Dùng làm tâm hút khi bóp thon mặt (slim/V-line). Float thuần để test trên JVM.
 */
data class FaceAxis(val topX: Float, val topY: Float, val botX: Float, val botY: Float)

/**
 * Điểm mỏ neo trên khuôn mặt dùng cho các phép chỉnh mặt (warp/makeup).
 * [cheeks] là các mỏ neo viền má/hàm hai bên (mỗi cái mang bán kính ảnh hưởng), [faceAxis]
 * là trục giữa — cả hai phục vụ bóp thon mặt.
 */
data class FaceLandmarks(
    val eyes: List<EyeAnchor>,
    val cheeks: List<EyeAnchor> = emptyList(),
    val faceAxis: FaceAxis? = null,
    // 468 điểm gốc cho các xử lý cần toạ độ chi tiết (mịn da, tô son). Rỗng khi không dò được.
    val allPoints: List<FaceMeshPoint> = emptyList(),
)

// ID viền má/hàm theo chuẩn Canonical Face Mesh (lấy từ FACE_OVAL, nửa dưới khuôn mặt).
private val LEFT_CHEEK_IDS = intArrayOf(234, 93, 132, 58, 172, 136, 150)
private val RIGHT_CHEEK_IDS = intArrayOf(454, 323, 361, 288, 397, 365, 379)
// Trục giữa: sống mũi (168) → cằm (152).
private const val NOSE_BRIDGE_ID = 168
private const val CHIN_ID = 152
// Bán kính ảnh hưởng mỗi anchor má = bề ngang mặt * hệ số (đủ chồng lấn cho warp mượt).
private const val CHEEK_RADIUS_FRAC = 0.20f

/**
 * Dò khuôn mặt bằng ML Kit **Face Mesh** (468 điểm 3D) và trả về các điểm mỏ neo cần
 * cho chỉnh mặt. Trả về null nếu không tìm thấy mặt nào.
 *
 * Dùng [FaceMeshDetectorOptions.FACE_MESH] (mặc định) để có đủ 468 điểm + các nhóm
 * contour ([FaceMesh.getPoints]). Lưu ý của ML Kit: Face Mesh chỉ hoạt động tốt khi mặt
 * cách camera trong ~2m và không phù hợp ảnh nhiều mặt ở xa — ở đây ta lấy mặt lớn nhất.
 *
 * Toạ độ trả về nằm trong hệ pixel của [bitmap] truyền vào (rotation = 0); bỏ qua trục z
 * vì warp/makeup chỉ làm việc 2D.
 */
suspend fun detectFaceLandmarks(bitmap: Bitmap): FaceLandmarks? {
    val options = FaceMeshDetectorOptions.Builder()
        .setUseCase(FaceMeshDetectorOptions.FACE_MESH)
        .build()
    val detector = FaceMeshDetection.getClient(options)
    try {
        val image = InputImage.fromBitmap(bitmap, 0)
        val meshes = detector.process(image).await()
        // Lấy mặt lớn nhất (gần camera nhất) làm chủ thể.
        val mesh = meshes.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            ?: return null
        // Contour mắt từ Face Mesh chính xác hơn bbox detection: tâm + bán kính sát mắt thật.
        val eyes = listOfNotNull(
            eyeAnchorFrom(mesh.getPoints(FaceMesh.LEFT_EYE)),
            eyeAnchorFrom(mesh.getPoints(FaceMesh.RIGHT_EYE)),
        )
        // Má/hàm + trục giữa cho bóp thon mặt — đọc thẳng từ 468 điểm (allPoints[id]).
        val all = mesh.allPoints
        val maxCheekId = maxOf(LEFT_CHEEK_IDS.max(), RIGHT_CHEEK_IDS.max(), CHIN_ID, NOSE_BRIDGE_ID)
        val faceWidth = mesh.boundingBox.width().toFloat()
        val cheeks: List<EyeAnchor>
        val faceAxis: FaceAxis?
        if (all.size > maxCheekId && faceWidth > 0f) {
            val r = faceWidth * CHEEK_RADIUS_FRAC
            cheeks = (LEFT_CHEEK_IDS + RIGHT_CHEEK_IDS).map { id ->
                val p = all[id].position
                EyeAnchor(cx = p.x, cy = p.y, radius = r)
            }
            val top = all[NOSE_BRIDGE_ID].position
            val bot = all[CHIN_ID].position
            faceAxis = FaceAxis(top.x, top.y, bot.x, bot.y)
        } else {
            cheeks = emptyList()
            faceAxis = null
        }
        if (eyes.isEmpty() && cheeks.isEmpty() && all.isEmpty()) return null
        return FaceLandmarks(
            eyes = eyes,
            cheeks = cheeks,
            faceAxis = faceAxis,
            allPoints = all,
        )
    } finally {
        detector.close()
    }
}

/** Tâm = trung bình các điểm contour; bán kính = nửa cạnh lớn hơn của bbox contour. */
private fun eyeAnchorFrom(points: List<FaceMeshPoint>): EyeAnchor? {
    if (points.isEmpty()) return null
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    var sumX = 0f
    var sumY = 0f
    for (p in points) {
        val x = p.position.x
        val y = p.position.y
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
        sumX += x
        sumY += y
    }
    val radius = maxOf(maxX - minX, maxY - minY) / 2f
    if (radius <= 0f) return null
    return EyeAnchor(cx = sumX / points.size, cy = sumY / points.size, radius = radius)
}
