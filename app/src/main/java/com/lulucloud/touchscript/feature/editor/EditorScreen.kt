package com.lulucloud.touchscript.feature.editor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.lulucloud.touchscript.common.AutomationLauncher
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    context: Context
) {
    val uiState by viewModel.uiState.collectAsState()
    val editorScroll = rememberScrollState()
    val chipScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "脚本编辑器",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "面向 DSL 的首版编辑器，脚本会被编译成 Lua 后再执行。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = uiState.currentScriptName,
            onValueChange = viewModel::updateScriptName,
            label = { Text("脚本名称") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = viewModel::createScript) {
                Text("新建脚本")
            }
            Button(onClick = viewModel::compileCurrentScript) {
                Text("编译 DSL")
            }
            Button(
                onClick = {
                    viewModel.viewModelScope.launch {
                        val (_, source) = viewModel.saveCurrentScript()
                        AutomationLauncher.start(
                            context = context,
                            scriptName = viewModel.uiState.value.currentScriptName.ifBlank { "未命名脚本" },
                            scriptSource = source
                        )
                    }
                }
            ) {
                Text("保存并运行")
            }
        }

        ScriptSelectorSection(
            scriptNames = uiState.scripts,
            onSelect = viewModel::selectScript
        )

        TemplateSection(
            templates = uiState.templates.map { it.name to it },
            onInsert = viewModel::insertTemplate
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(chipScroll),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("点击 540 1600", "等待 300", "循环 10 次", "结束循环", "如果 次数 > 0", "结束如果", "返回", "主页")
                .forEach { keyword ->
                    AssistChip(
                        onClick = { viewModel.insertKeyword(keyword) },
                        label = { Text(keyword) }
                    )
                }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DSL 编辑区", style = MaterialTheme.typography.titleMedium)
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
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.width(32.dp)
                    )
                    BasicTextField(
                        value = uiState.currentSource,
                        onValueChange = viewModel::updateSource,
                        textStyle = TextStyle(
                            color = Color(0xFFF5F7FA),
                            fontFamily = FontFamily.Monospace
                        ),
                        visualTransformation = DslSyntaxHighlightTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 260.dp)
                            .verticalScroll(editorScroll)
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("编译结果", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = uiState.compileMessage.ifBlank { "尚未编译" },
                    style = MaterialTheme.typography.bodyMedium
                )
                uiState.compileError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
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

@Composable
private fun ScriptSelectorSection(
    scriptNames: List<com.lulucloud.touchscript.data.local.ScriptEntity>,
    onSelect: (com.lulucloud.touchscript.data.local.ScriptEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("现有脚本", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            scriptNames.forEach { script ->
                AssistChip(
                    onClick = { onSelect(script) },
                    label = { Text(script.name) }
                )
            }
        }
    }
}

@Composable
private fun TemplateSection(
    templates: List<Pair<String, com.lulucloud.touchscript.data.local.ScriptTemplateEntity>>,
    onInsert: (com.lulucloud.touchscript.data.local.ScriptTemplateEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("模板脚本", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            templates.forEach { (_, template) ->
                AssistChip(
                    onClick = { onInsert(template) },
                    label = { Text(template.name) }
                )
            }
        }
        templates.forEach { (_, template) ->
            Text(
                text = "${template.name}：${template.description}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.wrapContentHeight()
            )
        }
    }
}

private fun buildLineNumbers(source: String): String {
    val lineCount = source.lines().size.coerceAtLeast(1)
    return (1..lineCount).joinToString(separator = "\n")
}
