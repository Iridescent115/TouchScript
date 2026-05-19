package com.lulucloud.touchscript.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lulucloud.touchscript.core.automation.DebugScriptDraft
import com.lulucloud.touchscript.core.automation.DebugScriptDraftStore
import com.lulucloud.touchscript.core.script.CompilationResult
import com.lulucloud.touchscript.core.script.ScriptCompiler
import com.lulucloud.touchscript.data.repository.FileScriptRepository
import com.lulucloud.touchscript.data.repository.LocalScriptFile
import com.lulucloud.touchscript.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditorUiState(
    val currentFilePath: String? = null,
    val currentScriptName: String = "未命名脚本",
    val currentSource: String = "",
    val generatedLua: String = "",
    val compileMessage: String = "未编译",
    val compileError: String? = null,
    val scriptWorkspaceUri: String? = null
)

class EditorViewModel(
    private val fileScriptRepository: FileScriptRepository,
    private val settingsRepository: SettingsRepository,
    private val scriptCompiler: ScriptCompiler
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<String>()

    init {
        loadSelectedScriptOrDefault()
        loadWorkspace()
    }

    fun updateSource(source: String) {
        if (source != _uiState.value.currentSource) {
            undoStack.addLast(_uiState.value.currentSource)
            if (undoStack.size > MAX_UNDO_COUNT) {
                undoStack.removeFirst()
            }
            _uiState.value = _uiState.value.copy(currentSource = source)
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(currentSource = undoStack.removeLast())
        }
    }

    fun compileCurrentScript() {
        viewModelScope.launch {
            runCatching { scriptCompiler.compile(_uiState.value.currentSource) }
                .onSuccess(::applyCompilationResult)
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        generatedLua = "",
                        compileMessage = "编译失败",
                        compileError = throwable.message
                    )
                }
        }
    }

    fun openFile(location: String) {
        viewModelScope.launch {
            val file = fileScriptRepository.readFile(location)
            applyOpenedFile(file, "已打开文件：${file.fileName}")
        }
    }

    fun insertSnippet(snippet: String) {
        val normalizedSnippet = snippet.trimEnd()
        if (normalizedSnippet.isBlank()) return

        val current = _uiState.value.currentSource.trimEnd()
        val next = when {
            current.isBlank() -> normalizedSnippet
            else -> "$current\n\n$normalizedSnippet"
        }
        updateSource(next)
    }

    fun saveCurrentToLocation(location: String) {
        viewModelScope.launch {
            val current = _uiState.value
            val saved = fileScriptRepository.saveScript(
                fileNameWithoutExtension = current.currentScriptName,
                content = current.currentSource,
                path = location
            )
            applySavedFile(saved, "已保存")
        }
    }

    fun saveAsToLocation(name: String, location: String) {
        viewModelScope.launch {
            val saved = fileScriptRepository.saveScript(
                fileNameWithoutExtension = normalizeScriptDisplayName(name),
                content = _uiState.value.currentSource,
                path = location
            )
            applySavedFile(saved, "已另存为 ${saved.fileName}")
        }
    }

    fun prepareDebugDraft(onReady: () -> Unit) {
        val current = _uiState.value
        DebugScriptDraftStore.set(
            DebugScriptDraft(
                scriptName = current.currentScriptName,
                content = current.currentSource
            )
        )
        onReady()
    }

    fun clearDebugDraft() {
        DebugScriptDraftStore.clear()
    }

    fun createNewFile(name: String) {
        undoStack.clear()
        _uiState.value = _uiState.value.copy(
            currentFilePath = null,
            currentScriptName = normalizeScriptDisplayName(name),
            currentSource = "",
            generatedLua = "",
            compileMessage = "新建脚本",
            compileError = null
        )
    }

    fun configureScriptWorkspace(parentTreeUri: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val workspaceUri = fileScriptRepository.ensureScriptWorkspace(parentTreeUri)
            settingsRepository.setScriptWorkspaceUri(workspaceUri)
            _uiState.value = _uiState.value.copy(scriptWorkspaceUri = workspaceUri)
            onReady(workspaceUri)
        }
    }

    private fun loadSelectedScriptOrDefault() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            val targetLocation = settings.selectedScriptPath ?: return@launch
            runCatching { fileScriptRepository.readFile(targetLocation) }
                .onSuccess { applyOpenedFile(it, "已打开文件：${it.fileName}") }
        }
    }

    private fun loadWorkspace() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            _uiState.value = _uiState.value.copy(scriptWorkspaceUri = settings.scriptWorkspaceUri)
        }
    }

    private suspend fun applySavedFile(file: LocalScriptFile, message: String) {
        settingsRepository.setSelectedScript(file.absolutePath, file.name)
        _uiState.value = _uiState.value.copy(
            currentFilePath = file.absolutePath,
            currentScriptName = normalizeScriptDisplayName(file.name),
            currentSource = file.content,
            compileMessage = message,
            compileError = null
        )
    }

    private suspend fun applyOpenedFile(file: LocalScriptFile, message: String) {
        undoStack.clear()
        settingsRepository.setSelectedScript(file.absolutePath, file.name)
        _uiState.value = _uiState.value.copy(
            currentFilePath = file.absolutePath,
            currentScriptName = normalizeScriptDisplayName(file.name),
            currentSource = file.content,
            generatedLua = "",
            compileMessage = message,
            compileError = null
        )
    }

    private fun applyCompilationResult(result: CompilationResult) {
        _uiState.value = _uiState.value.copy(
            generatedLua = result.luaSource,
            compileMessage = "编译成功，共 ${result.ast.statements.size} 条顶层语句",
            compileError = null
        )
    }

    private companion object {
        const val MAX_UNDO_COUNT = 80
    }
}

private fun normalizeScriptDisplayName(name: String): String {
    var result = name.trim()
    while (result.endsWith(".tscript", ignoreCase = true)) {
        result = result.dropLast(".tscript".length)
    }
    return result.ifBlank { "未命名脚本" }
}
