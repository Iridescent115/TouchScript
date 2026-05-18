package com.lulucloud.touchscript.core.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class TouchWorkshopAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityServiceRegistry.attach(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        AccessibilityServiceRegistry.detach(this)
        return super.onUnbind(intent)
    }

    suspend fun click(x: Int, y: Int): Boolean = performPathGesture(
        durationMs = 1L
    ) {
        moveTo(x.toFloat(), y.toFloat())
    }

    suspend fun longPress(x: Int, y: Int, durationMs: Long): Boolean = performPathGesture(
        durationMs = durationMs
    ) {
        moveTo(x.toFloat(), y.toFloat())
    }

    suspend fun swipe(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        durationMs: Long
    ): Boolean = performPathGesture(durationMs = durationMs) {
        moveTo(startX.toFloat(), startY.toFloat())
        lineTo(endX.toFloat(), endY.toFloat())
    }

    fun back(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

    fun home(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    fun launchApp(packageName: String): Boolean {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launchIntent)
        return true
    }

    private suspend fun performPathGesture(
        durationMs: Long,
        configurePath: Path.() -> Unit
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val path = Path().apply(configurePath)
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceAtLeast(1L))
        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        val dispatched = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            },
            null
        )

        if (!dispatched && continuation.isActive) {
            continuation.resume(false)
        }
    }
}
