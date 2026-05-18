package com.lulucloud.touchscript.feature.settings

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lulucloud.touchscript.common.openAccessibilitySettings
import com.lulucloud.touchscript.common.openNotificationSettings
import com.lulucloud.touchscript.common.openOverlaySettings
import com.lulucloud.touchscript.ui.components.WorkshopHeroCard
import com.lulucloud.touchscript.ui.components.WorkshopPanel
import com.lulucloud.touchscript.ui.components.WorkshopScreen
import com.lulucloud.touchscript.ui.components.WorkshopStatusChip

@Composable
fun SettingsScreen(context: Context) {
    WorkshopScreen(modifier = Modifier.statusBarsPadding()) {
        WorkshopHeroCard(
            eyebrow = "运行准备",
            title = "设置",
            subtitle = "把真正影响脚本执行的系统权限放在前面，页面本身保持清楚、直给。"
        ) {
            WorkshopStatusChip(text = "执行前检查")
        }

        PermissionPanel(
            title = "无障碍服务",
            description = "执行点击、滑动、返回等自动化动作前，需要先开启无障碍服务。",
            actionText = "打开无障碍设置",
            onClick = { context.openAccessibilitySettings() }
        )

        PermissionPanel(
            title = "悬浮窗权限",
            description = "控制条需要悬浮窗权限，开启后才能显示脚本启动、暂停和日志入口。",
            actionText = "打开悬浮窗设置",
            onClick = { context.openOverlaySettings() }
        )

        PermissionPanel(
            title = "通知权限",
            description = "前台服务运行时会展示通知，建议一并开启，避免运行状态不透明。",
            actionText = "打开通知设置",
            onClick = { context.openNotificationSettings() }
        )

        WorkshopPanel(
            title = "推荐顺序",
            subtitle = "按这个顺序完成准备，回到首页后就能更顺手地进入执行链路。"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkshopStatusChip(text = "1. 开无障碍")
                WorkshopStatusChip(text = "2. 开悬浮窗")
                WorkshopStatusChip(text = "3. 开通知")
                WorkshopStatusChip(text = "4. 回首页启用")
            }
        }
    }
}

@Composable
private fun PermissionPanel(
    title: String,
    description: String,
    actionText: String,
    onClick: () -> Unit
) {
    WorkshopPanel(title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(actionText)
            }
        }
    }
}
