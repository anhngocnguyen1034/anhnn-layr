package com.example.anhnn_layr.presentation.screens.layr

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.anhnn_layr.domain.models.DraftSummary

/** Các tab của thanh điều hướng dưới cùng. */
private enum class LayrTab(val label: String, val icon: ImageVector) {
    HOME("Trang chủ", Icons.Outlined.Home),
    STUDIO("Studio", Icons.Outlined.FolderOpen),
    ACCOUNT("Tài khoản", Icons.Outlined.AccountCircle),
}

/**
 * Khung chính của LAYR ở cấp "trang chủ": chứa Bottom Navigation cố định và
 * chuyển nội dung giữa 3 tab Trang chủ / Studio / Tài khoản.
 *
 * Các callback nhập ảnh được chuyển tiếp từ tab Trang chủ ra ngoài (RembgScreen)
 * để điều hướng sang màn Editor.
 */
@Composable
fun LayrMainScreen(
    drafts: List<DraftSummary>,
    onCapture: () -> Unit,
    onPickFromGallery: () -> Unit,
    onOpenRecent: (Int) -> Unit,
    onOpenDraft: (String) -> Unit,
    onDeleteDraft: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Tab đang chọn — giữ lại khi xoay màn hình
    var selectedTab by rememberSaveable { mutableStateOf(LayrTab.HOME) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LayrColors.Background,
        bottomBar = {
            NavigationBar(containerColor = LayrColors.Surface) {
                LayrTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LayrColors.OnTealContainer,
                            selectedTextColor = LayrColors.Teal,
                            indicatorColor = LayrColors.TealContainer,
                            unselectedIconColor = LayrColors.TextSecondary,
                            unselectedTextColor = LayrColors.TextSecondary,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                LayrTab.HOME -> LayrHomeScreen(
                    onCapture = onCapture,
                    onPickFromGallery = onPickFromGallery,
                    onSeeAllRecent = { selectedTab = LayrTab.STUDIO },
                    onOpenRecent = onOpenRecent,
                    onOpenSettings = { selectedTab = LayrTab.ACCOUNT },
                    onTipPortrait = onCapture,   // mẹo làm nét -> mở luồng nhập ảnh
                    onTipRemoveBg = onPickFromGallery, // mẹo xóa nền -> chọn ảnh
                )
                LayrTab.STUDIO -> StudioScreen(
                    drafts = drafts,
                    onOpenDraft = onOpenDraft,
                    onDeleteDraft = onDeleteDraft,
                )
                LayrTab.ACCOUNT -> AccountScreen()
            }
        }
    }
}
