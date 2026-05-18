package com.lulucloud.touchscript.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lulucloud.touchscript.core.script.ScriptCompiler
import com.lulucloud.touchscript.data.repository.FileScriptRepository
import com.lulucloud.touchscript.data.repository.LocalScriptFile
import com.lulucloud.touchscript.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class HomeUiState(
    val availableScripts: List<LocalScriptFile> = emptyList(),
    val selectedScriptName: String = "",
    val selectedScriptPath: String? = null,
    val selectedScriptSummary: String = "还没有加载脚本",
    val isScriptReady: Boolean = false,
    val validationMessage: String = "请先加载一个脚本文件"
)

class HomeViewModel(
    private val fileScriptRepository: FileScriptRepository,
    private val settingsRepository: SettingsRepository,
    private val scriptCompiler: ScriptCompiler
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeSelectedScript()
        refreshFiles()
    }

    fun refreshFiles() {
        viewModelScope.launch {
            val files = fileScriptRepository.listAllScripts()
            _uiState.value = _uiState.value.copy(availableScripts = files)
        }
    }

    fun loadScript(file: LocalScriptFile) {
        viewModelScope.launch {
            settingsRepository.setSelectedScript(file.absolutePath, file.name)
            validateFile(file.absolutePath)
        }
    }

    private fun observeSelectedScript() {
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                val path = settings.selectedScriptPath
                if (path.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        selectedScriptName = "",
                        selectedScriptPath = null,
                        selectedScriptSummary = "还没有加载脚本",
                        isScriptReady = false,
                        validationMessage = "请先加载一个脚本文件"
                    )
                } else {
                    validateFile(path)
                }
            }
        }
    }

    private suspend fun validateFile(path: String) {
        runCatching {
            val file = fileScriptRepository.readFile(path)
            val compilation = scriptCompiler.compile(file.content)
            file to compilation
        }.onSuccess { (file, compilation) ->
            _uiState.value = _uiState.value.copy(
                selectedScriptName = file.name,
                selectedScriptPath = file.absolutePath,
                selectedScriptSummary = "脚本包含 ${compilation.ast.statements.size} 条顶层语句",
                isScriptReady = true,
                validationMessage = "脚本可用，已通过 DSL 编译校验"
            )
        }.onFailure { throwable ->
            _uiState.value = _uiState.value.copy(
                selectedScriptName = _uiState.value.selectedScriptName,
                selectedScriptPath = path,
                selectedScriptSummary = "脚本校验失败",
                isScriptReady = false,
                validationMessage = throwable.message ?: "脚本校验失败"
            )
        }
    }
}
