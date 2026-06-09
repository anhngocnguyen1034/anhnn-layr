package com.example.anhnn_layr.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * Test phần toán warp "mắt to" ([computeEyeEnlargeVerts]) — hàm thuần, chạy trên JVM.
 * Không kiểm bước drawBitmapMesh (cần Android framework).
 */
class FaceReshapeTest {

    private val W = 200
    private val H = 200
    private val MESH = 40                       // phải khớp hằng số trong FaceReshape
    private val cols = MESH + 1

    private fun oneEye(cx: Float, cy: Float, r: Float) =
        FaceLandmarks(listOf(EyeAnchor(cx = cx, cy = cy, radius = r)))

    /** Lấy đỉnh (col,row) từ mảng verts xen kẽ x,y. */
    private fun vert(verts: FloatArray, col: Int, row: Int): Pair<Float, Float> {
        val idx = (row * cols + col) * 2
        return verts[idx] to verts[idx + 1]
    }

    @Test
    fun `no-op khi strength 0`() {
        assertNull(computeEyeEnlargeVerts(W, H, oneEye(100f, 100f, 20f), 0f))
    }

    @Test
    fun `no-op khi khong co mat`() {
        assertNull(computeEyeEnlargeVerts(W, H, null, 1f))
        assertNull(computeEyeEnlargeVerts(W, H, FaceLandmarks(emptyList()), 1f))
    }

    @Test
    fun `kich thuoc mang verts dung`() {
        val verts = computeEyeEnlargeVerts(W, H, oneEye(100f, 100f, 20f), 1f)
        assertNotNull(verts)
        assertEquals(cols * cols * 2, verts!!.size)
    }

    @Test
    fun `dinh xa vung anh huong khong di chuyen`() {
        val verts = computeEyeEnlargeVerts(W, H, oneEye(100f, 100f, 20f), 1f)!!
        // Góc (0,0) cách xa tâm mắt (100,100) hơn bán kính ảnh hưởng (20*2.2=44).
        val (x, y) = vert(verts, 0, 0)
        assertEquals(0f, x, 1e-3f)
        assertEquals(0f, y, 1e-3f)
    }

    @Test
    fun `dinh trong vung bi day ra xa tam`() {
        val cx = 100f
        val cy = 100f
        val verts = computeEyeEnlargeVerts(W, H, oneEye(cx, cy, 20f), 1f)!!
        // Lưới đều 200/40 = 5px/ô → cột 22, hàng 20 = (110,100): nằm phải tâm, trong vùng.
        val (x, y) = vert(verts, 22, 20)
        val baseX = 110f
        val baseY = 100f
        // Phải bị đẩy RA XA tâm theo trục x (sang phải), y gần như giữ nguyên.
        assertTrue("phải dịch sang phải", x > baseX)
        assertEquals(baseY, y, 1e-2f)
        // Khoảng cách tới tâm sau warp lớn hơn trước.
        assertTrue(hypot(x - cx, y - cy) > hypot(baseX - cx, baseY - cy))
    }

    @Test
    fun `cuong do cao day manh hon cuong do thap`() {
        val eye = oneEye(100f, 100f, 20f)
        val weak = computeEyeEnlargeVerts(W, H, eye, 0.3f)!!
        val strong = computeEyeEnlargeVerts(W, H, eye, 1f)!!
        val (wx, _) = vert(weak, 22, 20)
        val (sx, _) = vert(strong, 22, 20)
        assertTrue("cường độ cao đẩy xa hơn", sx > wx)
    }

    @Test
    fun `dinh ngay tam gan nhu khong dich`() {
        // Đỉnh trùng tâm: offset = 0 → displacement = 0 dù falloff cực đại.
        val verts = computeEyeEnlargeVerts(W, H, oneEye(100f, 100f, 20f), 1f)!!
        val (x, y) = vert(verts, 20, 20) // (100,100) = tâm
        assertEquals(100f, x, 1e-3f)
        assertEquals(100f, y, 1e-3f)
    }
}
