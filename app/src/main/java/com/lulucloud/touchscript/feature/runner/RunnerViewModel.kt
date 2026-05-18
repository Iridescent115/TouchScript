package com.lulucloud.touchscript.feature.runner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lulucloud.touchscript.core.automation.AutomationSessionManager
import com.lulucloud.touchscript.core.automation.AutomationSessionState
import com.lulucloud.touchscript.core.automation.ExecutionLogEntry
import com.lulucloud.touchscript.data.local.RunRecordEntity
import com.lulucloud.touchscript.data.repository.AppSettings
import com.lulucloud.touchscript.data.repository.ScriptRepository
import com.lulucloud.touchscript.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class RunnerUiState(
    val sessionState: AutomationSessionState = AutomationSessionState(),
    val logs: List<ExecutionLogEntry> = emptyList(),
    val recentRuns: List<RunRecordEntity> = emptyList(),
    val settings: AppSettings = AppSettings()
)

class RunnerViewModel(
    private val sessionManager: AutomationSessionManager,
    private val scriptRepository: ScriptRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(RunnerUiState())
    val uiState: StateFlow<RunnerUiState> = _uiState.asStateFlow()

    init {
        observeSession()
        observeRecentRuns()
        observeSettings()
    }

    fun setFloatingPanel(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowFloatingPanel(enabled)
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.sessionState.collectLatest { session ->
                _uiState.value = _uiState.value.copy(sessionState = session)
            }
        }
        viewModelScope.launch {
            sessionManager.logs.collectLatest { logs ->
                _uiState.value = _uiState.value.copy(logs = logs)
            }
        }
    }

    private fun observeRecentRuns() {
        viewModelScope.launch {
            scriptRepository.observeRecentRuns().collectLatest { runs ->
                _uiState.value = _uiState.value.copy(recentRuns = runs)
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
    }
}
