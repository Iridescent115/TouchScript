package com.lulucloud.touchscript.feature.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lulucloud.touchscript.common.AutomationLauncher
import com.lulucloud.touchscript.data.repository.LocalScriptFile

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    context: Context
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFileDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("触灵工坊", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "首页只负责装载脚本和启用悬浮控制条。脚本加载后会先做一次 DSL 编译校验。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("当前脚本", style = MaterialTheme.typography.titleMedium)
                Text(uiState.selectedScriptName.ifBlank { "未加载脚本" })
                Text(
                    uiState.selectedScriptSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    uiState.validationMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.isScriptReady) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
                viewModel.refreshFiles()
                showFileDialog = true
            }) {
                Text("加载脚本")
            }

            Button(
                onClick = { AutomationLauncher.showOverlay(context) },
                enabled = uiState.isScriptReady,
                modifier = Modifier.widthIn(min = 160.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isScriptReady) Color(0xFF66BB6A) else Color(0xFFBDBDBD),
                    contentColor = if (uiState.isScriptReady) Color(0xFF0E3E18) else Color(0xFF4F4F4F),
                    disabledContainerColor = Color(0xFFBDBDBD),
                    disabledContentColor = Color(0xFF4F4F4F)
                )
            ) {
                Text(text = "启用悬浮窗")
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
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
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
                            Text(
                                text = if (file.isTemplate) "模板 · ${file.name}" else file.name,
                                modifier = Modifier.fillMaxWidth()
                            )
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
