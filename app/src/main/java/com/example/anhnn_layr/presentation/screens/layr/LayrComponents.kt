package com.example.anhnn_layr.presentation.screens.layr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.roundToInt

/* ----------------------------------------------------------------------------
 *  SLIDER SO SÁNH BEFORE / AFTER
 *  Hai lớp nội dung chồng lên nhau: lớp AFTER nằm dưới (hiển thị toàn bộ),
 *  lớp BEFORE phủ lên và bị cắt theo vị trí thanh trượt (phần bên trái).
 *  Kéo tay nắm để thay đổi tỉ lệ lộ ra.
 * ------------------------------------------------------------------------- */
@Composable
fun BeforeAfterSlider(
    before: @Composable BoxScope.() -> Unit,
    after: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    initialFraction: Float = 0.5f,
    handle: @Composable () -> Unit = { DefaultDivisorHandle() },
) {
    // Tỉ lệ phần BEFORE hiển thị (0f..1f)
    var fraction by remember { mutableFloatStateOf(initialFraction) }

    BoxWithConstraints(modifier) {
        val widthPx = constraints.maxWidth.toFloat()
        val handleSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { 36.dp.toPx() }

        // Lớp AFTER (nền) — luôn hiển thị đầy đủ
        Box(Modifier.matchParentSize()) { after() }

        // Lớp BEFORE — cắt theo fraction để chỉ lộ phần bên trái
        Box(
            Modifier
                .matchParentSize()
                .drawWithContent {
                    clipRect(right = size.width * fraction) { this@drawWithContent.drawContent() }
                },
        ) { before() }

        // Đường kẻ dọc phân tách
        Box(
            Modifier
                .fillMaxHeight()
                .width(2.dp)
                .align(Alignment.CenterStart)
                .offset { IntOffset((fraction * widthPx).roundToInt() - 1, 0) }
                .background(Color.White.copy(alpha = 0.9f)),
        )

        // Tay nắm tròn — kéo ngang để chỉnh tỉ lệ
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((fraction * widthPx - handleSizePx / 2f).roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        fraction = (fraction + dragAmount / widthPx).coerceIn(0f, 1f)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            handle()
        }
    }
}

/** Tay nắm mặc định: vòng tròn kính mờ + icon hai chiều. */
@Composable
private fun DefaultDivisorHandle() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.UnfoldMore,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Nhãn nhỏ "BEFORE" / "AFTER" bo tròn, nền tối bán trong suốt. */
@Composable
fun CornerTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}

/* ----------------------------------------------------------------------------
 *  NÚT PILL KÍNH MỜ (icon + nhãn) — dùng cho thanh công cụ Editor
 * ------------------------------------------------------------------------- */
@Composable
fun GlassPillButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LayrColors.Teal,
) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .glass(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            color = LayrColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/* ----------------------------------------------------------------------------
 *  TIÊU ĐỀ SECTION (chữ in hoa giãn cách) + hành động phụ bên phải
 * ------------------------------------------------------------------------- */
@Composable
fun LayrSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Tiêu đề: chữ in hoa, xám nhạt, nhỏ gọn (đúng phong cách feat.md)
        Text(
            text = title,
            color = LayrColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
        )
        if (actionText != null) {
            Text(
                text = actionText,
                color = LayrColors.Teal,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = onActionClick != null) { onActionClick?.invoke() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

/* ----------------------------------------------------------------------------
 *  THẺ MẸO NHANH (vòng tròn icon + tiêu đề + mô tả)
 * ------------------------------------------------------------------------- */
@Composable
fun TipCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LayrColors.Surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Vòng tròn icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(LayrColors.SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LayrColors.Teal,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                color = LayrColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                color = LayrColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

/* ----------------------------------------------------------------------------
 *  THUMBNAIL "ẢNH GẦN ĐÂY" — load ảnh qua Coil (AsyncImage).
 *  [model] có thể là Uri (ảnh đã chụp) hoặc URL String.
 * ------------------------------------------------------------------------- */
@Composable
fun RecentPhotoCard(
    model: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(width = 110.dp, height = 150.dp)
            .clip(RoundedCornerShape(12.dp)) // bo góc 12dp theo feat.md
            .background(LayrColors.Surface)
            .clickable(onClick = onClick),
    )
}

/* ----------------------------------------------------------------------------
 *  ITEM THANH ĐIỀU HƯỚNG DƯỚI CÙNG (icon + nhãn, có trạng thái chọn)
 * ------------------------------------------------------------------------- */
@Composable
fun LayrNavItem(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) LayrColors.TealContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val content = if (selected) LayrColors.OnTealContainer else LayrColors.TextSecondary
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = text,
            color = content,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
}
