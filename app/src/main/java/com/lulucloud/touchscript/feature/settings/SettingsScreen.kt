package com.lulucloud.touchscript.feature.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lulucloud.touchscript.common.openAccessibilitySettings
import com.lulucloud.touchscript.common.openNotificationSettings
import com.lulucloud.touchscript.common.openOverlaySettings

@Composable
fun SettingsScreen(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "这里仅保留脚本自动化所需的系统权限入口。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PermissionCard(
            title = "无障碍服务",
            description = "启用后才能执行点击、滑动、返回等自动化动作。",
            actionText = "打开无障碍设置",
            onClick = { context.openAccessibilitySettings() }
        )

        PermissionCard(
            title = "悬浮窗权限",
            description = "启用后才能显示圆形横条悬浮控制窗。",
            actionText = "打开悬浮窗设置",
            onClick = { context.openOverlaySettings() }
        )

        PermissionCard(
            title = "通知权限",
            description = "用于前台服务运行时展示通知。",
            actionText = "打开通知设置",
            onClick = { context.openNotificationSettings() }
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    actionText: String,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onClick) {
                Text(actionText)
            }
        }
    }
}
