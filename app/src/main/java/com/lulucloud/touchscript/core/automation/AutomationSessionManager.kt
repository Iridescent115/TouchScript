package com.lulucloud.touchscript.core.automation

import com.lulucloud.touchscript.data.repository.ScriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

class AutomationSessionManager(
    private val scriptRepository: ScriptRepository
) {
    private val _sessionState = MutableStateFlow(AutomationSessionState())
    private val _logs = MutableStateFlow<List<ExecutionLogEntry>>(emptyList())
    private val _paused = MutableStateFlow(false)

    val sessionState: StateFlow<AutomationSessionState> = _sessionState.asStateFlow()
    val logs: StateFlow<List<ExecutionLogEntry>> = _logs.asStateFlow()

    suspend fun start(scriptName: String) {
        _sessionState.value = AutomationSessionState(
            status = SessionStatus.RUNNING,
            scriptName = scriptName,
            startedAt = System.currentTimeMillis()
        )
        _paused.value = false
        _logs.value = emptyList()
        appendLog("INFO", "开始执行脚本：$scriptName")
    }

    suspend fun appendLog(level: String, message: String) {
        _logs.value = (_logs.value + ExecutionLogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            message = message
        )).takeLast(MAX_LOG_COUNT)
    }

    suspend fun pause() {
        if (_sessionState.value.status == SessionStatus.RUNNING) {
            _paused.value = true
            _sessionState.value = _sessionState.value.copy(status = SessionStatus.PAUSED)
            appendLog("INFO", "脚本已暂停")
        }
    }

    suspend fun resume() {
        if (_sessionState.value.status == SessionStatus.PAUSED) {
            _paused.value = false
            _sessionState.value = _sessionState.value.copy(status = SessionStatus.RUNNING)
            appendLog("INFO", "脚本继续执行")
        }
    }

    suspend fun togglePause() {
        when (_sessionState.value.status) {
            SessionStatus.RUNNING -> pause()
            SessionStatus.PAUSED -> resume()
            else -> Unit
        }
    }

    suspend fun awaitIfPaused() {
        if (_paused.value) {
            _paused.filter { paused -> !paused }.first()
        }
    }

    suspend fun completeSuccess(summary: String) {
        finish(SessionStatus.SUCCESS, summary)
    }

    suspend fun completeFailure(summary: String) {
        finish(SessionStatus.FAILED, summary)
    }

    suspend fun completeCancelled(summary: String) {
        finish(SessionStatus.CANCELLED, summary)
    }

    private suspend fun finish(status: SessionStatus, summary: String) {
        val current = _sessionState.value
        val finishedAt = System.currentTimeMillis()
        _paused.value = false
        _sessionState.value = current.copy(
            status = status,
            finishedAt = finishedAt,
            summary = summary
        )
        appendLog("INFO", summary)
        if (current.scriptName.isNotBlank()) {
            scriptRepository.addRunRecord(
                scriptName = current.scriptName,
                status = status.name,
                summary = summary,
                startedAt = current.startedAt,
                endedAt = finishedAt
            )
        }
    }

    private companion object {
        const val MAX_LOG_COUNT = 200
    }
}
