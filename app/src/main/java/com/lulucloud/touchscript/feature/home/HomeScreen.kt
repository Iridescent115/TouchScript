package com.lulucloud.touchscript.feature.home

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import com.lulucloud.touchscript.common.AutomationLauncher
import com.lulucloud.touchscript.data.repository.LocalScriptFile
import com.lulucloud.touchscript.ui.components.WorkshopHeroCard
import com.lulucloud.touchscript.ui.components.WorkshopPanel
import com.lulucloud.touchscript.ui.components.WorkshopScreen
import com.lulucloud.touchscript.ui.components.WorkshopStatusChip
import com.lulucloud.touchscript.ui.theme.WorkshopSuccess
import com.lulucloud.touchscript.ui.theme.WorkshopSuccessSoft

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    context: Context
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFileDialog by remember { mutableStateOf(false) }
    val readyColor = if (uiState.isScriptReady) WorkshopSuccess else MaterialTheme.colorScheme.error
    val readyContainer = if (uiState.isScriptReady) {
        WorkshopSuccessSoft
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    WorkshopScreen(modifier = Modifier.statusBarsPadding()) {
        WorkshopHeroCard(
            eyebrow = "原型工作台",
            title = "触灵工坊",
            subtitle = "把中文脚本的加载、编译校验和悬浮窗启动收进一个更克制、更像工具台的首页。"
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                WorkshopStatusChip(text = "DSL → Lua")
                WorkshopStatusChip(
                    text = if (uiState.isScriptReady) "脚本已就绪" else "等待校验通过",
                    containerColor = readyContainer,
                    contentColor = readyColor
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeMetricCard(
                modifier = Modifier.weight(1f),
                title = "编译链",
                value = "DSL 校验"
            )
            HomeMetricCard(
                modifier = Modifier.weight(1f),
                title = "执行入口",
                value = "悬浮窗控制"
            )
        }

        WorkshopPanel(
            title = "当前脚本",
            subtitle = "加载脚本后会自动执行一次 DSL 编译校验。"
        ) {
            Text(
                text = uiState.selectedScriptName.ifBlank { "还没有选择脚本" },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = uiState.selectedScriptSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            WorkshopStatusChip(
                text = uiState.validationMessage,
                containerColor = readyContainer,
                contentColor = readyColor
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { AutomationLauncher.showOverlay(context) },
                enabled = uiState.isScriptReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 160.dp)
            ) {
                Text("启用悬浮窗")
            }

            OutlinedButton(
                onClick = {
                    viewModel.refreshFiles()
                    showFileDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("加载脚本")
            }
        }

        WorkshopPanel(
            title = "使用流程",
            subtitle = "首页只保留一个主动作，避免在启动前让用户分心。"
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkflowChip("1. 选择脚本")
                WorkflowChip("2. 自动校验")
                WorkflowChip("3. 启动悬浮窗")
                WorkflowChip("4. 在悬浮条里执行")
            }
        }
    }

    if (showFileDialog) {
        ScriptFilePickerDialog(
            title = "选择要加载的脚本",
            files = uiState.availableScripts,
            onDismiss = { showFileDialog = false },
            onSelect = {
                viewModel.loadScript(it)
                showFileDialog = false
            }
        )
    }
}

@Composable
private fun HomeMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun WorkflowChip(text: String) {
    WorkshopStatusChip(
        text = text,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

@Composable
fun ScriptFilePickerDialog(
    title: String,
    files: List<LocalScriptFile>,
    onDismiss: () -> Unit,
    onSelect: (LocalScriptFile) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (files.isEmpty()) {
                    Text("本地还没有脚本文件")
                } else {
                    files.forEach { file ->
                        TextButton(
                            onClick = { onSelect(file) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = file.name)
                                if (file.isTemplate) {
                                    Text(
                                        text = "模板",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
