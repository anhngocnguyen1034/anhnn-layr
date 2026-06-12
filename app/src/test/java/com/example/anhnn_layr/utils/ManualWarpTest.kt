package com.example.anhnn_layr.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test phần toán nắn tay ([computeManualWarpVerts]) — hàm thuần, chạy trên JVM.
 * Không kiểm bước drawBitmapMesh (cần Android framework).
 */
class ManualWarpTest {

    private val W = 400
    private val H = 400
    private val MESH = 40                       // phải khớp hằng số trong FaceReshape
    private val cols = MESH + 1

    /** Lấy đỉnh (col,row) từ mảng verts xen kẽ x,y. */
    private fun vert(verts: FloatArray, col: Int, row: Int): Pair<Float, Float> {
        val idx = (row * cols + col) * 2
        return verts[idx] to verts[idx + 1]
    }

    @Test
    fun `no-op khi khong co net`() {
        assertNull(computeManualWarpVerts(W, H, emptyList()))
    }

    @Test
    fun `no-op khi net khong dich chuyen hoac ban kinh 0`() {
        assertNull(computeManualWarpVerts(W, H, listOf(WarpStroke(200f, 200f, 0f, 0f, 100f))))
        assertNull(computeManualWarpVerts(W, H, listOf(WarpStroke(200f, 200f, 10f, 0f, 0f))))
    }

    @Test
    fun `dinh tai tam di chuyen dung bang vector keo`() {
        // Lưới 40 ô trên 400px → đỉnh cách nhau 10px; (200,200) là đỉnh (20,20).
        val verts = computeManualWarpVerts(W, H, listOf(WarpStroke(200f, 200f, 20f, -10f, 100f)))!!
        val (x, y) = vert(verts, 20, 20)
        assertEquals(220f, x, 1e-3f)
        assertEquals(190f, y, 1e-3f)
    }

    @Test
    fun `dinh ngoai ban kinh khong di chuyen`() {
        val verts = computeManualWarpVerts(W, H, listOf(WarpStroke(200f, 200f, 20f, 0f, 50f)))!!
        val (x, y) = vert(verts, 0, 0)
        assertEquals(0f, x, 1e-3f)
        assertEquals(0f, y, 1e-3f)
    }

    @Test
    fun `vuot qua nhanh bi kep theo ban kinh`() {
        // Kéo 200px với bán kính 100 → kẹp còn 100 * 0.5 = 50px.
        val verts = computeManualWarpVerts(W, H, listOf(WarpStroke(200f, 200f, 200f, 0f, 100f)))!!
        val (x, _) = vert(verts, 20, 20)
        assertEquals(250f, x, 1e-3f)
    }

    @Test
    fun `hai net cong don dich chuyen`() {
        val s = WarpStroke(200f, 200f, 10f, 0f, 100f)
        val verts = computeManualWarpVerts(W, H, listOf(s, s))!!
        val (x, _) = vert(verts, 20, 20)
        assertEquals(220f, x, 1e-3f)
    }

    @Test
    fun `luc giam dan ra ria (smoothstep)`() {
        val verts = computeManualWarpVerts(W, H, listOf(WarpStroke(200f, 200f, 20f, 0f, 100f)))!!
        val (xCenter, _) = vert(verts, 20, 20)
        val (xHalf, _) = vert(verts, 25, 20)    // cách tâm 50px = nửa bán kính
        val shiftCenter = xCenter - 200f
        val shiftHalf = xHalf - 250f
        assertTrue(shiftHalf > 0f && shiftHalf < shiftCenter)
    }

    @Test
    fun `kich thuoc mang verts dung`() {
        val verts = computeManualWarpVerts(W, H, listOf(WarpStroke(200f, 200f, 10f, 0f, 50f)))
        assertNotNull(verts)
        assertEquals(cols * cols * 2, verts!!.size)
    }
}
