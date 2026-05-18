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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutomationRunnerService : LifecycleService() {
    private var currentJob: Job? = null
    private lateinit var overlayController: AutomationOverlayController

    override fun onCreate() {
        super.onCreate()
        overlayController = AutomationOverlayController(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                lifecycleScope.launch {
                    val container = appContainer()
                    container.sessionManager.completeCancelled("用户停止了当前脚本")
                    stopRunner()
                }
            }

            ACTION_START -> {
                val scriptName = intent.getStringExtra(EXTRA_SCRIPT_NAME).orEmpty().ifBlank {
                    getString(R.string.default_script_name)
                }
                val scriptSource = intent.getStringExtra(EXTRA_SCRIPT_SOURCE).orEmpty()
                if (scriptSource.isBlank()) {
                    lifecycleScope.launch {
                        appContainer().sessionManager.completeFailure("脚本内容为空，无法执行")
                        stopRunner()
                    }
                } else {
                    startForeground(NOTIFICATION_ID, createNotification(scriptName))
                    startRunner(scriptName, scriptSource)
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        overlayController.hide()
        currentJob?.cancel()
        super.onDestroy()
    }

    private fun startRunner(scriptName: String, scriptSource: String) {
        currentJob?.cancel()
        val container = appContainer()
        currentJob = lifecycleScope.launch(Dispatchers.Default) {
            try {
                container.sessionManager.start(scriptName)
                val settings = container.settingsRepository.settings.first()
                if (settings.showFloatingPanel) {
                    withContext(Dispatchers.Main) {
                        overlayController.show(scriptName)
                    }
                }
                val compilation = container.scriptCompiler.compile(scriptSource)
                container.sessionManager.appendLog("INFO", "DSL 编译成功，准备执行 Lua")
                container.scriptRuntime.execute(compilation.luaSource, scriptName)
                container.sessionManager.completeSuccess("脚本执行完成")
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                container.sessionManager.completeCancelled("脚本执行已取消")
                throw cancelled
            } catch (throwable: Throwable) {
                container.sessionManager.appendLog("ERROR", throwable.message ?: "未知错误")
                container.sessionManager.completeFailure("脚本执行失败")
            } finally {
                stopRunner()
            }
        }
    }

    private suspend fun stopRunner() {
        currentJob?.cancel()
        currentJob = null
        withContext(Dispatchers.Main) {
            overlayController.hide()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(scriptName: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AutomationRunnerService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_running_title))
            .setContentText(getString(R.string.notification_running_text, scriptName))
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .addAction(0, getString(R.string.stop_running), stopIntent)
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
        const val ACTION_START = "com.lulucloud.touchscript.action.START"
        const val ACTION_STOP = "com.lulucloud.touchscript.action.STOP"
        const val EXTRA_SCRIPT_NAME = "extra_script_name"
        const val EXTRA_SCRIPT_SOURCE = "extra_script_source"

        private const val CHANNEL_ID = "touch_workshop_runner"
        private const val NOTIFICATION_ID = 1001
    }
}
