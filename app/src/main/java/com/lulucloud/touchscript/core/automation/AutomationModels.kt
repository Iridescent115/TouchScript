package com.lulucloud.touchscript.core.automation

data class AutomationResult(
    val success: Boolean,
    val message: String? = null
)

sealed interface AutomationAction

data class ClickAction(
    val x: Int,
    val y: Int
) : AutomationAction

data class LongPressAction(
    val x: Int,
    val y: Int,
    val durationMs: Long
) : AutomationAction

data class SwipeAction(
    val startX: Int,
    val startY: Int,
    val endX: Int,
    val endY: Int,
    val durationMs: Long
) : AutomationAction

data class SleepAction(
    val durationMs: Long
) : AutomationAction

data class LaunchAppAction(
    val packageName: String
) : AutomationAction

data object BackAction : AutomationAction

data object HomeAction : AutomationAction

interface AutomationExecutor {
    suspend fun perform(action: AutomationAction): AutomationResult
}

enum class SessionStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED
}

data class AutomationSessionState(
    val status: SessionStatus = SessionStatus.IDLE,
    val scriptName: String = "",
    val startedAt: Long = 0L,
    val finishedAt: Long = 0L,
    val summary: String = ""
)

data class ExecutionLogEntry(
    val timestamp: Long,
    val level: String,
    val message: String
)
