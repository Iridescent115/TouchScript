package com.lulucloud.touchscript.core.automation

import android.content.Context
import kotlinx.coroutines.delay

class AccessibilityAutomationExecutor(
    private val context: Context,
    private val sessionManager: AutomationSessionManager
) : AutomationExecutor {

    override suspend fun perform(action: AutomationAction): AutomationResult {
        sessionManager.ensureNotStopped()
        return when (action) {
            is ClickAction -> withService { click(action.x, action.y) }
            is LongPressAction -> withService { longPress(action.x, action.y, action.durationMs) }
            is SwipeAction -> withService {
                swipe(
                    startX = action.startX,
                    startY = action.startY,
                    endX = action.endX,
                    endY = action.endY,
                    durationMs = action.durationMs
                )
            }

            is KeyboardInputAction -> withService { inputText(action.text) }

            is SleepAction -> {
                var remainingMs = action.durationMs.coerceAtLeast(0L)
                while (remainingMs > 0L) {
                    sessionManager.ensureNotStopped()
                    val step = remainingMs.coerceAtMost(SLEEP_POLL_INTERVAL_MS)
                    delay(step)
                    remainingMs -= step
                }
                sessionManager.ensureNotStopped()
                AutomationResult(success = true)
            }

            is LaunchAppAction -> {
                val service = AccessibilityServiceRegistry.service.value
                val launched = service?.launchApp(action.packageName)
                    ?: context.packageManager.getLaunchIntentForPackage(action.packageName)?.let { intent ->
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        true
                    } ?: false
                if (launched) {
                    AutomationResult(success = true)
                } else {
                    AutomationResult(success = false, message = "无法启动应用：${action.packageName}")
                }
            }

            BackAction -> withService { back() }
            HomeAction -> withService { home() }
        }
    }

    private suspend fun withService(block: suspend TouchWorkshopAccessibilityService.() -> Boolean): AutomationResult {
        sessionManager.ensureNotStopped()
        val service = AccessibilityServiceRegistry.service.value
            ?: return AutomationResult(success = false, message = "无障碍服务未连接")
        return if (service.block()) {
            sessionManager.ensureNotStopped()
            AutomationResult(success = true)
        } else {
            AutomationResult(success = false, message = "系统拒绝执行自动化动作")
        }
    }

    private companion object {
        const val SLEEP_POLL_INTERVAL_MS = 50L
    }
}
