package com.lulucloud.touchscript.core.automation

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.lulucloud.touchscript.R

class AutomationOverlayController(
    private val context: Context
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    fun show(scriptName: String) {
        if (!Settings.canDrawOverlays(context) || overlayView != null) {
            return
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_overlay_panel)
            setPadding(32, 24, 32, 24)
            addView(
                TextView(context).apply {
                    text = context.getString(R.string.overlay_running, scriptName)
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 14f
                }
            )
            addView(
                Button(context).apply {
                    text = context.getString(R.string.stop_running)
                    setOnClickListener {
                        context.startService(
                            Intent(context, AutomationRunnerService::class.java).apply {
                                action = AutomationRunnerService.ACTION_STOP
                            }
                        )
                    }
                }
            )
        }

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
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 120
        }

        windowManager.addView(container, params)
        overlayView = container
    }

    fun hide() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }
}
