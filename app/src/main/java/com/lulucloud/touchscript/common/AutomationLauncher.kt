package com.lulucloud.touchscript.common

import android.content.Context
import android.content.Intent
import com.lulucloud.touchscript.core.automation.AutomationRunnerService

object AutomationLauncher {
    fun start(context: Context, scriptName: String, scriptSource: String) {
        context.startService(
            Intent(context, AutomationRunnerService::class.java).apply {
                action = AutomationRunnerService.ACTION_START
                putExtra(AutomationRunnerService.EXTRA_SCRIPT_NAME, scriptName)
                putExtra(AutomationRunnerService.EXTRA_SCRIPT_SOURCE, scriptSource)
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
