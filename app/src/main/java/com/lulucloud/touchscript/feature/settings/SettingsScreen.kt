package com.lulucloud.touchscript.feature.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lulucloud.touchscript.common.openAccessibilitySettings
import com.lulucloud.touchscript.common.openNotificationSettings
import com.lulucloud.touchscript.common.openOverlaySettings
import com.lulucloud.touchscript.ui.components.WorkshopPanel
import com.lulucloud.touchscript.ui.components.WorkshopScreen

@Composable
fun SettingsScreen(context: Context) {
    WorkshopScreen(modifier = Modifier.statusBarsPadding()) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)

        PermissionPanel(
            title = "无障碍服务",
            actionText = "打开无障碍设置",
            onClick = { context.openAccessibilitySettings() }
        )

        PermissionPanel(
            title = "悬浮窗权限",
            actionText = "打开悬浮窗设置",
            onClick = { context.openOverlaySettings() }
        )

        PermissionPanel(
            title = "通知权限",
            actionText = "打开通知设置",
            onClick = { context.openNotificationSettings() }
        )
    }
}

@Composable
private fun PermissionPanel(
    title: String,
    actionText: String,
    onClick: () -> Unit
) {
    WorkshopPanel(title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(actionText)
            }
        }
    }
}
