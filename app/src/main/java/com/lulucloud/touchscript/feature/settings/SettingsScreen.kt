package com.lulucloud.touchscript.feature.settings

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
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
fun SettingsScreen(
    context: Context,
    workspaceUri: String?,
    onChooseWorkspace: () -> Unit
) {
    WorkshopScreen(modifier = Modifier.statusBarsPadding()) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)

        WorkspacePanel(
            workspaceUri = workspaceUri,
            onChooseWorkspace = onChooseWorkspace
        )

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
private fun WorkspacePanel(
    workspaceUri: String?,
    onChooseWorkspace: () -> Unit
) {
    WorkshopPanel(title = "工作目录") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onChooseWorkspace,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("选择工作目录")
            }
            Text(
                text = "当前工作目录：${formatWorkspaceUri(workspaceUri)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

private fun formatWorkspaceUri(workspaceUri: String?): String {
    if (workspaceUri.isNullOrBlank()) {
        return "未选择"
    }

    return runCatching {
        val uri = Uri.parse(workspaceUri)
        DocumentsContract.getDocumentId(uri)
            .replace("primary:", "内部存储/")
            .ifBlank { workspaceUri }
    }.getOrDefault(workspaceUri)
}
