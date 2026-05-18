package com.lulucloud.touchscript.feature.runner

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.lulucloud.touchscript.common.AutomationLauncher
import com.lulucloud.touchscript.common.canDrawOverlay
import com.lulucloud.touchscript.common.isAccessibilityServiceEnabled
import com.lulucloud.touchscript.common.openAccessibilitySettings
import com.lulucloud.touchscript.common.openNotificationSettings
import com.lulucloud.touchscript.common.openOverlaySettings

@Composable
fun RunnerScreen(
    viewModel: RunnerViewModel,
    context: Context
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val accessibilityEnabled = context.isAccessibilityServiceEnabled()
    val overlayEnabled = context.canDrawOverlay()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "执行中心",
            style = MaterialTheme.typography.headlineSmall
        )

        PermissionCard(
            title = "无障碍服务",
            status = if (accessibilityEnabled) "已启用" else "未启用",
            actionText = "打开设置",
            onAction = { context.openAccessibilitySettings() }
        )

        PermissionCard(
            title = "悬浮窗权限",
            status = if (overlayEnabled) "已授予" else "未授予",
            actionText = "授权悬浮窗",
            onAction = { context.openOverlaySettings() }
        )

        PermissionCard(
            title = "通知权限",
            status = "用于前台服务常驻通知",
            actionText = "通知设置",
            onAction = { context.openNotificationSettings() }
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("运行状态", style = MaterialTheme.typography.titleMedium)
                Text("当前状态：${uiState.sessionState.status.name}")
                Text("当前脚本：${uiState.sessionState.scriptName.ifBlank { "暂无" }}")
                Text("结果摘要：${uiState.sessionState.summary.ifBlank { "等待执行" }}")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("启用悬浮控制面板")
                    Switch(
                        checked = uiState.settings.showFloatingPanel,
                        onCheckedChange = viewModel::setFloatingPanel
                    )
                }
                Button(onClick = { AutomationLauncher.stop(context) }) {
                    Text("停止当前脚本")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("执行日志", style = MaterialTheme.typography.titleMedium)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF11151D))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (uiState.logs.isEmpty()) {
                        Text("暂无日志", color = Color(0xFFD6DEE8))
                    } else {
                        uiState.logs.forEach { log ->
                            Text(
                                text = "[${log.level}] ${log.message}",
                                color = Color(0xFFD6DEE8),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("最近运行记录", style = MaterialTheme.typography.titleMedium)
                if (uiState.recentRuns.isEmpty()) {
                    Text("暂无运行记录")
                } else {
                    uiState.recentRuns.forEach { record ->
                        Text(
                            text = "${record.scriptName} · ${record.status} · ${record.summary}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    status: String,
    actionText: String,
    onAction: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}
