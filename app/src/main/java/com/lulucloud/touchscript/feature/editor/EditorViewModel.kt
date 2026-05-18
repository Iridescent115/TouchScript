package com.lulucloud.touchscript.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val compileMessage: String = "尚未编译",
    val compileError: String? = null,
    val scriptFiles: List<LocalScriptFile> = emptyList(),
    val templateFiles: List<LocalScriptFile> = emptyList()
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
        refreshFileLists()
        loadSelectedScriptOrDefault()
    }

    fun updateScriptName(name: String) {
        _uiState.value = _uiState.value.copy(currentScriptName = name)
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

    fun openFile(file: LocalScriptFile) {
        undoStack.clear()
        _uiState.value = _uiState.value.copy(
            currentFilePath = file.absolutePath,
            currentScriptName = file.name,
            currentSource = file.content,
            generatedLua = "",
            compileMessage = "已打开文件：${file.fileName}",
            compileError = null
        )
        viewModelScope.launch {
            settingsRepository.setSelectedScript(file.absolutePath, file.name)
        }
    }

    fun insertTemplate(file: LocalScriptFile) {
        val next = if (_uiState.value.currentSource.isBlank()) {
            file.content
        } else {
            _uiState.value.currentSource + "\n\n" + file.content
        }
        updateSource(next)
    }

    fun saveCurrent() {
        viewModelScope.launch {
            val current = _uiState.value
            val saved = fileScriptRepository.saveScript(
                fileNameWithoutExtension = current.currentScriptName,
                content = current.currentSource,
                path = current.currentFilePath
            )
            applySavedFile(saved, "已保存")
        }
    }

    fun saveAs(name: String) {
        viewModelScope.launch {
            val saved = fileScriptRepository.saveScript(
                fileNameWithoutExtension = name,
                content = _uiState.value.currentSource
            )
            applySavedFile(saved, "已另存为 ${saved.fileName}")
        }
    }

    fun createNewFile() {
        undoStack.clear()
        _uiState.value = EditorUiState(
            currentScriptName = "未命名脚本",
            scriptFiles = _uiState.value.scriptFiles,
            templateFiles = _uiState.value.templateFiles
        )
    }

    fun refreshFileLists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                scriptFiles = fileScriptRepository.listUserScripts(),
                templateFiles = fileScriptRepository.listTemplates()
            )
        }
    }

    private fun loadSelectedScriptOrDefault() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            val targetFile = settings.selectedScriptPath
                ?.let { path -> runCatching { fileScriptRepository.readFile(path) }.getOrNull() }
                ?: fileScriptRepository.listUserScripts().firstOrNull()

            if (targetFile != null) {
                openFile(targetFile)
            }
        }
    }

    private suspend fun applySavedFile(file: LocalScriptFile, message: String) {
        settingsRepository.setSelectedScript(file.absolutePath, file.name)
        _uiState.value = _uiState.value.copy(
            currentFilePath = file.absolutePath,
            currentScriptName = file.name,
            currentSource = file.content,
            compileMessage = message,
            compileError = null
        )
        refreshFileLists()
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
