package com.lulucloud.touchscript.core.automation

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.PixelFormat
import android.graphics.RectF
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CoordinateCaptureOverlayController(
    private val context: Context,
    private val onPointCaptured: (ScreenPoint) -> Unit,
    private val onRectCaptured: (ScreenRect) -> Unit
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
    private var selectionRectView: SelectionRectView? = null
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
        selectionRectView = null
        captureLocked = false
    }

    fun showRectangleCaptureLayer(stepLabel: String) {
        if (scrimView != null) {
            hintView?.text = "请拖动选框选择$stepLabel"
            selectionRectView?.reset()
            captureLocked = false
            return
        }

        var rawStartPoint: ScreenPoint? = null
        var displayStartPoint: ScreenPoint? = null

        val scrim = FrameLayout(context).apply {
            setBackgroundColor(0x88343B45.toInt())
            isClickable = true
        }

        val selectionRect = SelectionRectView(context)

        scrim.setOnTouchListener { _, event ->
            if (captureLocked) {
                return@setOnTouchListener true
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    rawStartPoint = ScreenPoint(
                        x = event.rawX.roundToInt(),
                        y = event.rawY.roundToInt()
                    )
                    displayStartPoint = ScreenPoint(
                        x = event.x.roundToInt(),
                        y = event.y.roundToInt()
                    )
                    selectionRect.reset()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val start = displayStartPoint ?: return@setOnTouchListener true
                    val current = ScreenPoint(
                        x = event.x.roundToInt(),
                        y = event.y.roundToInt()
                    )
                    selectionRect.update(start, current)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val rawStart = rawStartPoint ?: return@setOnTouchListener true
                    val displayStart = displayStartPoint ?: return@setOnTouchListener true
                    val rawEnd = ScreenPoint(
                        x = event.rawX.roundToInt(),
                        y = event.rawY.roundToInt()
                    )
                    val displayEnd = ScreenPoint(
                        x = event.x.roundToInt(),
                        y = event.y.roundToInt()
                    )
                    selectionRect.update(displayStart, displayEnd)
                    val rect = normalizeRect(rawStart, rawEnd)
                    hintView?.text = "已选择区域：${rect.left}, ${rect.top}, ${rect.right}, ${rect.bottom}"
                    captureLocked = true
                    onRectCaptured(rect)
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    rawStartPoint = null
                    displayStartPoint = null
                    selectionRect.reset()
                    true
                }

                else -> true
            }
        }

        val hint = TextView(context).apply {
            text = "请拖动选框选择$stepLabel"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
            setPadding(28)
            setBackgroundResource(R.drawable.bg_overlay_panel)
        }

        scrim.addView(
            selectionRect,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
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
        pointView = null
        crosshairHorizontalView = null
        crosshairVerticalView = null
        crosshairRingView = null
        selectionRectView = selectionRect
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
        selectionRectView = null
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

    private fun normalizeRect(start: ScreenPoint, end: ScreenPoint): ScreenRect {
        val maxRight = screenWidth().coerceAtLeast(1)
        val maxBottom = screenHeight().coerceAtLeast(1)
        val left = min(start.x, end.x).coerceIn(0, maxRight - 1)
        val top = min(start.y, end.y).coerceIn(0, maxBottom - 1)
        val right = max(start.x, end.x).coerceIn(left + 1, maxRight)
        val bottom = max(start.y, end.y).coerceIn(top + 1, maxBottom)
        return ScreenRect(left = left, top = top, right = right, bottom = bottom)
    }

    private inner class SelectionRectView(context: Context) : View(context) {
        private val rect = RectF()
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33F4D7A1
            style = Paint.Style.FILL
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(244, 215, 161)
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
        }

        init {
            visibility = View.GONE
        }

        fun update(start: ScreenPoint, end: ScreenPoint) {
            rect.set(
                min(start.x, end.x).toFloat(),
                min(start.y, end.y).toFloat(),
                max(start.x, end.x).toFloat(),
                max(start.y, end.y).toFloat()
            )
            visibility = View.VISIBLE
            invalidate()
        }

        fun reset() {
            rect.setEmpty()
            visibility = View.GONE
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (rect.isEmpty) {
                return
            }
            canvas.drawRoundRect(rect, dp(8).toFloat(), dp(8).toFloat(), fillPaint)
            canvas.drawRoundRect(rect, dp(8).toFloat(), dp(8).toFloat(), strokePaint)
        }
    }
}
