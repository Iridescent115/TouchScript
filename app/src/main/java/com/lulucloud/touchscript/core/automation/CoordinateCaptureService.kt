package com.lulucloud.touchscript.core.automation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.lulucloud.touchscript.MainActivity
import com.lulucloud.touchscript.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CoordinateCaptureService : LifecycleService() {
    private lateinit var overlayController: CoordinateCaptureOverlayController
    private val capturedPoints = mutableListOf<ScreenPoint>()
    private var activeRequest: CoordinateCaptureRequest? = null

    override fun onCreate() {
        super.onCreate()
        overlayController = CoordinateCaptureOverlayController(this, ::handlePointCaptured, ::handleRectCaptured)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> showOverlay()
            ACTION_BEGIN_CAPTURE -> beginCapture()
            ACTION_HIDE_OVERLAY -> stopCaptureOverlay()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        overlayController.hide()
        super.onDestroy()
    }

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }

        activeRequest = CoordinateCaptureManager.activeRequest.value
        capturedPoints.clear()
        startForeground(NOTIFICATION_ID, createNotification("抓抓已启用"))
        overlayController.show()
        overlayController.updateStatus(activeRequest?.let(::buildIdleStatus) ?: "点“抓取”开始取点")
    }

    private fun beginCapture() {
        val request = activeRequest ?: CoordinateCaptureManager.activeRequest.value
        if (request == null) {
            overlayController.updateStatus("当前没有待抓取的坐标")
            return
        }
        activeRequest = request
        if (request.mode == CoordinateCaptureMode.RECTANGLE) {
            val label = request.stepLabels.firstOrNull() ?: "识别区域"
            overlayController.updateStatus("正在选择$label")
            overlayController.showRectangleCaptureLayer(label)
        } else {
            val nextLabel = request.stepLabels.getOrElse(capturedPoints.size) { "目标点" }
            overlayController.updateStatus("正在抓取$nextLabel")
            overlayController.showCaptureLayer(nextLabel)
        }
    }

    private fun handlePointCaptured(point: ScreenPoint) {
        val request = activeRequest ?: return
        if (request.mode != CoordinateCaptureMode.POINTS) {
            return
        }
        capturedPoints += point
        lifecycleScope.launch {
            delay(220)
            overlayController.hideCaptureLayer()

            if (capturedPoints.size >= request.pointCount) {
                overlayController.updateStatus("已抓到 ${capturedPoints.joinToString("  ") { "(${it.x}, ${it.y})" }}")
                CoordinateCaptureManager.complete(capturedPoints.toList())
                delay(450)
                stopCaptureOverlay()
            } else {
                val nextLabel = request.stepLabels.getOrElse(capturedPoints.size) { "目标点" }
                overlayController.updateStatus("已抓到第 ${capturedPoints.size} 个点，请继续抓取$nextLabel")
                overlayController.showCaptureLayer(nextLabel)
            }
        }
    }

    private fun handleRectCaptured(rect: ScreenRect) {
        val request = activeRequest ?: return
        if (request.mode != CoordinateCaptureMode.RECTANGLE) {
            return
        }
        lifecycleScope.launch {
            delay(260)
            overlayController.hideCaptureLayer()
            overlayController.updateStatus("已选择区域：${rect.left}, ${rect.top}, ${rect.right}, ${rect.bottom}")
            CoordinateCaptureManager.complete(rect)
            delay(450)
            stopCaptureOverlay()
        }
    }

    private fun stopCaptureOverlay() {
        capturedPoints.clear()
        activeRequest = null
        CoordinateCaptureManager.cancel()
        overlayController.hide()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildIdleStatus(request: CoordinateCaptureRequest): String {
        return if (request.mode == CoordinateCaptureMode.RECTANGLE) {
            "点“抓取”后拖动选择${request.stepLabels.firstOrNull() ?: "识别区域"}"
        } else if (request.pointCount == 1) {
            "点“抓取”后选择${request.stepLabels.firstOrNull() ?: "目标点"}"
        } else {
            "点“抓取”后依次选择${request.stepLabels.joinToString("、")}"
        }
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
            "抓抓服务",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.lulucloud.touchscript.action.SHOW_COORDINATE_CAPTURE_OVERLAY"
        const val ACTION_BEGIN_CAPTURE = "com.lulucloud.touchscript.action.BEGIN_COORDINATE_CAPTURE"
        const val ACTION_HIDE_OVERLAY = "com.lulucloud.touchscript.action.HIDE_COORDINATE_CAPTURE_OVERLAY"

        private const val CHANNEL_ID = "touch_workshop_coordinate_capture"
        private const val NOTIFICATION_ID = 1002

        fun launch(context: Context, request: CoordinateCaptureRequest) {
            if (!Settings.canDrawOverlays(context)) {
                Toast.makeText(context, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
                return
            }
            CoordinateCaptureManager.activate(request)
            val intent = Intent(context, CoordinateCaptureService::class.java).apply {
                action = ACTION_SHOW_OVERLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
