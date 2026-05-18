package com.lulucloud.touchscript.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lulucloud.touchscript.core.script.CompilationResult
import com.lulucloud.touchscript.core.script.ScriptCompiler
import com.lulucloud.touchscript.data.local.ScriptEntity
import com.lulucloud.touchscript.data.local.ScriptTemplateEntity
import com.lulucloud.touchscript.data.repository.ScriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class EditorUiState(
    val scripts: List<ScriptEntity> = emptyList(),
    val templates: List<ScriptTemplateEntity> = emptyList(),
    val currentScriptId: Long? = null,
    val currentScriptName: String = "",
    val currentSource: String = "",
    val generatedLua: String = "",
    val compileMessage: String = "",
    val compileError: String? = null
)

class EditorViewModel(
    private val scriptRepository: ScriptRepository,
    private val scriptCompiler: ScriptCompiler
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        observeScripts()
        observeTemplates()
    }

    fun createScript() {
        _uiState.value = _uiState.value.copy(
            currentScriptId = null,
            currentScriptName = "未命名脚本",
            currentSource = "",
            generatedLua = "",
            compileMessage = "已创建新脚本草稿",
            compileError = null
        )
    }

    fun selectScript(script: ScriptEntity) {
        _uiState.value = _uiState.value.copy(
            currentScriptId = script.id,
            currentScriptName = script.name,
            currentSource = script.source,
            generatedLua = "",
            compileMessage = "已载入脚本：${script.name}",
            compileError = null
        )
    }

    fun updateScriptName(name: String) {
        _uiState.value = _uiState.value.copy(currentScriptName = name)
    }

    fun updateSource(source: String) {
        _uiState.value = _uiState.value.copy(currentSource = source)
    }

    fun insertTemplate(template: ScriptTemplateEntity) {
        val currentSource = _uiState.value.currentSource
        val nextSource = if (currentSource.isBlank()) {
            template.source
        } else {
            "$currentSource\n\n${template.source}"
        }
        _uiState.value = _uiState.value.copy(
            currentSource = nextSource,
            compileMessage = "已插入模板：${template.name}"
        )
    }

    fun insertKeyword(keyword: String) {
        val source = _uiState.value.currentSource
        val nextSource = if (source.isBlank()) keyword else "$source\n$keyword"
        _uiState.value = _uiState.value.copy(currentSource = nextSource)
    }

    fun compileCurrentScript() {
        viewModelScope.launch {
            val source = _uiState.value.currentSource
            runCatching { scriptCompiler.compile(source) }
                .onSuccess(::updateCompileResult)
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        generatedLua = "",
                        compileMessage = "编译失败",
                        compileError = throwable.message
                    )
                }
        }
    }

    suspend fun saveCurrentScript(): Pair<Long, String> {
        val current = _uiState.value
        val name = current.currentScriptName.ifBlank { "未命名脚本" }
        val source = current.currentSource.ifBlank {
            """
                记录 "这是一个空脚本"
                等待 300
            """.trimIndent()
        }
        val scriptId = scriptRepository.saveScript(current.currentScriptId, name, source)
        _uiState.value = _uiState.value.copy(
            currentScriptId = scriptId,
            currentScriptName = name,
            currentSource = source,
            compileMessage = "脚本已保存",
            compileError = null
        )
        return scriptId to source
    }

    private fun updateCompileResult(result: CompilationResult) {
        _uiState.value = _uiState.value.copy(
            generatedLua = result.luaSource,
            compileMessage = "编译成功，共 ${result.ast.statements.size} 条顶层语句",
            compileError = null
        )
    }

    private fun observeScripts() {
        viewModelScope.launch {
            scriptRepository.observeScripts().collectLatest { scripts ->
                val state = _uiState.value
                val currentScript = scripts.firstOrNull { it.id == state.currentScriptId } ?: scripts.firstOrNull()
                _uiState.value = if (currentScript != null && state.currentScriptId == null) {
                    state.copy(
                        scripts = scripts,
                        currentScriptId = currentScript.id,
                        currentScriptName = currentScript.name,
                        currentSource = currentScript.source
                    )
                } else {
                    state.copy(scripts = scripts)
                }
            }
        }
    }

    private fun observeTemplates() {
        viewModelScope.launch {
            scriptRepository.observeTemplates().collectLatest { templates ->
                _uiState.value = _uiState.value.copy(templates = templates)
            }
        }
    }
}
