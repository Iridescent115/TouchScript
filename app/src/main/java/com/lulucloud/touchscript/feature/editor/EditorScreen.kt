package com.lulucloud.touchscript.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lulucloud.touchscript.feature.home.ScriptFilePickerDialog
import com.lulucloud.touchscript.ui.components.WorkshopHeroCard
import com.lulucloud.touchscript.ui.components.WorkshopPanel
import com.lulucloud.touchscript.ui.components.WorkshopScreen
import com.lulucloud.touchscript.ui.components.WorkshopStatusChip
import com.lulucloud.touchscript.ui.theme.WorkshopSuccess
import com.lulucloud.touchscript.ui.theme.WorkshopSuccessSoft

@Composable
fun EditorScreen(viewModel: EditorViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val editorScroll = rememberScrollState()
    var showOpenDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    val lineCount = uiState.currentSource.lines().size.coerceAtLeast(1)
    val charCount = uiState.currentSource.length
    val isCompileSuccess = uiState.compileError == null && uiState.generatedLua.isNotBlank()
    val resultContainer = when {
        isCompileSuccess -> WorkshopSuccessSoft
        uiState.compileError != null -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val resultColor = when {
        isCompileSuccess -> WorkshopSuccess
        uiState.compileError != null -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val resultLabel = when {
        isCompileSuccess -> "编译成功"
        uiState.compileError != null -> "编译失败"
        else -> "等待编译"
    }

    WorkshopScreen(modifier = Modifier.statusBarsPadding()) {
        WorkshopHeroCard(
            eyebrow = "中文脚本编辑台",
            title = "脚本编辑器",
            subtitle = "保留工具属性和控制感，把脚本编辑区做得更像一张安静的工作台。"
        ) {
            Button(onClick = viewModel::compileCurrentScript) {
                Text("编译")
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditorCommandButton(text = "新建", onClick = viewModel::createNewFile)
            EditorCommandButton(text = "插入模板", onClick = { showTemplateDialog = true })
            EditorCommandButton(text = "保存", onClick = viewModel::saveCurrent)
            EditorCommandButton(text = "另存为", onClick = { showSaveAsDialog = true })
            EditorCommandButton(
                text = "打开文件",
                onClick = {
                    viewModel.refreshFileLists()
                    showOpenDialog = true
                }
            )
            EditorCommandButton(text = "撤销", onClick = viewModel::undo)
        }

        OutlinedTextField(
            value = uiState.currentScriptName,
            onValueChange = viewModel::updateScriptName,
            label = { Text("文件名") },
            supportingText = { Text("保存时会自动写入本地 scripts 目录。") },
            modifier = Modifier.fillMaxWidth()
        )

        WorkshopPanel(
            title = "脚本编辑区",
            subtitle = "支持中文 DSL 语法高亮，保持长时间编辑时的可读性。"
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF151E29),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WorkshopStatusChip(
                            text = "中文 DSL",
                            containerColor = Color(0xFF243245),
                            contentColor = Color(0xFFE9D3BB)
                        )
                        Text(
                            text = "$lineCount 行 / $charCount 字",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF93A4B8)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF101720), shape = MaterialTheme.shapes.medium)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = buildLineNumbers(uiState.currentSource),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF627287),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            ),
                            modifier = Modifier.width(36.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 320.dp, max = 520.dp)
                        ) {
                            if (uiState.currentSource.isBlank()) {
                                Text(
                                    text = "在这里编写你的中文脚本，例如：\n记录 \"开始执行\"\n点击 540 1600",
                                    style = TextStyle(
                                        color = Color(0xFF597087),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp
                                    )
                                )
                            }
                            BasicTextField(
                                value = uiState.currentSource,
                                onValueChange = viewModel::updateSource,
                                textStyle = TextStyle(
                                    color = Color(0xFFF5F1E7),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                ),
                                visualTransformation = DslSyntaxHighlightTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 320.dp, max = 520.dp)
                                    .verticalScroll(editorScroll)
                            )
                        }
                    }
                }
            }
        }

        WorkshopPanel(
            title = "编译结果",
            subtitle = "这里展示当前脚本的校验结果，以及对应生成的 Lua 代码。"
        ) {
            WorkshopStatusChip(
                text = resultLabel,
                containerColor = resultContainer,
                contentColor = resultColor
            )
            Text(uiState.compileMessage)
            uiState.compileError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            if (uiState.generatedLua.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                    shape = MaterialTheme.shapes.large
                ) {
                    SelectionContainer {
                        Text(
                            text = uiState.generatedLua,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 20.sp
                            ),
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }
    }

    if (showOpenDialog) {
        ScriptFilePickerDialog(
            title = "打开本地脚本",
            files = uiState.scriptFiles + uiState.templateFiles,
            onDismiss = { showOpenDialog = false },
            onSelect = {
                viewModel.openFile(it)
                showOpenDialog = false
            }
        )
    }

    if (showTemplateDialog) {
        ScriptFilePickerDialog(
            title = "选择模板脚本",
            files = uiState.templateFiles,
            onDismiss = { showTemplateDialog = false },
            onSelect = {
                viewModel.insertTemplate(it)
                showTemplateDialog = false
            }
        )
    }

    if (showSaveAsDialog) {
        SaveAsDialog(
            initialName = uiState.currentScriptName,
            onDismiss = { showSaveAsDialog = false },
            onConfirm = {
                viewModel.saveAs(it)
                showSaveAsDialog = false
            }
        )
    }
}

@Composable
private fun EditorCommandButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick) {
        Text(text)
    }
}

@Composable
private fun SaveAsDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var fileName by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("另存为") },
        text = {
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text("文件名") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(fileName) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun buildLineNumbers(source: String): String {
    val lineCount = source.lines().size.coerceAtLeast(1)
    return (1..lineCount).joinToString(separator = "\n")
}
