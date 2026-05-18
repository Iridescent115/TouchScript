package com.lulucloud.touchscript.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lulucloud.touchscript.feature.home.ScriptFilePickerDialog

@Composable
fun EditorScreen(viewModel: EditorViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val editorScroll = rememberScrollState()
    var showOpenDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("脚本编辑器", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = viewModel::compileCurrentScript) {
                    Text("编译")
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = viewModel::createNewFile) { Text("新建") }
                TextButton(onClick = { showTemplateDialog = true }) { Text("插入模板") }
                TextButton(onClick = viewModel::saveCurrent) { Text("保存") }
                TextButton(onClick = { showSaveAsDialog = true }) { Text("另存为") }
                TextButton(onClick = {
                    viewModel.refreshFileLists()
                    showOpenDialog = true
                }) { Text("打开文件") }
                TextButton(onClick = viewModel::undo) { Text("撤销") }
            }

            OutlinedTextField(
                value = uiState.currentScriptName,
                onValueChange = viewModel::updateScriptName,
                label = { Text("文件名") },
                modifier = Modifier.fillMaxWidth()
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("脚本编辑区", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF121722))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = buildLineNumbers(uiState.currentSource),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF63708A),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier.width(32.dp)
                        )
                        BasicTextField(
                            value = uiState.currentSource,
                            onValueChange = viewModel::updateSource,
                            textStyle = TextStyle(
                                color = Color(0xFFF5F7FA),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                lineHeight = 18.sp
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("编译结果", style = MaterialTheme.typography.titleMedium)
                    Text(uiState.compileMessage)
                    uiState.compileError?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                    if (uiState.generatedLua.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF4EFE5))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = uiState.generatedLua,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
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
