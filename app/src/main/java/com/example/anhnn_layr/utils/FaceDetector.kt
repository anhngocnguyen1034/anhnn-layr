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

/** Điểm 2D thuần (không [PointF]) — dùng cho đường viền môi khi tô màu. */
data class FacePoint(val x: Float, val y: Float)

/**
 * Điểm mỏ neo trên khuôn mặt dùng cho các phép chỉnh mặt (warp/makeup).
 * [lipOutline] là đa giác viền NGOÀI của môi (rỗng nếu không dò được).
 */
data class FaceLandmarks(
    val eyes: List<EyeAnchor>,
    val lipOutline: List<FacePoint> = emptyList(),
)

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
        // Viền ngoài môi = mép trên môi-trên nối với mép dưới môi-dưới (đảo chiều) để
        // tạo đa giác kín bao cả miệng.
        val upperTop = mesh.getPoints(FaceMesh.UPPER_LIP_TOP)
        val lowerBottom = mesh.getPoints(FaceMesh.LOWER_LIP_BOTTOM)
        val lipOutline = if (upperTop.isNotEmpty() && lowerBottom.isNotEmpty()) {
            (upperTop + lowerBottom.reversed()).map { FacePoint(it.position.x, it.position.y) }
        } else emptyList()
        if (eyes.isEmpty() && lipOutline.isEmpty()) return null
        return FaceLandmarks(eyes = eyes, lipOutline = lipOutline)
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
