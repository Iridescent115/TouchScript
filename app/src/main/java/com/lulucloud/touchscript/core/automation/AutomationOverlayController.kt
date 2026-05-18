package com.lulucloud.touchscript.core.automation

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.lulucloud.touchscript.R

class AutomationOverlayController(
    private val context: Context
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: LinearLayout? = null
    private var statusView: TextView? = null
    private var logsView: TextView? = null
    private var startStopView: TextView? = null
    private var pauseView: TextView? = null
    private var logPanelVisible = false

    fun show() {
        if (!Settings.canDrawOverlays(context) || rootView != null) {
            return
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_overlay_pill)
            setPadding(18)
            addView(actionButton("启动/停止") {
                dispatch(AutomationRunnerService.ACTION_TOGGLE_START_STOP)
            }.also { startStopView = it })
            addView(actionButton("暂停") {
                dispatch(AutomationRunnerService.ACTION_TOGGLE_PAUSE)
            }.also { pauseView = it })
            addView(actionButton("日志") {
                logPanelVisible = !logPanelVisible
                logsView?.visibility = if (logPanelVisible) View.VISIBLE else View.GONE
            })
            addView(actionButton("退出") {
                dispatch(AutomationRunnerService.ACTION_HIDE_OVERLAY)
            })
        }

        val logPanel = TextView(context).apply {
            setBackgroundResource(R.drawable.bg_overlay_panel)
            setPadding(24)
            visibility = View.GONE
            setTextColor(0xFFF6F1E8.toInt())
            textSize = 12f
        }.also { logsView = it }

        val status = TextView(context).apply {
            setTextColor(0xFFF6F1E8.toInt())
            textSize = 12f
        }.also { statusView = it }

        container.addView(buttonRow)
        container.addView(status)
        container.addView(logPanel)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 150
        }

        windowManager.addView(container, params)
        rootView = container
    }

    fun render(sessionState: AutomationSessionState, logs: List<ExecutionLogEntry>) {
        startStopView?.text = if (sessionState.status == SessionStatus.RUNNING || sessionState.status == SessionStatus.PAUSED) {
            "停止"
        } else {
            "启动"
        }
        pauseView?.text = if (sessionState.status == SessionStatus.PAUSED) "继续" else "暂停"
        statusView?.text = "状态：${sessionState.status.name}  脚本：${sessionState.scriptName.ifBlank { "未加载" }}"
        logsView?.text = buildString {
            append("状态：${sessionState.status.name}\n")
            if (sessionState.summary.isNotBlank()) {
                append("摘要：${sessionState.summary}\n")
            }
            append("\n最近日志：\n")
            if (logs.isEmpty()) {
                append("暂无日志")
            } else {
                logs.takeLast(6).forEach { log ->
                    append("[${log.level}] ${log.message}\n")
                }
            }
        }.trim()
    }

    fun hide() {
        rootView?.let(windowManager::removeView)
        rootView = null
        statusView = null
        logsView = null
        startStopView = null
        pauseView = null
        logPanelVisible = false
    }

    private fun actionButton(text: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
            setPadding(24)
            setOnClickListener { onClick() }
        }
    }

    private fun dispatch(action: String) {
        context.startService(
            Intent(context, AutomationRunnerService::class.java).apply {
                this.action = action
            }
        )
    }
}
