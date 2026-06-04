package com.example.anhnn_layr

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.anhnn_layr.presentation.screens.RembgScreen
import com.example.anhnn_layr.presentation.theme.AnhnnlayrTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge: nội dung vẽ tràn dưới status/navigation bar. Vì LAYR khóa
        // Dark mode nên ép cả hai thanh hệ thống dùng kiểu "dark" (icon sáng) và
        // trong suốt, bất kể hệ thống đang ở Light hay Dark — nếu không, ở Light
        // mode icon sẽ tối và chìm vào nền tối của app.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            // LAYR khóa ở Dark mode hoàn toàn theo thiết kế
            AnhnnlayrTheme(darkTheme = true) {
                // Full-bleed: chỉ phủ màu nền toàn màn hình (kể cả vùng sau system
                // bars). KHÔNG pad insets ở đây — mỗi màn hình tự xử lý insets của
                // mình (Scaffold / statusBarsPadding / navigationBarsPadding…).
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RembgScreen()
                }
            }
        }
    }
}
