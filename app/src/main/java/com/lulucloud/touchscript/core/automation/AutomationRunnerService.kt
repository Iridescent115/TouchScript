package com.lulucloud.touchscript.core.automation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.lulucloud.touchscript.MainActivity
import com.lulucloud.touchscript.R
import com.lulucloud.touchscript.TouchWorkshopApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutomationRunnerService : LifecycleService() {
    private var currentJob: Job? = null
    private var overlayVisible = false
    private lateinit var overlayController: AutomationOverlayController

    override fun onCreate() {
        super.onCreate()
        overlayController = AutomationOverlayController(this)
        createNotificationChannel()
        observeSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> showOverlay()
            ACTION_HIDE_OVERLAY -> lifecycleScope.launch { stopExecutionAndHideOverlay() }
            ACTION_TOGGLE_START_STOP -> lifecycleScope.launch { toggleStartStop() }
            ACTION_TOGGLE_PAUSE -> lifecycleScope.launch { appContainer().sessionManager.togglePause() }
            ACTION_STOP -> lifecycleScope.launch { stopExecutionByUser() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        overlayController.hide()
        currentJob?.cancel()
        super.onDestroy()
    }

    private fun showOverlay() {
        overlayVisible = true
        startForeground(NOTIFICATION_ID, createNotification("悬浮窗已启用"))
        overlayController.show()
        renderOverlay()
    }

    private fun hideOverlay() {
        overlayVisible = false
        overlayController.hide()
        if (currentJob == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun toggleStartStop() {
        when (appContainer().sessionManager.sessionState.value.status) {
            SessionStatus.RUNNING,
            SessionStatus.PAUSED -> stopExecutionByUser()

            else -> startSelectedScript()
        }
    }

    private suspend fun startSelectedScript() {
        val container = appContainer()
        val debugDraft = DebugScriptDraftStore.get()
        if (debugDraft != null) {
            startScriptExecution(
                scriptName = debugDraft.scriptName,
                source = debugDraft.content
            )
            return
        }

        val settings = container.settingsRepository.getSettings()
        val path = settings.selectedScriptPath
            ?: run {
                container.sessionManager.completeFailure("未选择脚本文件")
                return
            }

        val file = runCatching { container.fileScriptRepository.readFile(path) }
            .getOrElse { throwable ->
                container.sessionManager.completeFailure(throwable.message ?: "读取脚本失败")
                return
            }

        startScriptExecution(
            scriptName = file.name,
            source = file.content
        )
    }

    private fun startScriptExecution(
        scriptName: String,
        source: String
    ) {
        val container = appContainer()
        currentJob?.cancel()
        currentJob = lifecycleScope.launch(Dispatchers.Default) {
            try {
                container.sessionManager.start(scriptName)
                val compilation = container.scriptCompiler.compile(source)
                container.sessionManager.appendLog("INFO", "DSL 编译成功，准备执行 Lua")
                updateNotification("正在执行：$scriptName")
                container.scriptRuntime.execute(compilation.luaSource, scriptName)
                container.sessionManager.completeSuccess("脚本执行完成")
            } catch (stop: AutomationStopException) {
                container.sessionManager.completeCancelled(stop.message ?: "用户停止了当前脚本")
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                if (container.sessionManager.isStopRequested()) {
                    container.sessionManager.completeCancelled("用户停止了当前脚本")
                } else {
                    throw cancelled
                }
            } catch (throwable: Throwable) {
                container.sessionManager.appendLog("ERROR", throwable.message ?: "未知错误")
                container.sessionManager.completeFailure("脚本执行失败")
            } finally {
                currentJob = null
                if (!overlayVisible) {
                    withContext(Dispatchers.Main) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                } else {
                    updateNotification("悬浮窗已启用")
                }
            }
        }
    }

    private suspend fun stopExecutionByUser() {
        appContainer().sessionManager.requestStop("用户停止了当前脚本")
        currentJob?.cancel()
        currentJob = null
        if (!overlayVisible) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            updateNotification("悬浮窗已启用")
        }
    }

    private suspend fun stopExecutionAndHideOverlay() {
        val status = appContainer().sessionManager.sessionState.value.status
        val isRunning = status == SessionStatus.RUNNING || status == SessionStatus.PAUSED
        if (currentJob != null || isRunning) {
            appContainer().sessionManager.requestStop("用户退出悬浮窗，已停止当前脚本")
            currentJob?.cancel()
            currentJob = null
        }
        hideOverlay()
    }

    private fun observeSession() {
        lifecycleScope.launch {
            appContainer().sessionManager.sessionState.collectLatest {
                renderOverlay()
            }
        }
        lifecycleScope.launch {
            appContainer().sessionManager.logs.collectLatest {
                renderOverlay()
            }
        }
    }

    private fun renderOverlay() {
        if (overlayVisible) {
            overlayController.render(
                sessionState = appContainer().sessionManager.sessionState.value,
                logs = appContainer().sessionManager.logs.value
            )
        }
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(contentText))
    }

    private fun createNotification(contentText: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("触灵工坊")
            .setContentText(contentText)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun appContainer() = (application as TouchWorkshopApplication).appContainer

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.lulucloud.touchscript.action.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.lulucloud.touchscript.action.HIDE_OVERLAY"
        const val ACTION_TOGGLE_START_STOP = "com.lulucloud.touchscript.action.TOGGLE_START_STOP"
        const val ACTION_TOGGLE_PAUSE = "com.lulucloud.touchscript.action.TOGGLE_PAUSE"
        const val ACTION_STOP = "com.lulucloud.touchscript.action.STOP"

        private const val CHANNEL_ID = "touch_workshop_runner"
        private const val NOTIFICATION_ID = 1001
    }
}
