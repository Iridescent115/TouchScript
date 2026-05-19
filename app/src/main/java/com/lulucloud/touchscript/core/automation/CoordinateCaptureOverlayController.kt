package com.lulucloud.touchscript.core.automation

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.lulucloud.touchscript.R
import kotlin.math.roundToInt

class CoordinateCaptureOverlayController(
    private val context: Context,
    private val onPointCaptured: (ScreenPoint) -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var controlView: LinearLayout? = null
    private var statusView: TextView? = null
    private var scrimView: FrameLayout? = null
    private var hintView: TextView? = null
    private var pointView: TextView? = null
    private var crosshairHorizontalView: View? = null
    private var crosshairVerticalView: View? = null
    private var crosshairRingView: View? = null
    private var captureLocked = false

    fun show() {
        if (controlView != null) {
            return
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_overlay_pill)
            setPadding(18)
            addView(actionButton("抓取") {
                dispatch(CoordinateCaptureService.ACTION_BEGIN_CAPTURE)
            })
            addView(actionButton("退出") {
                dispatch(CoordinateCaptureService.ACTION_HIDE_OVERLAY)
            })
        }

        val status = TextView(context).apply {
            setTextColor(0xFFF6F1E8.toInt())
            textSize = 12f
            setPadding(10)
        }.also { statusView = it }

        container.addView(buttonRow)
        container.addView(status)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 150
        }

        windowManager.addView(container, params)
        controlView = container
    }

    fun updateStatus(text: String) {
        statusView?.text = text
    }

    fun showCaptureLayer(stepLabel: String) {
        if (scrimView != null) {
            hintView?.text = "请点击屏幕上的$stepLabel"
            captureLocked = false
            return
        }

        val scrim = FrameLayout(context).apply {
            setBackgroundColor(0x88343B45.toInt())
            isClickable = true
            setOnTouchListener { _, event ->
                if (captureLocked) {
                    return@setOnTouchListener true
                }
                if (event.action == MotionEvent.ACTION_DOWN) {
                    captureLocked = true
                    val capturePoint = ScreenPoint(
                        x = event.rawX.roundToInt(),
                        y = event.rawY.roundToInt()
                    )
                    val displayPoint = ScreenPoint(
                        x = event.x.roundToInt(),
                        y = event.y.roundToInt()
                    )
                    showPoint(capturePoint = capturePoint, displayPoint = displayPoint)
                    onPointCaptured(capturePoint)
                    true
                } else {
                    false
                }
            }
        }

        val hint = TextView(context).apply {
            text = "请点击屏幕上的$stepLabel"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
            setPadding(28)
            setBackgroundResource(R.drawable.bg_overlay_panel)
        }

        val point = TextView(context).apply {
            visibility = View.GONE
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            setPadding(20)
            setBackgroundResource(R.drawable.bg_overlay_panel)
        }

        val horizontalCrosshair = View(context).apply {
            visibility = View.GONE
            setBackgroundColor(0xFFF4D7A1.toInt())
        }

        val verticalCrosshair = View(context).apply {
            visibility = View.GONE
            setBackgroundColor(0xFFF4D7A1.toInt())
        }

        val ringCrosshair = View(context).apply {
            visibility = View.GONE
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x00000000)
                setStroke(dp(2), 0xFFF4D7A1.toInt())
            }
        }

        scrim.addView(
            hint,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply {
                topMargin = 260
            }
        )
        scrim.addView(
            horizontalCrosshair,
            FrameLayout.LayoutParams(
                dp(32),
                dp(2)
            )
        )
        scrim.addView(
            verticalCrosshair,
            FrameLayout.LayoutParams(
                dp(2),
                dp(32)
            )
        )
        scrim.addView(
            ringCrosshair,
            FrameLayout.LayoutParams(
                dp(18),
                dp(18)
            )
        )
        scrim.addView(
            point,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(scrim, params)
        scrimView = scrim
        hintView = hint
        pointView = point
        crosshairHorizontalView = horizontalCrosshair
        crosshairVerticalView = verticalCrosshair
        crosshairRingView = ringCrosshair
        captureLocked = false
    }

    fun hideCaptureLayer() {
        scrimView?.let(windowManager::removeView)
        scrimView = null
        hintView = null
        pointView = null
        crosshairHorizontalView = null
        crosshairVerticalView = null
        crosshairRingView = null
        captureLocked = false
    }

    fun hide() {
        hideCaptureLayer()
        controlView?.let(windowManager::removeView)
        controlView = null
        statusView = null
    }

    private fun showPoint(
        capturePoint: ScreenPoint,
        displayPoint: ScreenPoint
    ) {
        val markerView = pointView ?: return
        val markerParams = markerView.layoutParams as? FrameLayout.LayoutParams ?: return
        markerView.text = "(${capturePoint.x}, ${capturePoint.y})"
        markerView.visibility = View.VISIBLE
        markerParams.leftMargin = (displayPoint.x + 18).coerceAtMost(screenWidth() - 240).coerceAtLeast(12)
        markerParams.topMargin = (displayPoint.y - 90).coerceAtMost(screenHeight() - 120).coerceAtLeast(12)
        markerView.layoutParams = markerParams

        crosshairHorizontalView?.let { horizontal ->
            val params = horizontal.layoutParams as? FrameLayout.LayoutParams ?: return@let
            params.leftMargin = (displayPoint.x - dp(16)).coerceAtLeast(0)
            params.topMargin = (displayPoint.y - dp(1)).coerceAtLeast(0)
            horizontal.layoutParams = params
            horizontal.visibility = View.VISIBLE
        }

        crosshairVerticalView?.let { vertical ->
            val params = vertical.layoutParams as? FrameLayout.LayoutParams ?: return@let
            params.leftMargin = (displayPoint.x - dp(1)).coerceAtLeast(0)
            params.topMargin = (displayPoint.y - dp(16)).coerceAtLeast(0)
            vertical.layoutParams = params
            vertical.visibility = View.VISIBLE
        }

        crosshairRingView?.let { ring ->
            val params = ring.layoutParams as? FrameLayout.LayoutParams ?: return@let
            params.leftMargin = (displayPoint.x - dp(9)).coerceAtLeast(0)
            params.topMargin = (displayPoint.y - dp(9)).coerceAtLeast(0)
            ring.layoutParams = params
            ring.visibility = View.VISIBLE
        }
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
        val intent = Intent(context, CoordinateCaptureService::class.java).apply {
            this.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun overlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun screenWidth(): Int = context.resources.displayMetrics.widthPixels

    private fun screenHeight(): Int = context.resources.displayMetrics.heightPixels

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).roundToInt()
    }
}
