package com.lulucloud.touchscript.common

import android.content.Context
import android.content.Intent
import com.lulucloud.touchscript.core.automation.AutomationRunnerService

object AutomationLauncher {
    fun showOverlay(context: Context) {
        context.startService(
            Intent(context, AutomationRunnerService::class.java).apply {
                action = AutomationRunnerService.ACTION_SHOW_OVERLAY
            }
        )
    }

    fun hideOverlay(context: Context) {
        context.startService(
            Intent(context, AutomationRunnerService::class.java).apply {
                action = AutomationRunnerService.ACTION_HIDE_OVERLAY
            }
        )
    }

    fun stop(context: Context) {
        context.startService(
            Intent(context, AutomationRunnerService::class.java).apply {
                action = AutomationRunnerService.ACTION_STOP
            }
        )
    }
}
